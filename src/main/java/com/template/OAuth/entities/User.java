package com.template.OAuth.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.template.OAuth.enums.AuthProvider;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String picture;

    @Enumerated(EnumType.STRING)
    private AuthProvider primaryProvider;

    // OAuth Provider IDs
    private String googleId;
    private String spotifyId;
    private String appleId;
    private String soundcloudId;
}
