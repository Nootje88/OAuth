package com.template.OAuth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/premium")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM')")
public class PremiumController {

    @GetMapping("/content")
    public ResponseEntity<Map<String, String>> getPremiumContent() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Welcome to premium content");
        response.put("content", "This is premium content available only to premium users");
        return ResponseEntity.ok(response);
    }
}