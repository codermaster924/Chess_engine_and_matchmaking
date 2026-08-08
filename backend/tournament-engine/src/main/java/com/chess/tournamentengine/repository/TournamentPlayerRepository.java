package com.chess.tournamentengine.repository;

import com.chess.tournamentengine.model.TournamentPlayer;
import com.chess.tournamentengine.model.TournamentPlayerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access interface for the {@code tournament_players} junction table.
 *
 * <p>The primary repository consumed by the FIDE Dutch pairing engine and the
 * match result service. All pairing-round inputs are derived from queries
 * defined here, and all per-player state updates are persisted through this
 * interface's inherited {@code save()} method.</p>
 */
@Repository
public interface TournamentPlayerRepository extends JpaRepository<TournamentPlayer, TournamentPlayerId> {

    /**
     * Retrieves all enrolled players for a given tournament, ordered by
     * descending score then descending FIDE rating.
     *
     * <p>This ordering produces the deterministic sorted list required as
     * input to the FIDE Dutch bracket partitioning step at the start of
     * every pairing round.</p>
     *
     * @param tid the tournament identifier
     * @return ordered list of {@link TournamentPlayer} records
     */
    @Query("""
            SELECT tp FROM TournamentPlayer tp
            JOIN tp.player p
            WHERE tp.id.tid = :tid
            ORDER BY tp.playerScore DESC, p.fideRating DESC
            """)
    List<TournamentPlayer> findAllByTidOrderByScoreAndRating(@Param("tid") UUID tid);

    /**
     * Returns {@code true} if the specified player is already enrolled in the specified tournament.
     *
     * @param tid the tournament identifier
     * @param pid the player identifier
     * @return {@code true} if an enrollment record exists
     */
    boolean existsByIdTidAndIdPid(UUID tid, UUID pid);

    /**
     * Retrieves the enrollment record for a specific player in a specific tournament.
     *
     * @param tid the tournament identifier
     * @param pid the player identifier
     * @return an {@link Optional} containing the record if found
     */
    @Query("""
            SELECT tp FROM TournamentPlayer tp
            WHERE tp.id.tid = :tid AND tp.id.pid = :pid
            """)
    Optional<TournamentPlayer> findByTidAndPid(@Param("tid") UUID tid, @Param("pid") UUID pid);

    /**
     * Returns the total number of players enrolled in a given tournament.
     *
     * <p>Used to determine bye eligibility when the player count is odd,
     * and to validate that a sufficient number of players exist before
     * generating a round.</p>
     *
     * @param tid the tournament identifier
     * @return count of enrolled players
     */
    long countByIdTid(UUID tid);
}
