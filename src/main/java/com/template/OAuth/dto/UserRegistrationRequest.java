package com.template.OAuth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistrationRequest {
    private String email;
    private String name;
    private String password;
    private String picture;
}