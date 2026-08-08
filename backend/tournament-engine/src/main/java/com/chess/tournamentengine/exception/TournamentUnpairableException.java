package com.chess.tournamentengine.exception;

/**
 * Thrown when the FIDE Dutch pairing algorithm exhausts all valid configurations
 * for a score bracket and its backtracking cascade reaches the top of the bracket
 * stack without resolution.
 *
 * <p>This exception represents a mathematically unpairable tournament state —
 * a condition that can arise when hard constraints (no rematches, no third
 * consecutive same color) cannot be simultaneously satisfied across all players.</p>
 *
 * <p>Mapped to HTTP {@code 422 Unprocessable Entity} by {@link GlobalExceptionHandler}.</p>
 */
public class TournamentUnpairableException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message description of the unpairable condition
     */
    public TournamentUnpairableException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a detail message and a root cause.
     *
     * @param message description of the unpairable condition
     * @param cause   the underlying cause
     */
    public TournamentUnpairableException(String message, Throwable cause) {
        super(message, cause);
    }
}
