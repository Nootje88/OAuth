package com.template.OAuth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor  // ✅ Ensure a default constructor is available
public class AuthResponse {
    private String token;
    private String message;
}
