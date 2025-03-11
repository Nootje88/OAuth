package com.template.OAuth.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private List<String> errors = new ArrayList<>();
    private String message;
    private Map<String, String> fieldErrors;

    public ValidationErrorResponse(String message) {
        this.message = message;
    }

    public ValidationErrorResponse(String message, Map<String, String> errorMap) {
        this.message = message;
        this.fieldErrors = errorMap;
        this.errors = errorMap.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.toList());
    }
}