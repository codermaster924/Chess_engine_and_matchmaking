package com.chess.tournamentengine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Junction entity resolving the many-to-many relationship between
 * {@link Player} and {@link Tournament}.
 *
 * <p>Beyond recording enrollment, this entity is the primary state store
 * for the FIDE Dutch pairing algorithm. All four algorithm-sensitive fields
 * are read at the start of every pairing round and updated atomically
 * upon result entry via {@code MatchService}.</p>
 *
 * <p><b>Algorithm fields:</b></p>
 * <ul>
 *   <li>{@code playerScore}     - Cumulative points; determines score bracket membership.</li>
 *   <li>{@code colorDiff}       - Net color balance (White games − Black games); must remain ≤ |2|.</li>
 *   <li>{@code consecutiveSame} - Consecutive same-color counter; enforces the FIDE 3-in-a-row hard rule.</li>
 *   <li>{@code hasReceivedBye}  - Bye eligibility flag; a player may receive at most one bye per tournament.</li>
 * </ul>
 */
@Entity
@Table(name = "tournament_players")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentPlayer {

    /**
     * Composite primary key {@code (tid, pid)}.
     * {@code @EmbeddedId} instructs JPA to use {@link TournamentPlayerId} as the key object.
     */
    @EmbeddedId
    private TournamentPlayerId id;

    /**
     * Owning-side association to {@link Tournament}.
     * {@code @MapsId("tid")} shares the FK column with the embedded primary key,
     * preventing a duplicate {@code tid} column in the schema.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tid")
    @JoinColumn(name = "tid")
    private Tournament tournament;

    /**
     * Owning-side association to {@link Player}.
     * {@code @MapsId("pid")} shares the FK column with the embedded primary key.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("pid")
    @JoinColumn(name = "pid")
    private Player player;

    /**
     * Cumulative score accumulated by this player in the current tournament.
     * Win = 1.0, Draw = 0.5, Loss = 0.0, Bye = 0.5.
     */
    @Column(name = "player_score", nullable = false)
    private double playerScore = 0.0;

    /**
     * Net color balance: {@code (White games played) − (Black games played)}.
     * A negative value indicates a preference for White in the next round;
     * a positive value indicates a preference for Black.
     */
    @Column(name = "color_diff", nullable = false)
    private int colorDiff = 0;

    /**
     * Consecutive same-color counter.
     * Positive values represent consecutive White games; negative values represent consecutive Black games.
     * A value of {@code ±2} triggers the FIDE hard constraint preventing a third consecutive same color.
     */
    @Column(name = "consecutive_same", nullable = false)
    private int consecutiveSame = 0;

    /**
     * Indicates whether this player has already received a bye in this tournament.
     * Per FIDE regulations, a player may receive at most one bye per tournament.
     */
    @Column(name = "has_received_bye", nullable = false)
    private boolean hasReceivedBye = false;
}
