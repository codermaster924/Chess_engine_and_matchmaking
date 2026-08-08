package com.chess.tournamentengine.model;

import com.chess.tournamentengine.model.enums.MatchResult;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entity representing a single board assignment within a tournament round.
 *
 * <p>A {@code Match} is a weak entity — it exists only in the context of a
 * parent {@link Tournament}. The combination of {@code (tid, round_no, board_no)}
 * is functionally unique within the schema.</p>
 *
 * <p>{@code blackPid} is nullable to accommodate bye rounds, where an odd-numbered
 * player receives a half-point without an opponent. {@code matchResult} defaults
 * to {@link MatchResult#UNPLAYED} at board creation and is updated by an arbiter
 * via {@code PUT /api/v1/matches/{match_id}/result}.</p>
 */
@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    /** Primary key. Auto-generated UUID, immutable after creation. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "match_id", updatable = false, nullable = false)
    private UUID matchId;

    /** Foreign key referencing the parent {@code tournaments.tid}. */
    @Column(name = "tid", nullable = false)
    private UUID tid;

    /** Round number within the tournament (1-indexed). */
    @Column(name = "round_no", nullable = false)
    private int roundNo;

    /** Physical board number at the venue. Lower numbers denote higher-stakes boards. */
    @Column(name = "board_no", nullable = false)
    private int boardNo;

    /** Foreign key referencing the player assigned to the White pieces. */
    @Column(name = "white_pid", nullable = false)
    private UUID whitePid;

    /**
     * Foreign key referencing the player assigned to the Black pieces.
     * {@code NULL} for bye rounds where no opponent is assigned.
     */
    @Column(name = "black_pid")
    private UUID blackPid;

    /**
     * Outcome of this board match, persisted as its enum name (e.g., {@code "DRAW"}).
     * Defaults to {@link MatchResult#UNPLAYED} at the time of board generation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "match_result", nullable = false, length = 50)
    private MatchResult matchResult = MatchResult.UNPLAYED;
}
