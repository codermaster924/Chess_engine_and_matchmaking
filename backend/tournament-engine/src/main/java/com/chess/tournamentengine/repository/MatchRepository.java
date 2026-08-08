package com.chess.tournamentengine.repository;

import com.chess.tournamentengine.model.Match;
import com.chess.tournamentengine.model.enums.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Data access interface for the {@code matches} table.
 *
 * <p>Provides board-level and opponent-history queries required by both the
 * FIDE Dutch pairing engine and the match result submission flow. The most
 * critical query, {@link #havePlayedBefore}, enforces the FIDE hard constraint
 * that prohibits rematches within a single tournament.</p>
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    /**
     * Retrieves all board assignments for a given round in a tournament,
     * ordered by ascending board number.
     *
     * @param tid     the tournament identifier
     * @param roundNo the round number
     * @return list of {@link Match} records for the specified round
     */
    List<Match> findByTidAndRoundNoOrderByBoardNoAsc(UUID tid, int roundNo);

    /**
     * Determines whether two players have been paired against each other
     * in any previous round of the specified tournament.
     *
     * <p>Checks both color assignments, since a player may have sat on
     * either side of the board in a prior encounter. A {@code true} result
     * constitutes a FIDE hard constraint violation and must cause the
     * engine to reject the candidate pairing.</p>
     *
     * @param tid  the tournament identifier
     * @param pid1 the first player's identifier
     * @param pid2 the second player's identifier
     * @return {@code true} if the players have previously been paired
     */
    @Query("""
            SELECT COUNT(m) > 0 FROM Match m
            WHERE m.tid = :tid
            AND (
                (m.whitePid = :pid1 AND m.blackPid = :pid2)
                OR
                (m.whitePid = :pid2 AND m.blackPid = :pid1)
            )
            """)
    boolean havePlayedBefore(
            @Param("tid") UUID tid,
            @Param("pid1") UUID pid1,
            @Param("pid2") UUID pid2
    );

    /**
     * Retrieves the full match history for a given player within a tournament,
     * ordered by ascending round number.
     *
     * @param tid the tournament identifier
     * @param pid the player identifier
     * @return chronologically ordered list of matches involving the player
     */
    @Query("""
            SELECT m FROM Match m
            WHERE m.tid = :tid
            AND (m.whitePid = :pid OR m.blackPid = :pid)
            ORDER BY m.roundNo ASC
            """)
    List<Match> findAllMatchesForPlayerInTournament(
            @Param("tid") UUID tid,
            @Param("pid") UUID pid
    );

    /**
     * Returns {@code true} if boards have already been generated for the specified round.
     *
     * <p>Used as a pre-flight check before generating a new round to prevent
     * duplicate pairing of an already-processed round.</p>
     *
     * @param tid     the tournament identifier
     * @param roundNo the round number to check
     * @return {@code true} if at least one board exists for the round
     */
    boolean existsByTidAndRoundNo(UUID tid, int roundNo);

    /**
     * Retrieves all matches in a tournament with the specified result status.
     *
     * <p>Primarily used to detect outstanding {@link MatchResult#UNPLAYED} boards
     * before permitting a new round to be generated.</p>
     *
     * @param tid         the tournament identifier
     * @param matchResult the result status to filter by
     * @return list of matching {@link Match} records
     */
    List<Match> findByTidAndMatchResult(UUID tid, MatchResult matchResult);
}
