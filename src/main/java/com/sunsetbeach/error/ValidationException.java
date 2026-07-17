package com.sunsetbeach.error;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 400 with the Zod {@code error.flatten()} shape: {@code { formErrors: [], fieldErrors: {} } }.
 * Used for the cross-field refinements Zod expresses via {@code .refine()} (from &lt;= to,
 * checkIn &lt; checkOut) - not expressible via Bean Validation, so they're checked manually
 * in the service layer and thrown here.
 */
public class ValidationException extends RuntimeException {

    private final List<String> formErrors;
    private final Map<String, List<String>> fieldErrors;

    public ValidationException(List<String> formErrors, Map<String, List<String>> fieldErrors) {
        super(formErrors.isEmpty() ? fieldErrors.toString() : String.join("; ", formErrors));
        this.formErrors = formErrors;
        this.fieldErrors = fieldErrors;
    }

    public static ValidationException field(String field, String message) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put(field, List.of(message));
        return new ValidationException(new ArrayList<>(), fieldErrors);
    }

    public List<String> getFormErrors() {
        return formErrors;
    }

    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }
}
