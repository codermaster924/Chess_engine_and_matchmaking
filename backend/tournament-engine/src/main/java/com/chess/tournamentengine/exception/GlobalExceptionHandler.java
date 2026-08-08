package com.chess.tournamentengine.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized exception handler for all REST controllers.
 *
 * <p>Intercepts exceptions propagated from the service and engine layers and
 * converts them into structured JSON error responses. All error bodies follow
 * a consistent schema containing {@code timestamp}, {@code status}, {@code error},
 * and {@code message} fields, making client-side error handling predictable.</p>
 *
 * <p><b>Handled exceptions and their HTTP mappings:</b></p>
 * <ul>
 *   <li>{@link TournamentUnpairableException}      → {@code 422 Unprocessable Entity}</li>
 *   <li>{@link InvalidMatchStateException}          → {@code 409 Conflict}</li>
 *   <li>{@link jakarta.persistence.EntityNotFoundException} → {@code 404 Not Found}</li>
 *   <li>{@link MethodArgumentNotValidException}     → {@code 400 Bad Request}</li>
 *   <li>{@link IllegalArgumentException}            → {@code 400 Bad Request}</li>
 *   <li>{@link Exception} (fallback)                → {@code 500 Internal Server Error}</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles cases where the FIDE algorithm cannot produce a valid pairing.
     *
     * @param ex the unpairable exception
     * @return {@code 422} response with algorithm failure details
     */
    @ExceptionHandler(TournamentUnpairableException.class)
    public ResponseEntity<Map<String, Object>> handleUnpairable(TournamentUnpairableException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Tournament Unpairable", ex.getMessage());
    }

    /**
     * Handles invalid match result state transitions (e.g., overwriting an existing result).
     *
     * @param ex the invalid state exception
     * @return {@code 409} response with conflict details
     */
    @ExceptionHandler(InvalidMatchStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidMatchState(InvalidMatchStateException ex) {
        return build(HttpStatus.CONFLICT, "Invalid Match State", ex.getMessage());
    }

    /**
     * Handles requests for resources that do not exist in the database.
     *
     * @param ex the entity not found exception
     * @return {@code 404} response
     */
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(jakarta.persistence.EntityNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage());
    }

    /**
     * Handles Bean Validation failures on request body fields annotated with
     * {@code @NotNull}, {@code @NotBlank}, {@code @Size}, etc.
     *
     * <p>Aggregates all field-level validation errors into a single response list.</p>
     *
     * @param ex the validation exception containing field error details
     * @return {@code 400} response with a list of violated constraints
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("messages", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles invalid argument conditions raised by the service or engine layer.
     *
     * @param ex the illegal argument exception
     * @return {@code 400} response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    /**
     * Fallback handler for any unhandled exception.
     *
     * <p>Returns a generic {@code 500} response. The actual cause is not exposed
     * to the client to avoid leaking internal implementation details.</p>
     *
     * @param ex the unhandled exception
     * @return {@code 500} response with a generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please contact the administrator.");
    }

    /**
     * Constructs a standard error response body.
     *
     * @param status  the HTTP status to return
     * @param error   a short error category label
     * @param message the detailed error message
     * @return a {@link ResponseEntity} with the structured error body
     */
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
