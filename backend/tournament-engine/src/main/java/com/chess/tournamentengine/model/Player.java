package com.chess.tournamentengine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Represents a globally registered chess player.
 
 *
 * This is the static identity table — it stores information
 * that belongs to a player regardless of which tournament they are in.
 * Tournament-specific state (score, color history, etc.) lives in
 * TournamentPlayer (the junction entity).
 */
@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    /**
     * Unique player identifier.
     * Generated as a UUID so IDs are globally unique and safe to expose in URLs.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pid", updatable = false, nullable = false)
    private UUID pid;

    /** Full name of the player. */
    @Column(name = "p_name", nullable = false, length = 100)
    private String pName;

    /** Player's geographic state or region. Optional. */
    @Column(name = "state", length = 50)
    private String state;
    //age of the player
    @Column(name = "age", nullable = false)
    private int age;
    //city of the player
    @Column(name = "city", nullable = false)
    private String city;
    //country of the player
    @Column(name = "country", nullable = false)
    private String country;

    //gender
    @Column(name = "gender", nullable = false, length = 1)
    private String gender;
    
    /**
     * Global FIDE rating.
     * Defaults to 0 for unrated players.
     * This rating is used for initial seeding/sorting when scores are tied.
     */
    @Column(name = "fide_rating", nullable = false)
    private int fideRating = 0;
}
