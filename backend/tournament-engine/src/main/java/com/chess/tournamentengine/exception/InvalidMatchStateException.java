package com.chess.tournamentengine.exception;

/**
 * Thrown when a result submission targets a match in an invalid state.
 *
 * <p>Valid transitions are strictly defined: a result may only be submitted
 * for a match whose current {@code matchResult} is {@code UNPLAYED}. Attempting
 * to overwrite an already-recorded result, or to submit a result for a
 * non-existent match, triggers this exception.</p>
 *
 * <p>Mapped to HTTP {@code 409 Conflict} by {@link GlobalExceptionHandler}.</p>
 */
public class InvalidMatchStateException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message description of the invalid state transition
     */
    public InvalidMatchStateException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a detail message and a root cause.
     *
     * @param message description of the invalid state transition
     * @param cause   the underlying cause
     */
    public InvalidMatchStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
