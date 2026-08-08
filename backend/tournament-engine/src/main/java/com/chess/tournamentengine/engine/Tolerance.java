package com.chess.tournamentengine.engine;

/**
 * Defines the three tolerance levels used during FIDE Dutch bracket solving.
 *
 * <p>The algorithm iterates through these levels in order. A stricter level is
 * always attempted before a more relaxed one. Hard constraints (no rematches,
 * no third consecutive same color) are enforced at every level regardless of
 * the current tolerance setting.</p>
 *
 * <ul>
 *   <li>{@code PERFECT_COLORS}  - All soft color preferences must be satisfied.</li>
 *   <li>{@code RELAXED_COLORS}  - Soft color preferences may be violated; only hard color rules apply.</li>
 *   <li>{@code IGNORE_COLORS}   - Color balance entirely ignored; only the no-third-consecutive hard rule is enforced.</li>
 * </ul>
 */
public enum Tolerance {
    PERFECT_COLORS,
    RELAXED_COLORS,
    IGNORE_COLORS
}
