package com.chess.tournamentengine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entity representing a prize placement within a specific tournament category.
 *
 * <p>Tournaments may distribute prizes across multiple concurrent categories
 * (e.g., Overall, Under-18, Unrated, Best Female). Each row in this table
 * records a single player's rank within one category for one tournament.
 * A player who qualifies for multiple categories will have multiple rows.</p>
 *
 * <p>This table serves as the authoritative source for tournament winners,
 * replacing a single {@code winner_pid} field on the tournament record.</p>
 */
@Entity
@Table(name = "tournament_awards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentAward {

    /** Primary key. Auto-generated UUID, immutable after creation. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "award_id", updatable = false, nullable = false)
    private UUID awardId;

    /** Foreign key referencing {@code tournaments.tid}. */
    @Column(name = "tid", nullable = false)
    private UUID tid;

    /** Foreign key referencing {@code players.pid}. */
    @Column(name = "pid", nullable = false)
    private UUID pid;

    /**
     * Prize category name. Free-text to support organizer-defined categories.
     * Standard values include {@code "Overall"}, {@code "U-18"}, {@code "Unrated"}, {@code "Best Female"}.
     */
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    /**
     * Ordinal rank within the category. {@code 1} denotes first place.
     */
    @Column(name = "standing", nullable = false)
    private int standing;
}
