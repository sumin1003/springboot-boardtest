package com.example.jwt;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService; 
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                                    throws ServletException, IOException {
        
        List<String> list = Arrays.asList(
            "/user/login"
            ,"/user/signup"
            ,"/login"
            ,"/css/**"
            ,"/js/**"
            ,"/images/**"
        );

        if(list.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response); //"요청을 다음 단계로 통과시켜라" (다음 필터 → 최종적으로 컨트롤러). 즉 검문 없이 들여보내는 것
            return; //이 필터의 나머지 코드(아래에 있을 토큰 검사)는 실행하지 마라
        }

        /*request.getMethod() — 이 요청의 HTTP 메서드를 반환: "GET", "POST", "OPTIONS" 등
        .equalsIgnoreCase("OPTIONS") — 그 값이 대소문자 무시하고 "OPTIONS"와 정확히 같은지 true/false */
        if(request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response); //이 요청의 메서드가 OPTIONS면 → 검사 없이 통과시켜라
            return;
        }

        String token = request.getHeader("Authorization");
        if(token != null && token.startsWith("Bearer")) { //토큰이 null이 아니고, Bearer로 시작하면". 둘 다 만족해야 통과
            token = token.substring(6); //앞 6글자를 버리고 나머지 전부
        }

        try {
            // token이 null도 아니고 빈 문자열도 아니면 (= 검사할 토큰이 일단 존재하면)
            if (token != null && !token.equalsIgnoreCase("")) {

                // jwtUtil.validateToken(token)
                //   → 어제 만든 그 메서드. 서명 진짜인지 + 만료 안 됐는지 + 형식 멀쩡한지 검사 → true/false
                if (jwtUtil.validateToken(token)) {

                    // jwtUtil.getUserInfoFromToken(token)
                    //   → 검증 통과한 토큰에서 내용물(Claims) 추출
                    Claims claims = jwtUtil.getUserInfoFromToken(token);

                    // claims.getSubject()
                    //   → createToken에서 setSubject(userId)로 넣었던 그 값을 도로 꺼냄
                    //     = "이 토큰의 주인이 누구인지"를 이제 알아냄
                    String loginId = claims.getSubject();
                    log.info("loginId check" + loginId);

                    // 꺼낸 loginId가 비어있지 않으면
                    if (loginId != null && !loginId.equalsIgnoreCase("")) {

                        // userDetailsService.loadUserByUsername(loginId)
                        //   → DB에서 이 loginId의 회원을 실제로 조회 (다음에 만들 UserDetailsServiceImpl이 할 일)
                        //     토큰은 유효해도 그 사이 탈퇴했을 수 있으니 "회원 명부 대조"를 하는 것.
                        //     결과는 UserDetails = 스프링 시큐리티가 이해하는 표준 회원 정보 객체
                        UserDetails userDetails = userDetailsService.loadUserByUsername(loginId);

                        // new UsernamePasswordAuthenticationToken(회원정보, null, 권한목록)
                        //   → 어제 말한 그 "도장"을 만드는 줄. "이 요청은 인증된 ○○씨다"라는 증명서 객체.
                        //     두 번째 인자 null = 비밀번호 자리인데, 이미 토큰으로 인증됐으니 필요 없음
                        //     getAuthorities() = 이 회원의 권한 목록 (ROLE_USER 등)
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        // SecurityContextHolder.getContext().setAuthentication(authentication)
                        //   → 만든 도장을 SecurityContext(이 요청 동안 유지되는 "인증 보관소")에 찍음.
                        //     이 줄이 실행된 순간부터 스프링 전체가 이 요청을 "로그인된 요청"으로 취급.
                        //     나중에 컨트롤러에서 "지금 누구야?"라고 물으면 여기서 꺼내주는 것.
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // 검문 끝, 통과 — 다음 필터/컨트롤러로 넘김
                        filterChain.doFilter(request, response);
                    } else {
                        throw new RuntimeException("유저를 찾을 수 없습니다.");
                    }
                } else {
                    throw new RuntimeException("TOKEN 유효하지 않습니다.");
                }
            } else {
                filterChain.doFilter(request, response);
                return;
            }
        } catch (Exception e) {
            // 위에서 던진 어떤 실패든 여기로 모임 → 직접 401 응답을 만들어서 내보냄
            // (필터는 컨트롤러 바깥이라 @RestControllerAdvice가 못 잡아 → 응답을 수동 제작해야 함)
            log.info(e.getMessage());

            // 상태코드 401 (Unauthorized = 인증 실패)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // 한글 깨짐 방지
            response.setCharacterEncoding("UTF-8");
            // ⚠ 버그: getContentType → setContentType("application/json")
            //   "지금 보내는 바디는 JSON이다"라고 클라이언트에 알리는 헤더
            response.setContentType("application/json");

            // response.getWriter() → 응답 바디에 글자를 직접 쓸 수 있는 펜을 얻음
            PrintWriter print = response.getWriter();
            // 에러 JSON을 문자열로 직접 작성해서 씀
            print.write("{\"error\": true, \"message\": \"로그인 에러\"}");
            print.flush();  // 버퍼에 남은 내용 즉시 전송
            print.close();  // 펜 반납
        }
    }
}