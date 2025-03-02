package com.template.OAuth.mapper;

import com.template.OAuth.dto.UserDto;
import com.template.OAuth.entities.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPicture(user.getPicture());
        dto.setPrimaryProvider(user.getPrimaryProvider());
        return dto;
    }
}
