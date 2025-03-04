package com.template.OAuth.repositories;

import com.template.OAuth.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // ✅ Find users by their OAuth provider ID
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findBySpotifyId(String spotifyId);
    Optional<User> findBySoundcloudId(String soundcloudId);
}
