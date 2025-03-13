package com.template.OAuth.controller;

import com.template.OAuth.dto.UserDto;
import com.template.OAuth.entities.User;
import com.template.OAuth.mapper.UserMapper;
import com.template.OAuth.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/profile")
    public UserDto getUser() {
        User currentUser = userService.getCurrentUser();
        return userMapper.toDto(currentUser);
    }

    @GetMapping("/{email}")
    public UserDto getUserByEmail(@PathVariable String email) {
        return userMapper.toDto(userService.findUserByEmail(email));
    }
}