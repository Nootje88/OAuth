package com.template.OAuth.controller;

import com.template.OAuth.dto.UserDto;
import com.template.OAuth.entities.User;
import com.template.OAuth.enums.Role;
import com.template.OAuth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // Only accessible by users with ADMIN role
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/assign-role")
    public ResponseEntity<String> assignRole(@RequestParam String email, @RequestParam Role role) {
        userService.assignRole(email, role);
        return ResponseEntity.ok("Role " + role + " assigned to user " + email);
    }

    // Only accessible by users with ADMIN role
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/remove-role")
    public ResponseEntity<String> removeRole(@RequestParam String email, @RequestParam Role role) {
        userService.removeRole(email, role);
        return ResponseEntity.ok("Role " + role + " removed from user " + email);
    }

    // Only accessible by ADMIN or MODERATOR roles
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @GetMapping("/moderator/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        List<UserDto> userDtos = users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    // Only accessible by users with PREMIUM role
    @PreAuthorize("hasRole('PREMIUM')")
    @GetMapping("/premium/content")
    public ResponseEntity<String> getPremiumContent() {
        return ResponseEntity.ok("This is premium content available only to premium users");
    }

    // Basic endpoint accessible to all authenticated users
    @GetMapping("/user/profile")
    public ResponseEntity<UserDto> getUserProfile() {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(convertToDto(currentUser));
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPicture(user.getPicture());
        dto.setRoles(user.getRoles());
        return dto;
    }
}