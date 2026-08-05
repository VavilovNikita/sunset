package com.sunsetbeach.error;

import com.sunsetbeach.model.ErrorMessage;
import com.sunsetbeach.model.ValidationError;
import com.sunsetbeach.model.ValidationErrorError;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    private static ValidationError toValidationError(List<String> formErrors, Map<String, List<String>> fieldErrors) {
        ValidationErrorError error = new ValidationErrorError();
        error.setFormErrors(formErrors);
        error.setFieldErrors(fieldErrors);
        return new ValidationError(error);
    }
}
