package com.chess.tournamentengine.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite primary key for the {@link TournamentPlayer} junction entity.
 *
 * <p>JPA mandates that composite primary keys reside in a dedicated {@code @Embeddable}
 * class implementing {@link Serializable}. The pair {@code (tid, pid)} uniquely identifies
 * one player's enrollment in one specific tournament.</p>
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentPlayerId implements Serializable {

    /** Foreign key referencing {@code tournaments.tid}. */
    @Column(name = "tid", nullable = false)
    private UUID tid;

    /** Foreign key referencing {@code players.pid}. */
    @Column(name = "pid", nullable = false)
    private UUID pid;
}
