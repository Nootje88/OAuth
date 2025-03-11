package com.template.OAuth.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private List<String> errors = new ArrayList<>();
    private String message;

    public ValidationErrorResponse(String message) {
        this.message = message;
    }

    public ValidationErrorResponse(Map<String, String> errorMap) {
        this.message = "Validation failed";
        errorMap.forEach((field, error) -> errors.add(field + ": " + error));
    }
}