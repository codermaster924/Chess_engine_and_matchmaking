package com.chess.tournamentengine.model.enums;

/**
 * Enumeration of all valid outcomes for a {@link com.chess.tournamentengine.model.Match}.
 *
 * <p>Restricting results to this fixed set enforces data integrity at the application level,
 * preventing arbitrary strings from entering the database via the result submission endpoint.</p>
 *
 * <ul>
 *   <li>{@code WHITE_WIN_PLAYED}   - White wins by conclusion of play.</li>
 *   <li>{@code BLACK_WIN_PLAYED}   - Black wins by conclusion of play.</li>
 *   <li>{@code DRAW}               - Game ends in a draw by agreement or adjudication.</li>
 *   <li>{@code WHITE_WIN_FORFEIT}  - Black fails to appear; White is awarded the point.</li>
 *   <li>{@code BLACK_WIN_FORFEIT}  - White fails to appear; Black is awarded the point.</li>
 *   <li>{@code UNPLAYED}           - Default state; board is scheduled but result not yet entered.</li>
 * </ul>
 */
public enum MatchResult {
    WHITE_WIN_PLAYED,
    BLACK_WIN_PLAYED,
    DRAW,
    WHITE_WIN_FORFEIT,
    BLACK_WIN_FORFEIT,
    UNPLAYED
}
