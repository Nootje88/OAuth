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

    @Column(nullable = false)  // ✅ Name should not be null
    private String name;

    @Column(nullable = false, unique = true)  // ✅ Email must be unique
    private String email;

    private String picture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)  // ✅ Every user should have a primary provider
    private AuthProvider primaryProvider;

    // ✅ Nullable since a user might not connect multiple services
    @Column(unique = true)
    private String googleId;

    @Column(unique = true)
    private String spotifyId;

     @Column(unique = true)  // Uncomment if using Apple ID
     private String appleId;

    @Column(unique = true)
    private String soundcloudId;
}
