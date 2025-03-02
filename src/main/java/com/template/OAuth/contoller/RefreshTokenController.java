package com.template.OAuth.controller;

import com.template.OAuth.dto.RefreshTokenRequest;
import com.template.OAuth.dto.RefreshTokenResponse;
import com.template.OAuth.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/refresh-token")
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    public RefreshTokenController(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping
    public ResponseEntity<RefreshTokenResponse> refreshAccessToken(@RequestBody RefreshTokenRequest request, HttpServletResponse response) {
        RefreshTokenResponse refreshTokenResponse = refreshTokenService.refreshToken(request.getRefreshToken(), response);
        return ResponseEntity.ok(refreshTokenResponse);
    }
}
