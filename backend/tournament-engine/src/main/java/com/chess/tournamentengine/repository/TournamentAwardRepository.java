package com.chess.tournamentengine.repository;

import com.chess.tournamentengine.model.TournamentAward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Data access interface for the {@code tournament_awards} table.
 *
 * <p>Manages the persistence and retrieval of prize placement records
 * across all categories within a tournament. This table is the authoritative
 * source for determining tournament winners, supporting multi-category
 * prize distributions.</p>
 */
@Repository
public interface TournamentAwardRepository extends JpaRepository<TournamentAward, UUID> {

    /**
     * Retrieves all award placements for a given tournament, ordered by
     * category name then standing (ascending).
     *
     * @param tid the tournament identifier
     * @return list of {@link TournamentAward} records ordered by category and rank
     */
    List<TournamentAward> findByTidOrderByCategoryAscStandingAsc(UUID tid);

    /**
     * Retrieves all awards received by a specific player in a given tournament.
     *
     * @param tid the tournament identifier
     * @param pid the player identifier
     * @return list of {@link TournamentAward} records for the player
     */
    List<TournamentAward> findByTidAndPid(UUID tid, UUID pid);

    /**
     * Retrieves all placements within a specific prize category in a tournament,
     * ordered by ascending standing.
     *
     * @param tid      the tournament identifier
     * @param category the prize category name (e.g., {@code "U-18"}, {@code "Overall"})
     * @return ranked list of {@link TournamentAward} records for the category
     */
    List<TournamentAward> findByTidAndCategoryOrderByStandingAsc(UUID tid, String category);
}
