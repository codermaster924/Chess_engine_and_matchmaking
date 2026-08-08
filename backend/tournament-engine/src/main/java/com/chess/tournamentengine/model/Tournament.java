package com.chess.tournamentengine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entity representing a tournament instance and its static configuration.
 *
 * <p>Defines the top-level container for a chess tournament. Stores immutable
 * configuration parameters set at tournament creation time. Dynamic per-player
 * state is maintained in {@link TournamentPlayer}. Prize outcomes are recorded
 * in {@link TournamentAward}.</p>
 */
@Entity
@Table(name = "tournaments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament {

    /** Primary key. Auto-generated UUID, immutable after creation. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tid", updatable = false, nullable = false)
    private UUID tid;

    /** Name or physical address of the venue hosting the tournament. */
    @Column(name = "t_venue", nullable = false, length = 255)
    private String tVenue;

    /** Optional link or descriptive text for the tournament brochure. */
    @Column(name = "brochure", columnDefinition = "TEXT")
    private String brochure;

    /**
     * Tournament format. For example: {@code "Swiss"}, {@code "Round Robin"}.
     * The current engine implementation targets Swiss pairing exclusively.
     */
    @Column(name = "t_format", nullable = false, length = 50)
    private String tFormat;

    /**
     * FIDE time control notation for each game.
     * Examples: {@code "90+30"} (classical), {@code "15+10"} (rapid), {@code "3+2"} (blitz).
     */
    @Column(name = "time_control", nullable = false, length = 50)
    private String timeControl;
}
