package com.template.OAuth.dto;

import com.template.OAuth.enums.AuthProvider;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    private String name;
    private String email;
    private String picture;
    private AuthProvider primaryProvider;
}
