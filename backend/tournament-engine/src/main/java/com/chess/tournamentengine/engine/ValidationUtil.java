package com.chess.tournamentengine.engine;

import com.chess.tournamentengine.model.TournamentPlayer;
import com.chess.tournamentengine.repository.MatchRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stateless helper component for evaluating FIDE pairing constraints.
 *
 * <p>Provides the {@code Is_Valid_Match} logic from the formal pseudocode, decomposed
 * into individual constraint checks. All methods are pure with respect to the
 * caller — no state is modified and no persistence calls are made except through
 * the explicitly passed {@link MatchRepository} parameter.</p>
 *
 * <p><b>Color preference encoding:</b></p>
 * <ul>
 *   <li>{@code 'W'} - Player prefers or requires White.</li>
 *   <li>{@code 'B'} - Player prefers or requires Black.</li>
 *   <li>{@code 'N'} - Player has no color preference (neutral).</li>
 * </ul>
 */
@Component
public class ValidationUtil {

    /**
     * Derives the color preference for a player based on their current tournament state.
     *
     * <p>A preference is a <em>hard requirement</em> when {@code |consecutiveSame| == 2}
     * and a <em>soft preference</em> otherwise.</p>
     *
     * @param tp the player's tournament state
     * @return {@code 'W'}, {@code 'B'}, or {@code 'N'}
     */
    public char getPreferredColor(TournamentPlayer tp) {
        int consec = tp.getConsecutiveSame();
        int diff   = tp.getColorDiff();

        if (consec >= 2)  return 'B';   // Hard: a third White would violate FIDE rules
        if (consec <= -2) return 'W';   // Hard: a third Black would violate FIDE rules
        if (diff < 0)     return 'W';   // Soft: more Black games played, prefers White
        if (diff > 0)     return 'B';   // Soft: more White games played, prefers Black
        return 'N';
    }

    /**
     * Returns {@code true} if assigning the given color to this player would cause them
     * to play the same color three consecutive rounds, violating a FIDE hard rule.
     *
     * @param tp          the player's tournament state
     * @param assignWhite {@code true} if White is to be assigned, {@code false} for Black
     * @return {@code true} if the assignment would trigger a three-in-a-row violation
     */
    public boolean wouldForceThirdSameColor(TournamentPlayer tp, boolean assignWhite) {
        int consec = tp.getConsecutiveSame();
        return (assignWhite && consec >= 2) || (!assignWhite && consec <= -2);
    }

    /**
     * Returns {@code true} if both players have a soft preference for the same color.
     * This check is only evaluated at {@link Tolerance#PERFECT_COLORS}.
     *
     * @param tp1 first player's tournament state
     * @param tp2 second player's tournament state
     * @return {@code true} if preferences conflict at the soft level
     */
    public boolean hasSoftColorConflict(TournamentPlayer tp1, TournamentPlayer tp2) {
        char p1 = getPreferredColor(tp1);
        char p2 = getPreferredColor(tp2);
        if (p1 == 'N' || p2 == 'N') return false;
        return p1 == p2;
    }

    /**
     * Determines whether {@code tp1} should receive White in a pairing with {@code tp2}.
     *
     * <p>Resolution priority:</p>
     * <ol>
     *   <li>Hard color requirements ({@code |consecutiveSame| == 2}) — honored unconditionally.</li>
     *   <li>Soft color preferences ({@code colorDiff} imbalance).</li>
     *   <li>Default: {@code tp1} (drawn from S1, the higher-ranked subgroup) receives White.</li>
     * </ol>
     *
     * @param tp1 first player (S1 representative)
     * @param tp2 second player (S2 representative)
     * @return {@code true} if tp1 receives White
     */
    public boolean assignWhiteToFirst(TournamentPlayer tp1, TournamentPlayer tp2) {
        char pref1 = getPreferredColor(tp1);
        char pref2 = getPreferredColor(tp2);
        boolean hard1 = Math.abs(tp1.getConsecutiveSame()) >= 2;
        boolean hard2 = Math.abs(tp2.getConsecutiveSame()) >= 2;

        // Hard requirements take precedence
        if (hard1 && pref1 == 'W') return true;
        if (hard1 && pref1 == 'B') return false;
        if (hard2 && pref2 == 'W') return false; // tp2 must be White → tp1 is Black
        if (hard2 && pref2 == 'B') return true;  // tp2 must be Black → tp1 is White

        // Soft preferences
        if (pref1 == 'W' && pref2 != 'W') return true;
        if (pref1 == 'B' && pref2 != 'B') return false;
        if (pref2 == 'W' && pref1 != 'W') return false;
        if (pref2 == 'B' && pref1 != 'B') return true;

        return true; // Default: tp1 receives White
    }

    /**
     * Full validity check for a candidate pair under the given tolerance level.
     *
     * <p><b>Hard constraints</b> (enforced at all tolerance levels):</p>
     * <ul>
     *   <li>Players must not have faced each other previously in this tournament.</li>
     *   <li>Color assignment must not cause a third consecutive same color for either player.</li>
     * </ul>
     *
     * <p><b>Soft constraint</b> (enforced only at {@link Tolerance#PERFECT_COLORS}):</p>
     * <ul>
     *   <li>Color preferences must not conflict between the two players.</li>
     * </ul>
     *
     * @param tp1            first player (from S1)
     * @param tp2            second player (from S2)
     * @param tolerance      current constraint relaxation level
     * @param tid            tournament identifier (required for rematch check)
     * @param matchRepository repository used to query match history
     * @return {@code true} if the pairing is valid under all applicable constraints
     */
    public boolean isValidPair(TournamentPlayer tp1, TournamentPlayer tp2,
                                Tolerance tolerance, UUID tid, MatchRepository matchRepository) {
        // Hard constraint: no rematches
        if (matchRepository.havePlayedBefore(tid, tp1.getId().getPid(), tp2.getId().getPid())) {
            return false;
        }

        // Determine color assignment before checking color-related hard rules
        boolean tp1GetsWhite = assignWhiteToFirst(tp1, tp2);

        // Hard constraint: no third consecutive same color for either player
        if (wouldForceThirdSameColor(tp1, tp1GetsWhite))  return false;
        if (wouldForceThirdSameColor(tp2, !tp1GetsWhite)) return false;

        // Soft constraint: color preference conflict (only enforced at PERFECT_COLORS)
        if (tolerance == Tolerance.PERFECT_COLORS && hasSoftColorConflict(tp1, tp2)) {
            return false;
        }

        return true;
    }
}
