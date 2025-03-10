package com.template.OAuth.dto;

import com.template.OAuth.enums.AuthProvider;
import com.template.OAuth.enums.Role;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String picture;
    private AuthProvider primaryProvider;
    private Set<Role> roles;
}