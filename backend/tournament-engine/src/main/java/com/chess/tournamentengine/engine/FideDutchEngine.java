package com.chess.tournamentengine.engine;

import com.chess.tournamentengine.exception.TournamentUnpairableException;
import com.chess.tournamentengine.model.Match;
import com.chess.tournamentengine.model.TournamentPlayer;
import com.chess.tournamentengine.model.enums.MatchResult;
import com.chess.tournamentengine.repository.MatchRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the FIDE Dutch Pairing System.
 *
 * <p>Combines the formal pseudocode's structured function decomposition
 * ({@code Solve_Bracket}, {@code GENERATE_TRANSPOSITIONS}, {@code GENERATE_EXCHANGES})
 * with an explicit, readable constraint-relaxation loop. Produces a deterministic
 * set of board assignments for a single tournament round.</p>
 *
 * <p><b>Algorithm overview:</b></p>
 * <ol>
 *   <li>If the player count is odd, select and remove a bye recipient.</li>
 *   <li>Partition remaining players into score buckets (DESC score order).</li>
 *   <li>Process each bucket top-down using {@link BracketState} and backtracking.</li>
 *   <li>For each bucket: split into S1 (top half) and S2 (bottom half).</li>
 *   <li>Iterate through tolerance levels: {@code PERFECT_COLORS → RELAXED_COLORS → IGNORE_COLORS}.</li>
 *   <li>At each level: try all transpositions of S2, then all exchanges between S1 and S2.</li>
 *   <li>On failure: backtrack to the previous bucket and force its next candidate.</li>
 *   <li>If the bracket stack is exhausted: throw {@link TournamentUnpairableException}.</li>
 * </ol>
 *
 * <p><b>Current implementation notes:</b></p>
 * <ul>
 *   <li>The downfloater for odd-sized brackets is always the middle player
 *       (index {@code floor(n/2)}). Full FIDE compliance would require trying
 *       alternate downfloaters — this is a known TODO.</li>
 *   <li>Permutation generation uses Heap's algorithm: O(n!) per S2 subgroup.
 *       Acceptable for typical FIDE bracket sizes (≤ 10 players per half).</li>
 * </ul>
 */
@Component
public class FideDutchEngine {

    private final MatchRepository matchRepository;
    private final ValidationUtil  validationUtil;

    public FideDutchEngine(MatchRepository matchRepository, ValidationUtil validationUtil) {
        this.matchRepository = matchRepository;
        this.validationUtil  = validationUtil;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Generates all board assignments for a single tournament round.
     *
     * <p>The returned {@link Match} objects are not persisted. The calling service
     * is responsible for saving them via {@code MatchRepository.saveAll()}.</p>
     *
     * @param tid           tournament identifier
     * @param roundNo       round number being generated
     * @param sortedPlayers enrolled players sorted by score DESC, FIDE rating DESC
     * @return ordered list of Match entities (board 1 = most prestigious pairing)
     * @throws TournamentUnpairableException if no valid pairing exists for this round
     */
    public List<Match> pairRound(UUID tid, int roundNo, List<TournamentPlayer> sortedPlayers) {
        List<TournamentPlayer> players = new ArrayList<>(sortedPlayers);

        // Step 1 — BYE assignment for odd player counts
        Match byeMatch = null;
        if (players.size() % 2 != 0) {
            TournamentPlayer byePlayer = selectByePlayer(players);
            players.remove(byePlayer);
            byeMatch = createByeMatch(tid, roundNo, byePlayer);
        }

        // Step 2 — Partition into score buckets, preserving DESC ordering
        List<List<TournamentPlayer>> buckets = partitionByScore(players);

        // Step 3 — Solve all buckets with backtracking
        List<Match> allMatches = processBucketsWithBacktracking(buckets, tid, roundNo);

        // Step 4 — Assign board numbers (1-indexed; lower = higher-ranked pairing)
        assignBoardNumbers(allMatches);

        // Step 5 — Append bye (does not consume a board number in the main sequence)
        if (byeMatch != null) {
            byeMatch.setBoardNo(allMatches.size() + 1);
            allMatches.add(byeMatch);
        }

        return allMatches;
    }

    // =========================================================================
    // BYE Handling
    // =========================================================================

    /**
     * Selects the bye recipient following FIDE priority rules.
     * Iterates from the lowest-ranked player upward, selecting the first player
     * who has not yet received a bye. Falls back to the absolute last player if
     * every enrolled player has already received a bye.
     */
    private TournamentPlayer selectByePlayer(List<TournamentPlayer> players) {
        for (int i = players.size() - 1; i >= 0; i--) {
            if (!players.get(i).isHasReceivedBye()) return players.get(i);
        }
        return players.get(players.size() - 1);
    }

    /**
     * Constructs a bye Match record. The bye player occupies the White slot;
     * {@code blackPid} is {@code null}. The result is recorded as
     * {@link MatchResult#WHITE_WIN_FORFEIT} representing the FIDE half-point bye award.
     */
    private Match createByeMatch(UUID tid, int roundNo, TournamentPlayer byePlayer) {
        return Match.builder()
                .tid(tid)
                .roundNo(roundNo)
                .boardNo(0)
                .whitePid(byePlayer.getId().getPid())
                .blackPid(null)
                .matchResult(MatchResult.WHITE_WIN_FORFEIT)
                .build();
    }

    // =========================================================================
    // Score Bracket Partitioning
    // =========================================================================

    /**
     * Partitions a sorted player list into contiguous score buckets.
     * Players sharing the same score form one bucket. Ordering within each
     * bucket (by FIDE rating DESC) is preserved from the input list.
     */
    private List<List<TournamentPlayer>> partitionByScore(List<TournamentPlayer> players) {
        List<List<TournamentPlayer>> buckets = new ArrayList<>();
        if (players.isEmpty()) return buckets;

        double currentScore = players.get(0).getPlayerScore();
        List<TournamentPlayer> currentBucket = new ArrayList<>();

        for (TournamentPlayer tp : players) {
            if (Double.compare(tp.getPlayerScore(), currentScore) != 0) {
                buckets.add(currentBucket);
                currentBucket = new ArrayList<>();
                currentScore = tp.getPlayerScore();
            }
            currentBucket.add(tp);
        }
        buckets.add(currentBucket);

        return buckets;
    }

    // =========================================================================
    // Top-Down Bucket Processing with Backtracking
    // =========================================================================

    /**
     * Processes score buckets in order using a stack-based backtracking scheme.
     *
     * <p>Each bucket is represented by a {@link BracketState} that holds an
     * ordered list of all valid candidate pairings for that bracket. The algorithm
     * advances through candidates using an index pointer. On failure, the current
     * bracket is removed from the stack and the previous bracket's index advances
     * to its next candidate, which may also change the downfloater composition
     * for subsequent brackets.</p>
     *
     * @throws TournamentUnpairableException when the stack is exhausted without a solution
     */
    private List<Match> processBucketsWithBacktracking(
            List<List<TournamentPlayer>> buckets, UUID tid, int roundNo) {

        List<BracketState> stack = new ArrayList<>();

        for (int bucketIndex = 0; bucketIndex < buckets.size(); ) {

            BracketState state;

            if (bucketIndex < stack.size()) {
                // Resuming a previously-entered bracket after backtracking from a lower one
                state = stack.get(bucketIndex);
            } else {
                // First visit: build the bracket player list and generate all candidates
                List<TournamentPlayer> downfloaters = (bucketIndex > 0)
                        ? extractDownfloaters(stack.get(bucketIndex - 1))
                        : List.of();

                List<TournamentPlayer> bracketPlayers = new ArrayList<>(buckets.get(bucketIndex));
                bracketPlayers.addAll(downfloaters);

                List<List<Match>> candidates = generateCandidates(bracketPlayers, tid, roundNo);
                state = new BracketState(bracketPlayers, candidates);
                stack.add(state);
            }

            if (state.hasNext()) {
                state.advance();
                bucketIndex++;
            } else {
                // Bracket fully exhausted — step back to the previous bracket (BACKTRACK)
                stack.remove(bucketIndex);
                bucketIndex--;

                if (bucketIndex < 0) {
                    throw new TournamentUnpairableException(
                            "Round " + " is mathematically unpairable. " +
                            "All bracket configurations and backtracking paths have been exhausted.");
                }
            }
        }

        return stack.stream()
                .flatMap(s -> s.currentMatches.stream())
                .collect(Collectors.toList());
    }

    /**
     * Extracts players from the given bracket state who were not assigned to any board.
     * These players become downfloaters appended to the next bracket's player list.
     *
     * <p>For even-sized brackets, this always returns an empty list.
     * For odd-sized brackets, exactly one player (the middle player at index
     * {@code floor(n/2)}) is never included in S1 or S2 and is always the downfloater.</p>
     */
    private List<TournamentPlayer> extractDownfloaters(BracketState state) {
        if (state.bracketPlayers.size() % 2 == 0) return List.of();

        Set<UUID> pairedPids = new HashSet<>();
        for (Match m : state.currentMatches) {
            pairedPids.add(m.getWhitePid());
            if (m.getBlackPid() != null) pairedPids.add(m.getBlackPid());
        }

        return state.bracketPlayers.stream()
                .filter(tp -> !pairedPids.contains(tp.getId().getPid()))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Candidate Generation — Solve_Bracket
    // =========================================================================

    /**
     * Generates all valid candidate pairings for a bracket in preference order.
     *
     * <p>Implements {@code Solve_Bracket} from the formal pseudocode:</p>
     * <pre>
     * FOR Tolerance_Level IN [PERFECT_COLORS, RELAXED_COLORS, IGNORE_COLORS]:
     *     FOR EACH Transposed_S2 IN GENERATE_TRANSPOSITIONS(S2):
     *         IF Is_Valid_Match(S1, Transposed_S2, Tolerance_Level): SAVE → RETURN SUCCESS
     *     FOR EACH Exchanged_State IN GENERATE_EXCHANGES(S1, S2):
     *         FOR EACH Transposed_S2 IN GENERATE_TRANSPOSITIONS(S2_New):
     *             IF Is_Valid_Match(S1_New, Transposed_S2, Tolerance_Level): SAVE → RETURN SUCCESS
     * RETURN FAILURE
     * </pre>
     *
     * <p>S1 = top {@code floor(n/2)} players; S2 = bottom {@code floor(n/2)} players.
     * The middle player in an odd-sized bracket ({@code bracket[floor(n/2)]}) is
     * excluded from both subgroups and will be identified as the downfloater.</p>
     */
    private List<List<Match>> generateCandidates(
            List<TournamentPlayer> bracketPlayers, UUID tid, int roundNo) {

        List<List<Match>> candidates = new ArrayList<>();
        if (bracketPlayers.size() < 2) return candidates;

        int n         = bracketPlayers.size();
        int halfFloor = n / 2;

        // S1 = top half (indices 0 to halfFloor-1)
        List<TournamentPlayer> s1 = new ArrayList<>(bracketPlayers.subList(0, halfFloor));
        // S2 = bottom half (indices n-halfFloor to n-1); middle player is excluded if n is odd
        List<TournamentPlayer> s2 = new ArrayList<>(bracketPlayers.subList(n - halfFloor, n));

        for (Tolerance tolerance : Tolerance.values()) {

            // Phase 1 — Transpositions: reorder S2 while S1 remains fixed
            for (List<TournamentPlayer> transposedS2 : generateTranspositions(new ArrayList<>(s2))) {
                if (isValidMatch(s1, transposedS2, tolerance, tid)) {
                    candidates.add(buildMatches(s1, transposedS2, tid, roundNo));
                }
            }

            // Phase 2 — Exchanges: swap one player between S1 and S2, then transpose the new S2
            for (ExchangeState exchange : generateExchanges(s1, s2)) {
                for (List<TournamentPlayer> transposedS2New : generateTranspositions(exchange.s2())) {
                    if (isValidMatch(exchange.s1(), transposedS2New, tolerance, tid)) {
                        candidates.add(buildMatches(exchange.s1(), transposedS2New, tid, roundNo));
                    }
                }
            }
        }

        return candidates;
    }

    // =========================================================================
    // Transposition Generation — GENERATE_TRANSPOSITIONS
    // =========================================================================

    /**
     * Produces all permutations of the S2 subgroup using Heap's in-place algorithm.
     *
     * <p>The original S2 ordering is always the first permutation returned,
     * giving it the highest priority within a tolerance level.</p>
     *
     * @param s2 the S2 subgroup to permute
     * @return list of all permutations; size = {@code |s2|!}
     */
    private List<List<TournamentPlayer>> generateTranspositions(List<TournamentPlayer> s2) {
        List<List<TournamentPlayer>> result = new ArrayList<>();
        heapPermute(new ArrayList<>(s2), s2.size(), result);
        return result;
    }

    /**
     * Recursive Heap's permutation algorithm.
     * Generates all {@code n!} permutations of {@code arr[0..n-1]} in-place,
     * appending each to {@code result} when {@code n == 1}.
     */
    private void heapPermute(List<TournamentPlayer> arr, int n,
                              List<List<TournamentPlayer>> result) {
        if (n == 1) {
            result.add(new ArrayList<>(arr));
            return;
        }
        for (int i = 0; i < n; i++) {
            heapPermute(arr, n - 1, result);
            if (i < n - 1) {
                // Even n: swap element i with last; Odd n: swap element 0 with last
                Collections.swap(arr, n % 2 == 0 ? i : 0, n - 1);
            }
        }
    }

    // =========================================================================
    // Exchange Generation — GENERATE_EXCHANGES
    // =========================================================================

    /**
     * Generates all states produced by swapping exactly one player from S1 with
     * one from S2. The relative ordering within each subgroup is preserved.
     *
     * <p>Exchange order: S1[0]↔S2[0], S1[0]↔S2[1], ..., S1[m-1]↔S2[m-1],
     * where {@code m = |S1| = |S2|}. This order matches the FIDE Dutch convention
     * of trying the least disruptive exchanges first.</p>
     *
     * @param s1 the S1 subgroup
     * @param s2 the S2 subgroup
     * @return all possible single-player exchange states
     */
    private List<ExchangeState> generateExchanges(
            List<TournamentPlayer> s1, List<TournamentPlayer> s2) {

        List<ExchangeState> exchanges = new ArrayList<>();

        for (int i = 0; i < s1.size(); i++) {
            for (int j = 0; j < s2.size(); j++) {
                List<TournamentPlayer> newS1 = new ArrayList<>(s1);
                List<TournamentPlayer> newS2 = new ArrayList<>(s2);
                TournamentPlayer tmp = newS1.get(i);
                newS1.set(i, newS2.get(j));
                newS2.set(j, tmp);
                exchanges.add(new ExchangeState(newS1, newS2));
            }
        }

        return exchanges;
    }

    // =========================================================================
    // Validity Check — Is_Valid_Match
    // =========================================================================

    /**
     * Validates an entire S1–S2 pairing configuration by checking each pair
     * {@code (S1[i], S2[i])} against all constraints applicable at the given tolerance.
     * Returns {@code false} on the first constraint violation encountered.
     */
    private boolean isValidMatch(List<TournamentPlayer> s1, List<TournamentPlayer> s2,
                                  Tolerance tolerance, UUID tid) {
        if (s1.size() != s2.size()) return false;

        for (int i = 0; i < s1.size(); i++) {
            if (!validationUtil.isValidPair(s1.get(i), s2.get(i), tolerance, tid, matchRepository)) {
                return false;
            }
        }
        return true;
    }

    // =========================================================================
    // Match Construction
    // =========================================================================

    /**
     * Converts a validated S1–S2 pairing into {@link Match} entity instances.
     * Color assignment per board is determined by {@link ValidationUtil#assignWhiteToFirst}.
     * Board numbers are initialized to {@code 0} and assigned globally afterward.
     */
    private List<Match> buildMatches(List<TournamentPlayer> s1, List<TournamentPlayer> s2,
                                      UUID tid, int roundNo) {
        List<Match> matches = new ArrayList<>();

        for (int i = 0; i < s1.size(); i++) {
            TournamentPlayer tp1 = s1.get(i);
            TournamentPlayer tp2 = s2.get(i);
            boolean tp1White = validationUtil.assignWhiteToFirst(tp1, tp2);

            matches.add(Match.builder()
                    .tid(tid)
                    .roundNo(roundNo)
                    .boardNo(0)
                    .whitePid(tp1White ? tp1.getId().getPid() : tp2.getId().getPid())
                    .blackPid(tp1White ? tp2.getId().getPid() : tp1.getId().getPid())
                    .matchResult(MatchResult.UNPLAYED)
                    .build());
        }

        return matches;
    }

    /**
     * Assigns sequential board numbers (1-indexed) to all non-bye matches.
     * Board 1 is the most prestigious pairing (highest-ranked players).
     */
    private void assignBoardNumbers(List<Match> matches) {
        for (int i = 0; i < matches.size(); i++) {
            matches.get(i).setBoardNo(i + 1);
        }
    }

    // =========================================================================
    // Internal Data Structures
    // =========================================================================

    /**
     * Represents the S1 and S2 subgroups produced by a single player exchange operation.
     */
    private record ExchangeState(List<TournamentPlayer> s1, List<TournamentPlayer> s2) {}

    /**
     * Tracks the pairing resolution state for a single score bracket.
     *
     * <p>Holds the pre-computed ordered list of all valid candidate pairings
     * and an index pointer that advances each time the backtracking algorithm
     * requests a new configuration for this bracket.</p>
     */
    private static class BracketState {

        final List<TournamentPlayer> bracketPlayers;
        final List<List<Match>>      candidates;
        private int                  currentIndex = 0;
        List<Match>                  currentMatches = List.of();

        BracketState(List<TournamentPlayer> bracketPlayers, List<List<Match>> candidates) {
            this.bracketPlayers = bracketPlayers;
            this.candidates     = candidates;
        }

        /** Returns {@code true} if at least one untried candidate remains. */
        boolean hasNext() {
            return currentIndex < candidates.size();
        }

        /** Advances to the next candidate and stores it as {@code currentMatches}. */
        void advance() {
            currentMatches = candidates.get(currentIndex++);
        }
    }
}
