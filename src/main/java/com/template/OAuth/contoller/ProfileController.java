package com.template.OAuth.controller;

import com.template.OAuth.dto.*;
import com.template.OAuth.entities.User;
import com.template.OAuth.service.ProfileService;
import com.template.OAuth.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ProfileController {

    private final UserService userService;
    private final ProfileService profileService;

    public ProfileController(UserService userService, ProfileService profileService) {
        this.userService = userService;
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ExtendedUserDto> getCurrentUserProfile() {
        User currentUser = userService.getCurrentUser();
        // Record user activity
        userService.recordUserActivity(currentUser);
        return ResponseEntity.ok(profileService.convertToExtendedDto(currentUser));
    }

    @PutMapping
    public ResponseEntity<ExtendedUserDto> updateProfile(@RequestBody ProfileUpdateDto profileUpdateDto) {
        User updatedUser = profileService.updateProfile(profileUpdateDto);
        return ResponseEntity.ok(profileService.convertToExtendedDto(updatedUser));
    }

    @PutMapping("/notifications")
    public ResponseEntity<ExtendedUserDto> updateNotificationPreferences(
            @RequestBody NotificationPreferencesDto preferencesDto) {
        User updatedUser = profileService.updateNotificationPreferences(preferencesDto);
        return ResponseEntity.ok(profileService.convertToExtendedDto(updatedUser));
    }

    @PostMapping(value = "/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExtendedUserDto> uploadProfilePicture(@RequestParam("file") MultipartFile file)
            throws IOException {
        User updatedUser = profileService.uploadProfilePicture(file);
        return ResponseEntity.ok(profileService.convertToExtendedDto(updatedUser));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ProfileUpdateHistoryDto>> getProfileUpdateHistory() {
        List<ProfileUpdateHistoryDto> history = profileService.getProfileUpdateHistory();
        return ResponseEntity.ok(history);
    }
}