package com.template.OAuth.controller;

import com.template.OAuth.dto.*;
import com.template.OAuth.entities.User;
import com.template.OAuth.service.MessageService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@Tag(name = "User Profile", description = "User profile management endpoints")
public class ProfileController {

    private final UserService userService;
    private final ProfileService profileService;
    private final MessageService messageService;

    @Autowired
    public ProfileController(UserService userService, ProfileService profileService, MessageService messageService) {
        this.userService = userService;
        this.profileService = profileService;
        this.messageService = messageService;
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
    public ResponseEntity<Map<String, Object>> updateProfile(
            @Valid @RequestBody
            @Parameter(description = "Profile data to update", required = true)
            ProfileUpdateDto profileUpdateDto) {

        User updatedUser = profileService.updateProfile(profileUpdateDto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", messageService.getMessage("profile.updated"));
        response.put("user", profileService.convertToExtendedDto(updatedUser));

        return ResponseEntity.ok(response);
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
    public ResponseEntity<Map<String, Object>> updateNotificationPreferences(
            @Valid @RequestBody
            @Parameter(description = "Notification preferences to update", required = true)
            NotificationPreferencesDto preferencesDto) {

        User updatedUser = profileService.updateNotificationPreferences(preferencesDto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", messageService.getMessage("profile.updated"));
        response.put("user", profileService.convertToExtendedDto(updatedUser));

        return ResponseEntity.ok(response);
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
    public ResponseEntity<?> uploadProfilePicture(
            @Parameter(description = "Profile picture file (jpg, png, gif)", required = true)
            @RequestParam("file") MultipartFile file)
            throws IOException {

        // Validate file
        if (file.isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", messageService.getMessage("profile.picture.invalid"));
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", messageService.getMessage("profile.picture.invalid"));
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Validate file size (additional check beyond Spring's multipart config)
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", messageService.getMessage("profile.picture.invalid"));
            return ResponseEntity.badRequest().body(errorResponse);
        }

        User updatedUser = profileService.uploadProfilePicture(file);

        Map<String, Object> response = new HashMap<>();
        response.put("message", messageService.getMessage("profile.picture.uploaded"));
        response.put("user", profileService.convertToExtendedDto(updatedUser));

        return ResponseEntity.ok(response);
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
    public ResponseEntity<Map<String, Object>> getProfileUpdateHistory() {
        List<ProfileUpdateHistoryDto> history = profileService.getProfileUpdateHistory();

        Map<String, Object> response = new HashMap<>();
        response.put("message", messageService.getMessage("profile.history.retrieved"));
        response.put("history", history);

        return ResponseEntity.ok(response);
    }
}