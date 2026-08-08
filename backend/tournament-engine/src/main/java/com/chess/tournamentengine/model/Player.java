package com.chess.tournamentengine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entity representing a globally registered chess player.
 *
 * <p>Stores static identity information that belongs to a player across all tournaments.
 * Tournament-specific runtime state (score, color history, bye status) is maintained
 * separately in {@link TournamentPlayer}.</p>
 */
@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    /** Primary key. Auto-generated UUID, immutable after creation. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pid", updatable = false, nullable = false)
    private UUID pid;

    /** Full legal name of the player. */
    @Column(name = "p_name", nullable = false, length = 100)
    private String pName;

    /** Geographic state or region the player represents. */
    @Column(name = "state", length = 50)
    private String state;

    /** Player's age in years. Used for age-category prize eligibility. */
    @Column(name = "age", nullable = false)
    private int age;

    /** City of residence. */
    @Column(name = "city", nullable = false)
    private String city;

    /** Country of residence or federation affiliation. */
    @Column(name = "country", nullable = false)
    private String country;

    /**
     * Gender identifier. Single character: {@code "M"}, {@code "F"}, or {@code "O"}.
     * Used for gender-category prize eligibility.
     */
    @Column(name = "gender", nullable = false, length = 1)
    private String gender;

    /**
     * Global FIDE rating. Defaults to {@code 0} for unrated players.
     * Used as a tiebreaker when players share the same score bracket during pairing.
     */
    @Column(name = "fide_rating", nullable = false)
    private int fideRating = 0;
}
