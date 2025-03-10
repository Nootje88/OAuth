package com.template.OAuth.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.template.OAuth.enums.AuthProvider;
import com.template.OAuth.enums.Role;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String picture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider primaryProvider;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    // Existing fields for OAuth provider IDs
    @Column(unique = true)
    private String googleId;

    @Column(unique = true)
    private String spotifyId;

    @Column(unique = true)
    private String appleId;

    @Column(unique = true)
    private String soundcloudId;

    // Helper methods for role management
    public void addRole(Role role) {
        roles.add(role);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
}