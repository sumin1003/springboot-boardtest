package com.example.service;

import com.example.DTO.LoginDto;
import com.example.DTO.LoginResponseDto;
import com.example.DTO.SignupDto;

public interface UserService {
    void createUser(SignupDto dto);
    LoginResponseDto userLogin(LoginDto dto);
}
