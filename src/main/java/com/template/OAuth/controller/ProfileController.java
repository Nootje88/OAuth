package com.template.OAuth.controller;

import com.template.OAuth.dto.*;
import com.template.OAuth.entities.User;
import com.template.OAuth.service.ProfileService;
import com.template.OAuth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@Tag(name = "User Profile", description = "User profile management endpoints")
public class ProfileController {

    private final UserService userService;
    private final ProfileService profileService;

    public ProfileController(UserService userService, ProfileService profileService) {
        this.userService = userService;
        this.profileService = profileService;
    }

    @Operation(summary = "Get current user profile",
            description = "Retrieves the complete profile of the currently authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ExtendedUserDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<ExtendedUserDto> getCurrentUserProfile() {
        User currentUser = userService.getCurrentUser();
        // Record user activity
        userService.recordUserActivity(currentUser);
        return ResponseEntity.ok(profileService.convertToExtendedDto(currentUser));
    }

    @Operation(summary = "Update user profile",
            description = "Updates the profile information of the currently authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = ExtendedUserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated",
                    content = @Content)
    })
    @PutMapping
    public ResponseEntity<ExtendedUserDto> updateProfile(
            @Valid @RequestBody
            @Parameter(description = "Profile data to update", required = true)
            ProfileUpdateDto profileUpdateDto) {

        User updatedUser = profileService.updateProfile(profileUpdateDto);
        return ResponseEntity.ok(profileService.convertToExtendedDto(updatedUser));
    }

    @Operation(summary = "Update notification preferences",
            description = "Updates the notification preferences of the currently authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification preferences updated successfully",
                    content = @Content(schema = @Schema(implementation = ExtendedUserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated",
                    content = @Content)
    })
    @PutMapping("/notifications")
    public ResponseEntity<ExtendedUserDto> updateNotificationPreferences(
            @Valid @RequestBody
            @Parameter(description = "Notification preferences to update", required = true)
            NotificationPreferencesDto preferencesDto) {

        User updatedUser = profileService.updateNotificationPreferences(preferencesDto);
        return ResponseEntity.ok(profileService.convertToExtendedDto(updatedUser));
    }

    @Operation(summary = "Upload profile picture",
            description = "Uploads a new profile picture for the currently authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile picture uploaded successfully",
                    content = @Content(schema = @Schema(implementation = ExtendedUserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file (empty or not an image)",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated",
                    content = @Content)
    })
    @PostMapping(value = "/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExtendedUserDto> uploadProfilePicture(
            @Parameter(description = "Profile picture file (jpg, png, gif)", required = true)
            @RequestParam("file") MultipartFile file)
            throws IOException {

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().build();
        }

        // Validate file size (additional check beyond Spring's multipart config)
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            return ResponseEntity.badRequest().build();
        }

        User updatedUser = profileService.uploadProfilePicture(file);
        return ResponseEntity.ok(profileService.convertToExtendedDto(updatedUser));
    }

    @Operation(summary = "Get profile update history",
            description = "Retrieves the history of changes made to the user's profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile update history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ProfileUpdateHistoryDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated",
                    content = @Content)
    })
    @GetMapping("/history")
    public ResponseEntity<List<ProfileUpdateHistoryDto>> getProfileUpdateHistory() {
        List<ProfileUpdateHistoryDto> history = profileService.getProfileUpdateHistory();
        return ResponseEntity.ok(history);
    }
}