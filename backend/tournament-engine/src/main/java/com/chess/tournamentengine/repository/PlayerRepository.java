package com.chess.tournamentengine.repository;

import com.chess.tournamentengine.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Data access interface for the {@code players} table.
 *
 * <p>Provides CRUD operations against the global player registry.
 * All player lookups are performed by primary key ({@code pid}),
 * which is handled natively by the inherited {@code JpaRepository} methods.</p>
 */
@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {
}
