package com.template.OAuth.dto;

import com.template.OAuth.enums.ThemePreference;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateDto {
    private String name;
    private String biography;
    private String location;
    private String phoneNumber;
    private String alternativeEmail;
    private ThemePreference themePreference;
}