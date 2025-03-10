package com.template.OAuth.dto;

import com.template.OAuth.enums.AuthProvider;
import com.template.OAuth.enums.NotificationType;
import com.template.OAuth.enums.Role;
import com.template.OAuth.enums.ThemePreference;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
public class ExtendedUserDto {
    private Long id;
    private String name;
    private String email;
    private String picture;
    private AuthProvider primaryProvider;
    private Set<Role> roles;

    // Extended profile information
    private String biography;
    private String location;
    private String phoneNumber;
    private String alternativeEmail;

    // User preferences
    private ThemePreference themePreference;
    private Set<NotificationType> enabledNotifications;

    // Activity information
    private Instant registrationDate;
    private Instant lastLoginDate;
    private Instant lastActiveDate;
    private Integer loginCount;
    private Integer profileUpdateCount;
}