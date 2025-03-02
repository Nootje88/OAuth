package com.template.OAuth.controller;

import com.template.OAuth.dto.UserDto;
import com.template.OAuth.mapper.UserMapper;
import com.template.OAuth.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/{email}")
    public UserDto getUser(@PathVariable String email) {
        return userMapper.toDto(userService.findUserByEmail(email));
    }
}
