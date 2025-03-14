package com.template.OAuth.validation;

import com.template.OAuth.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @Autowired
    public GlobalExceptionHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();

            // Get localized error message if message code is available
            String errorMessage;
            if (error.getDefaultMessage() != null && error.getDefaultMessage().startsWith("{")
                    && error.getDefaultMessage().endsWith("}")) {
                // Extract message code from {code}
                String messageKey = error.getDefaultMessage().substring(1, error.getDefaultMessage().length() - 1);
                errorMessage = messageService.getMessage(messageKey);
            } else {
                errorMessage = error.getDefaultMessage();
            }

            errors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse response = new ValidationErrorResponse(
                messageService.getMessage("validation.error"),
                errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> errorResponse = new HashMap<>();

        // Try to map exception message to a message key, or use a default
        String messageKey = "error.unknown";
        if (ex.getMessage() != null) {
            if (ex.getMessage().equals("User not found")) {
                messageKey = "user.not.found";
            } else if (ex.getMessage().contains("Token")) {
                messageKey = "auth.token.invalid";
            }
            // Add more mappings as needed
        }

        errorResponse.put("message", messageService.getMessage(messageKey));
        errorResponse.put("error", ex.getClass().getSimpleName());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}