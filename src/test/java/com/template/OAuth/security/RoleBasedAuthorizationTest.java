package com.template.OAuth.security;

import com.template.OAuth.entities.User;
import com.template.OAuth.enums.AuthProvider;
import com.template.OAuth.enums.Role;
import com.template.OAuth.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RoleBasedAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean up
        userRepository.deleteAll();

        // Create users with different roles
        createUserWithRole("admin@example.com", "Admin Name", AuthProvider.LOCAL, Role.ADMIN);
        createUserWithRole("user@example.com", "User Name", AuthProvider.LOCAL, Role.USER);
        createUserWithRole("moderator@example.com", "Moderator Name", AuthProvider.LOCAL, Role.MODERATOR);
        createUserWithRole("premium@example.com", "Premium Name", AuthProvider.LOCAL, Role.PREMIUM);
    }

    private User createUserWithRole(String email, String name, AuthProvider provider, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setPrimaryProvider(provider);
        user.setEnabled(true);

        // Add the role
        HashSet<Role> roles = new HashSet<>();
        roles.add(Role.USER); // Base role
        if (role != Role.USER) {
            roles.add(role);  // Specific role
        }
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUserAccess() throws Exception {
        // Users can access their profile
        mockMvc.perform(get("/api/user/profile")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // Users cannot access admin endpoints
        mockMvc.perform(get("/api/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden());

        // Users cannot access moderator endpoints
        mockMvc.perform(get("/api/moderator/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden());

        // Users cannot access premium content
        mockMvc.perform(get("/api/premium/content")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"USER", "ADMIN"})
    void testAdminAccess() throws Exception {
        // Admins can access user endpoints
        mockMvc.perform(get("/api/user/profile")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // Admins can access admin endpoints
        mockMvc.perform(get("/api/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // Admins can access moderator endpoints
        mockMvc.perform(get("/api/moderator/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "moderator@example.com", roles = {"USER", "MODERATOR"})
    void testModeratorAccess() throws Exception {
        // Moderators can access user endpoints
        mockMvc.perform(get("/api/user/profile")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // Moderators can access moderator endpoints
        mockMvc.perform(get("/api/moderator/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // Moderators cannot access admin endpoints
        mockMvc.perform(get("/api/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "premium@example.com", roles = {"USER", "PREMIUM"})
    void testPremiumAccess() throws Exception {
        // Premium users can access user endpoints
        mockMvc.perform(get("/api/user/profile")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // Premium users can access premium content
        mockMvc.perform(get("/api/premium/content")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // Premium users cannot access admin endpoints
        mockMvc.perform(get("/api/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedAccess() throws Exception {
        // Unauthenticated users cannot access protected endpoints
        mockMvc.perform(get("/api/user/profile")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized());

        // Unauthenticated users can access public endpoints
        mockMvc.perform(get("/auth/login-url")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }
}
