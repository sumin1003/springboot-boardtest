package com.example.cotroller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.DTO.LoginDto;
import com.example.DTO.LoginResponseDto;
import com.example.DTO.SignupDto;
import com.example.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    @PostMapping("/signup")
    public String createUser(@RequestBody SignupDto dto) {
        userService.createUser(dto);
        return "가입 성공";
    }

    @PostMapping("/login")
    public LoginResponseDto userLogin(@RequestBody LoginDto dto) {
        return userService.userLogin(dto);
    }
}
