package com.template.OAuth.repositories;

import com.template.OAuth.entities.ProfileUpdateHistory;
import com.template.OAuth.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileUpdateHistoryRepository extends JpaRepository<ProfileUpdateHistory, Long> {
    List<ProfileUpdateHistory> findByUserOrderByUpdateDateDesc(User user);
}