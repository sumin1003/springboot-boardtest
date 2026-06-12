package com.example.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.DTO.LoginDto;
import com.example.DTO.LoginResponseDto;
import com.example.DTO.SignupDto;
import com.example.entity.UserSecurity;
import com.example.jwt.JwtUtil;
import com.example.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void createUser(SignupDto dto) {
        //isPresent()는 Optional 상자가 비었는지 들었는지를 알려주는 메서드야 — 들어있으면 true, 비었으면 false.
        //isPresent가 true(이미 주인 있음)면 예외로 끊고, false(빈 아이디)면 if를 그냥 지나쳐서 아래 encode → save로 흘러가는 거야.
        if(userRepository.findByUserId(dto.getUserId()).isPresent()){
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        UserSecurity us = new UserSecurity(dto.getUserId(), passwordEncoder.encode(dto.getPassword()), dto.getUsername());
        userRepository.save(us);
    }

    @Override
    public LoginResponseDto userLogin(LoginDto dto) {
        // if(userRepository.findByUserId(dto.getUserId()).isEmpty()){
        //     throw new IllegalArgumentException("아이디가 존재하지 않습니다.");
        // }
        UserSecurity us = userRepository.findByUserId(dto.getUserId())
                                    .orElseThrow(()-> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        if(!passwordEncoder.matches(dto.getPassword(),us.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 틀립니다.");
        }
        String token = jwtUtil.createToken(us.getUserId());
        log.info("로그인 성공 : {}", dto.getUserId());
        return new LoginResponseDto(token);
    }
}
