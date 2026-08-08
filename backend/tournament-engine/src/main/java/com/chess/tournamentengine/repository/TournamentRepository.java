package com.chess.tournamentengine.repository;

import com.chess.tournamentengine.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Data access interface for the {@code tournaments} table.
 *
 * <p>Manages persistence of tournament configuration records.
 * Player enrollment and per-tournament runtime state are handled
 * by {@link TournamentPlayerRepository}, not this interface.</p>
 */
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, UUID> {
}
