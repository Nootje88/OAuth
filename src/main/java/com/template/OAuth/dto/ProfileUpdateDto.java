package com.template.OAuth.dto;

import com.template.OAuth.enums.ThemePreference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Data transfer object for updating user profile information")
public class ProfileUpdateDto {
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "User's display name", example = "John Doe", maxLength = 100)
    private String name;

    @Size(max = 500, message = "Biography cannot exceed 500 characters")
    @Schema(description = "User's biography or personal description", example = "Software developer with a passion for OAuth technologies", maxLength = 500)
    private String biography;

    @Size(max = 100, message = "Location cannot exceed 100 characters")
    @Schema(description = "User's location information", example = "New York, USA", maxLength = 100)
    private String location;

    @Pattern(regexp = "^$|^[\\+]?[(]?[0-9]{3}[)]?[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{4,6}$",
            message = "Phone number format is invalid")
    @Schema(description = "User's phone number", example = "+1 (123) 456-7890", pattern = "^$|^[\\+]?[(]?[0-9]{3}[)]?[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{4,6}$")
    private String phoneNumber;

    @Email(message = "Alternative email format is invalid")
    @Schema(description = "User's alternative email address", example = "johndoe@example.com", format = "email")
    private String alternativeEmail;

    @Schema(description = "User's preferred application theme", example = "DARK",
            allowableValues = {"LIGHT", "DARK", "SYSTEM"})
    private ThemePreference themePreference;
}