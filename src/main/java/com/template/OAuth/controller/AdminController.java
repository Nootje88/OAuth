package com.template.OAuth.controller;

import com.template.OAuth.dto.UserDto;
import com.template.OAuth.entities.User;
import com.template.OAuth.enums.Role;
import com.template.OAuth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@Tag(name = "Admin", description = "Admin management endpoints")
@SecurityRequirement(name = "cookie") // Require authentication for all admin endpoints
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Assign role to user",
            description = "Assigns a specific role to a user (Admin access required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role assigned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/assign-role")
    public ResponseEntity<String> assignRole(
            @Parameter(description = "User email", required = true)
            @RequestParam String email,
            @Parameter(description = "Role to assign", required = true,
                    schema = @Schema(implementation = Role.class))
            @RequestParam Role role) {
        userService.assignRole(email, role);
        return ResponseEntity.ok("Role " + role + " assigned to user " + email);
    }

    @Operation(summary = "Remove role from user",
            description = "Removes a specific role from a user (Admin access required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/remove-role")
    public ResponseEntity<String> removeRole(
            @Parameter(description = "User email", required = true)
            @RequestParam String email,
            @Parameter(description = "Role to remove", required = true,
                    schema = @Schema(implementation = Role.class))
            @RequestParam Role role) {
        userService.removeRole(email, role);
        return ResponseEntity.ok("Role " + role + " removed from user " + email);
    }

    @Operation(summary = "Get all users",
            description = "Retrieves all users in the system (Admin or Moderator access required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized",
                    content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @GetMapping("/moderator/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        List<UserDto> userDtos = users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    @Operation(summary = "Get premium content",
            description = "Retrieves premium content (Premium or Admin access required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Premium content retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized",
                    content = @Content)
    })
    @PreAuthorize("hasRole('PREMIUM')")
    @GetMapping("/premium/content")
    public ResponseEntity<String> getPremiumContent() {
        return ResponseEntity.ok("This is premium content available only to premium users");
    }

    @Operation(summary = "Get user profile",
            description = "Retrieves the profile of the currently authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Not authenticated",
                    content = @Content)
    })
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