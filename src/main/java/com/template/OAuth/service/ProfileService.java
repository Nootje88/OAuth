package com.template.OAuth.service;

import com.template.OAuth.dto.ExtendedUserDto;
import com.template.OAuth.dto.NotificationPreferencesDto;
import com.template.OAuth.dto.ProfileUpdateDto;
import com.template.OAuth.dto.ProfileUpdateHistoryDto;
import com.template.OAuth.entities.ProfileUpdateHistory;
import com.template.OAuth.entities.User;
import com.template.OAuth.repositories.ProfileUpdateHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    @Autowired
    private UserService userService;

    @Autowired
    private ProfileUpdateHistoryRepository profileUpdateHistoryRepository;

    @Value("${app.profile.upload-dir:uploads/profiles}")
    private String uploadDir;

    @Transactional
    public User updateProfile(ProfileUpdateDto profileUpdateDto) {
        User currentUser = userService.getCurrentUser();

        // Track profile changes and update history
        if (profileUpdateDto.getName() != null && !profileUpdateDto.getName().equals(currentUser.getName())) {
            currentUser.addUpdateHistory("name", currentUser.getName(), profileUpdateDto.getName());
            currentUser.setName(profileUpdateDto.getName());
        }

        if (profileUpdateDto.getBiography() != null && !profileUpdateDto.getBiography().equals(currentUser.getBiography())) {
            currentUser.addUpdateHistory("biography", currentUser.getBiography(), profileUpdateDto.getBiography());
            currentUser.setBiography(profileUpdateDto.getBiography());
        }

        if (profileUpdateDto.getLocation() != null && !profileUpdateDto.getLocation().equals(currentUser.getLocation())) {
            currentUser.addUpdateHistory("location", currentUser.getLocation(), profileUpdateDto.getLocation());
            currentUser.setLocation(profileUpdateDto.getLocation());
        }

        if (profileUpdateDto.getPhoneNumber() != null && !profileUpdateDto.getPhoneNumber().equals(currentUser.getPhoneNumber())) {
            currentUser.addUpdateHistory("phoneNumber", currentUser.getPhoneNumber(), profileUpdateDto.getPhoneNumber());
            currentUser.setPhoneNumber(profileUpdateDto.getPhoneNumber());
        }

        if (profileUpdateDto.getAlternativeEmail() != null && !profileUpdateDto.getAlternativeEmail().equals(currentUser.getAlternativeEmail())) {
            currentUser.addUpdateHistory("alternativeEmail", currentUser.getAlternativeEmail(), profileUpdateDto.getAlternativeEmail());
            currentUser.setAlternativeEmail(profileUpdateDto.getAlternativeEmail());
        }

        if (profileUpdateDto.getThemePreference() != null && !profileUpdateDto.getThemePreference().equals(currentUser.getThemePreference())) {
            currentUser.addUpdateHistory("themePreference",
                    currentUser.getThemePreference() != null ? currentUser.getThemePreference().name() : null,
                    profileUpdateDto.getThemePreference().name());
            currentUser.setThemePreference(profileUpdateDto.getThemePreference());
        }

        // Record user activity
        currentUser.recordActivity();

        return userService.saveUser(currentUser);
    }

    @Transactional
    public User updateNotificationPreferences(NotificationPreferencesDto preferencesDto) {
        User currentUser = userService.getCurrentUser();

        if (preferencesDto.getEnabledNotifications() != null) {
            String oldPreferences = currentUser.getEnabledNotifications() != null ?
                    currentUser.getEnabledNotifications().toString() : "[]";
            String newPreferences = preferencesDto.getEnabledNotifications().toString();

            currentUser.addUpdateHistory("enabledNotifications", oldPreferences, newPreferences);
            currentUser.setEnabledNotifications(preferencesDto.getEnabledNotifications());
        }

        // Record user activity
        currentUser.recordActivity();

        return userService.saveUser(currentUser);
    }

    @Transactional
    public User uploadProfilePicture(MultipartFile file) throws IOException {
        User currentUser = userService.getCurrentUser();

        // 1. Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 2. Generate unique filename
        String filename = UUID.randomUUID().toString() + "_" +
                StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        // 3. Save the file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 4. Update user's profile picture URL
        String oldPicture = currentUser.getPicture();
        String newPicture = filename;

        // 5. Record the change
        currentUser.addUpdateHistory("picture", oldPicture, newPicture);
        currentUser.setPicture(newPicture);

        // 6. Record activity
        currentUser.recordActivity();

        return userService.saveUser(currentUser);
    }

    @Transactional(readOnly = true)
    public List<ProfileUpdateHistoryDto> getProfileUpdateHistory() {
        User currentUser = userService.getCurrentUser();

        // Record user activity
        userService.recordUserActivity(currentUser);

        List<ProfileUpdateHistory> history = profileUpdateHistoryRepository.findByUserOrderByUpdateDateDesc(currentUser);

        return history.stream()
                .map(this::convertToHistoryDto)
                .collect(Collectors.toList());
    }

    public ExtendedUserDto convertToExtendedDto(User user) {
        ExtendedUserDto dto = new ExtendedUserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPicture(user.getPicture());
        dto.setPrimaryProvider(user.getPrimaryProvider());
        dto.setRoles(user.getRoles());

        // Extended information
        dto.setBiography(user.getBiography());
        dto.setLocation(user.getLocation());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAlternativeEmail(user.getAlternativeEmail());

        // Preferences
        dto.setThemePreference(user.getThemePreference());
        dto.setEnabledNotifications(user.getEnabledNotifications());

        // Activity information
        dto.setRegistrationDate(user.getRegistrationDate());
        dto.setLastLoginDate(user.getLastLoginDate());
        dto.setLastActiveDate(user.getLastActiveDate());
        dto.setLoginCount(user.getLoginCount());
        dto.setProfileUpdateCount(user.getProfileUpdateCount());

        return dto;
    }

    private ProfileUpdateHistoryDto convertToHistoryDto(ProfileUpdateHistory history) {
        ProfileUpdateHistoryDto dto = new ProfileUpdateHistoryDto();
        dto.setId(history.getId());
        dto.setFieldName(history.getFieldName());
        dto.setOldValue(history.getOldValue());
        dto.setNewValue(history.getNewValue());
        dto.setUpdateDate(history.getUpdateDate());
        return dto;
    }
}