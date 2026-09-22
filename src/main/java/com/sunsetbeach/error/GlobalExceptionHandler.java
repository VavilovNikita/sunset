package com.sunsetbeach.error;

import com.sunsetbeach.model.ErrorMessage;
import com.sunsetbeach.model.ValidationError;
import com.sunsetbeach.model.ValidationErrorError;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> formErrors = new ArrayList<>();
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                fieldErrors
                        .computeIfAbsent(fieldError.getField(), key -> new ArrayList<>())
                        .add(fieldError.getDefaultMessage());
            } else {
                formErrors.add(error.getDefaultMessage());
            }
        });
        return ResponseEntity.badRequest().body(toValidationError(formErrors, fieldErrors));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationError> handleValidation(ValidationException ex) {
        return ResponseEntity.badRequest().body(toValidationError(ex.getFormErrors(), ex.getFieldErrors()));
    }

    /**
     * A required @RequestParam (e.g. GET /payments/summary's from/to) missing from the request.
     * Without this handler Spring's own default 400 body would be a different shape than every
     * other validation failure in this API.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ValidationError> handleMissingParameter(MissingServletRequestParameterException ex) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(ex.getParameterName(), List.of("is required"));
        return ResponseEntity.badRequest().body(toValidationError(new ArrayList<>(), fieldErrors));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorMessage> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorMessage> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorMessage> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorMessage> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorMessage> handleTooManyRequests(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler({BadRequestException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErrorMessage> handleBadRequest(Exception ex) {
        String message = ex instanceof MissingServletRequestPartException ? "No files provided" : ex.getMessage();
        return ResponseEntity.badRequest().body(new ErrorMessage(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorMessage> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(new ErrorMessage("Invalid request parameters"));
    }

    /**
     * Last-resort backstop for a database failure nobody translated. Most writes that can
     * genuinely conflict already recognise their own failure by SQLSTATE right where it happens
     * ({@code BookingService#isSerializationFailure}, {@code SpaAppointmentService#translateOverlap}
     * - see {@link SqlStates} and CLAUDE.md's Concurrency section) and throw a {@link
     * ConflictException} well before it would ever reach here; this handler is deliberately not
     * where a "try again" conflict is supposed to be recognised, only where one that nobody
     * recognised ends up. Two things it must not do: claim a specific, known-safe-to-retry
     * conflict it hasn't actually identified - a 500, not a 409, so this can never be mistaken for
     * the same guarantee {@link #handleConflict} gives - and put the real database message, which
     * can carry table/column/constraint names or query fragments, in front of whoever is standing
     * at the desk. The real exception is logged here, in full, so a developer can still diagnose
     * it later; the response carries neither the message nor the exception type.
     */
    @ExceptionHandler({DataAccessException.class, TransactionSystemException.class})
    public ResponseEntity<ErrorMessage> handleUnrecognizedDatabaseFailure(Exception ex) {
        log.error("Unrecognized database failure", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorMessage("Something went wrong saving that. Please try again, or tell an administrator if it keeps happening."));
    }

    private static ValidationError toValidationError(List<String> formErrors, Map<String, List<String>> fieldErrors) {
        ValidationErrorError error = new ValidationErrorError();
        error.setFormErrors(formErrors);
        error.setFieldErrors(fieldErrors);
        return new ValidationError(error);
    }
}
