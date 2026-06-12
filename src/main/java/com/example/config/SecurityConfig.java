package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.jwt.JwtAuthorizationFilter;
import com.example.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

@Configuration       // "설정 클래스"라는 표시 — 안의 @Bean 메서드들을 스프링이 읽어 빈으로 등록
@EnableWebSecurity   // 스프링 시큐리티 활성화 + 아래 SecurityFilterChain 설정을 적용하겠다는 선언
@RequiredArgsConstructor // final 필드 2개를 받는 생성자 자동 생성 → 스프링이 주입
public class SecurityConfig {

    // JwtAuthorizationFilter에 넘겨줄 부품 2개.
    // 필터를 아래에서 new로 직접 만들기 때문에, 필터가 쓸 재료를 Config가 대신 주입받아 전달
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @Bean // 시큐리티 본체 설정 — 이 반환값(SecurityFilterChain)이 곧 보안 규칙 전체
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // http = 보안 설정을 조립하는 빌더. 체인으로 규칙을 쌓는다

        // ── URL별 출입 규칙 구역 ──────────────────────────────
        // 규칙은 위에서부터 순서대로 매칭됨 → 구체적 허용을 위에, 포괄적 잠금을 맨 아래에
        http.authorizeHttpRequests(auth -> auth

            // 회원가입/로그인: 누구나 허용 (토큰이 없는 게 당연한 경로 — 면제 원칙)
            .requestMatchers("/user/signup", "/user/login").permitAll()

            // 게시판의 GET(목록/검색/단건조회)만 공개 — "읽기는 공개" 정책
            // 첫 인자로 HttpMethod를 주면 같은 경로라도 메서드별로 규칙 분리 가능
            // ** = 하위 경로 전부
            .requestMatchers(HttpMethod.GET, "/post/**").permitAll()

            // /admin 경로는 ADMIN 권한 보유자만 (현재 admin 기능 없으므로 예비 규칙)
            .requestMatchers("/admin").hasRole("ADMIN")

            .requestMatchers("/error").permitAll() //error 개방 (변장 방지)
            
            // 위에서 언급 안 된 나머지 전부 = 인증(유효한 토큰) 필수
            // → 글쓰기(POST)/수정(PUT)/삭제(DELETE)가 자동으로 여기 걸려 잠김
            .anyRequest().authenticated()
        )

        // CSRF 방어 OFF — 세션 쿠키의 자동 전송을 악용하는 공격인데,
        // 우리는 토큰을 헤더에 직접 실어 보내는 방식이라 공격이 성립 안 함 (JWT API 표준 설정)
        .csrf(csrf -> csrf.disable())

        // 폼 로그인(스프링 기본 로그인 HTML 페이지) OFF —
        // 로그인은 우리가 만들 /user/login API가 처리하므로 불필요
        .formLogin(from -> from.disable())

        // 시큐리티 기본 필터 줄에서 UsernamePasswordAuthenticationFilter(폼 로그인 처리 필터)
        // **앞자리**에 내 JWT 필터를 끼워 넣음 = "기본 인증 검사 전에 토큰 검사부터 해라"
        .addFilterBefore(new JwtAuthorizationFilter(userDetailsService, jwtUtil),
                         UsernamePasswordAuthenticationFilter.class);

        // 쌓은 설정을 완성품으로 빌드 → 빈으로 등록되어 적용
        return http.build();
    }

    @Bean // 비밀번호 인코더 등록 — 회원가입(encode)/로그인(matches)에서 주입받아 사용
    public PasswordEncoder passwordEncoder() {
        // BCrypt: 솔트 내장 + 의도적으로 느린 비밀번호 전용 해시
        // 반환 타입을 인터페이스로 둔 이유: 구현체 교체에 유연 (Map = new HashMap 그 패턴)
        return new BCryptPasswordEncoder();
    }
}