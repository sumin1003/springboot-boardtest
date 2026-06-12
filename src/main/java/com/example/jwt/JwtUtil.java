package com.example.jwt;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*필터가 뭐예요? 프론트에서 요청 올 때마다 → "우리 쪽 사람 맞아?" → 토큰 확인 → 맞으면 통과, 아니면 401로 돌려보냄. 끝.
이제 그 검문소를 실제 코드로 만들면 되는데, 검문소가 할 일도 네가 이미 다 알아:

요청 헤더에서 토큰 꺼내고 (substringToken)
유효한지 확인하고 (validateToken)
누군지 알아내서 (getUserInfoFromToken)
"이 요청은 ○○씨 요청임"이라고 도장 찍고 통과

올바른 순서는:
substringToken — "Bearer " 떼고 순수 토큰 확보
validateToken — 진짜인지부터 확인 (위조/만료/형식)
getUserInfoFromToken — 통과한 토큰에서만 정보 추출
"검증 → 신뢰 → 사용" 순서는 보안 코드의 기본 원칙이야. */


@Slf4j              // Lombok: log.info() 등을 쓸 수 있는 Logger 객체(log)를 자동 생성
@Component          // 이 클래스를 Spring 빈으로 등록 → 다른 곳에서 주입받아 사용 가능
@RequiredArgsConstructor // Lombok: final 필드 생성자 자동 생성 (지금은 사실상 미사용)
public class JwtUtil {

    // HTTP 요청 헤더에서 토큰을 꺼낼 때 쓸 헤더 이름
    public static final String AUTHORIZATION_HEADER = "authorization";

    // 토큰 안에 권한(role)을 담을 때 쓸 클레임 키 이름 (현재 미사용, 권한 기능 추가 시 사용)
    public static final String AUTHORIZATION_KEY = "auth";

    // 토큰 앞에 붙는 HTTP 표준 접두어. 공백 포함 7글자 — substring(7)과 짝
    public static final String BEARER_PREFIX = "Bearer ";

    // 토큰 유효 시간 1시간 (ms 단위)
    private final long TOKEN_TIME = 60 * 60 * 1000;

    // 서명 알고리즘 HS256 (대칭키 방식: 같은 키로 서명+검증)
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    // application.properties의 값을 읽어 주입
    @Value("${jwt.secret.key}")
    private String secretKey;

    // 실제 서명/검증에 쓸 Key 객체
    private Key key;

    // 빈 생성 + 주입 완료 직후 1회 자동 실행 (생성자 시점엔 @Value가 아직 null이라 여기서 함)
    @PostConstruct
    public void init() {
        log.info(secretKey); // ⚠ 비밀키 로그 — 확인 후 삭제할 것

        // Base64.getDecoder()
        //   → Base64 해독기 객체를 얻는 메서드.
        //     Base64란: 바이너리(바이트) 데이터를 알파벳/숫자로만 된 문자열로 표현하는 인코딩 방식.
        //     비밀키는 원래 바이트 덩어리인데 properties 파일엔 문자열만 적을 수 있어서 Base64로 적어둔 것.
        // .decode(secretKey)
        //   → Base64 문자열을 원래의 바이트 배열(byte[])로 되돌리는 메서드.
        byte[] bytes = Base64.getDecoder().decode(secretKey);

        // Keys.hmacShaKeyFor(bytes)
        //   → jjwt가 제공하는 메서드. 바이트 배열을 받아서 HMAC-SHA 알고리즘용 Key 객체로 만들어줌.
        //     이때 키 길이가 충분한지(HS256은 최소 256비트=32바이트)도 검사해서, 짧으면 예외를 던짐.
        key = Keys.hmacShaKeyFor(bytes);
    }

    // [토큰 발급] 로그인 성공 시 호출
    public String createToken(String userId) {
        // new Date() → 현재 날짜+시각을 담은 객체 생성
        Date date = new Date();

        return BEARER_PREFIX +
                // Jwts.builder()
                //   → JWT를 조립하는 빌더 객체를 생성. 이후 .setXxx()를 체인으로 이어붙여 내용물을 채움
                Jwts.builder()

                    // .setSubject(userId)
                    //   → 토큰의 "sub"(subject) 클레임을 설정.
                    //     sub = 이 토큰의 주인이 누구인지를 나타내는 표준 항목.
                    //     나중에 getSubject()로 꺼내서 "이 요청은 누구의 요청인가"를 알아냄.
                    .setSubject(userId)

                    // .setExpiration(Date)
                    //   → 토큰의 "exp"(expiration) 클레임을 설정. 이 시각이 지나면 토큰은 무효.
                    //     date.getTime() = 현재 시각을 ms 숫자로 변환 (1970년 기준 경과 ms)
                    //     + TOKEN_TIME = 1시간 뒤 ms → 그걸 다시 new Date(...)로 Date 객체화
                    //     검증 시 parseClaimsJws가 이 값을 보고 만료면 ExpiredJwtException을 던짐.
                    .setExpiration(new Date(date.getTime() + TOKEN_TIME))

                    // .setIssuedAt(date)
                    //   → 토큰의 "iat"(issued at) 클레임 설정 = 발급 시각 기록.
                    //     필수는 아니지만 "언제 발급된 토큰인지" 추적/디버깅에 유용.
                    .setIssuedAt(date)

                    // .signWith(key, 알고리즘)
                    //   → 위에서 채운 내용(헤더+페이로드)을 비밀키로 서명(signature) 생성.
                    //     서명 = 내용물을 키로 계산한 도장. 내용이 1글자라도 바뀌면 서명이 안 맞아서
                    //     검증 단계에서 위조가 들통남. JWT 보안의 핵심.
                    .signWith(key, signatureAlgorithm)

                    // .compact()
                    //   → 지금까지 조립한 것을 최종 문자열로 압축해서 반환.
                    //     결과 형태: "xxxxx.yyyyy.zzzzz" (헤더.페이로드.서명 — 각각 Base64 인코딩됨)
                    .compact();
    }

    // [접두어 제거] "Bearer eyJ..." → "eyJ..."
    public String substringToken(String tokenValue) {
        // StringUtils.hasText(문자열)
        //   → Spring 제공 유틸. null 아님 + 길이 0 아님 + 공백만도 아님, 세 검사를 한 번에.
        //     (tokenValue != null && !tokenValue.trim().isEmpty() 를 한 단어로 줄인 것)
        // tokenValue.startsWith(BEARER_PREFIX)
        //   → 자바 String 기본 메서드. 문자열이 "Bearer "로 시작하는지 true/false.
        // && 단축 평가: 왼쪽이 false면 오른쪽 실행 안 함 → null일 때 startsWith 호출을 막아 NPE 방지
        if (StringUtils.hasText(tokenValue) &&
                tokenValue.startsWith(BEARER_PREFIX)) {

            // tokenValue.substring(7)
            //   → 자바 String 기본 메서드. 인덱스 7부터 끝까지 잘라서 반환.
            //     0~6번(7글자) = "Bearer "를 버리고 순수 토큰만 남기는 것.
            return tokenValue.substring(7);
        }
        // 토큰이 없거나 형식이 다르면 예외 → 필터 쪽에서 인증 실패 처리
        throw new NullPointerException("Not Found Token");
    }

    // [토큰 검증] 매 요청마다 필터가 호출하게 될 메서드
    public Boolean validateToken(String token) {
        try {
            // Jwts.parserBuilder()
            //   → 토큰을 "읽고 검증하는" 파서를 만드는 빌더 생성. (builder가 만들기용이면 이건 해석용)
            // .setSigningKey(key)
            //   → 서명 검증에 사용할 우리 비밀키를 파서에 등록.
            //     이 키로 다시 계산한 서명과 토큰에 붙은 서명을 비교하게 됨.
            // .build()
            //   → 설정이 끝난 파서를 실제로 생성.
            // .parseClaimsJws(token)
            //   → 진짜 일을 하는 메서드. 토큰을 분해해서
            //     ① 형식이 올바른가 ② 서명이 우리 키로 만든 게 맞는가 ③ 만료 안 됐는가
            //     를 전부 검사하고, 하나라도 실패하면 해당 예외를 던짐. 예외 없이 지나가면 유효한 토큰.
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            // SecurityException: 서명 불일치 = 위조 의심 / MalformedJwtException: JWT 구조 깨짐
            // 멀티캐치(|): 원인은 달라도 결론("거부")이 같아서 한 블록으로
            log.info("유효하지 않은 JWT 서명입니다." + e);
        } catch (ExpiredJwtException e) {
            // exp 시각 경과 → 재로그인 필요
            log.info("만료된 JWT 토큰입니다." + e);
        } catch (IllegalArgumentException e) {
            // token이 null/빈 문자열 등 파싱 시도조차 불가한 입력
            log.info("잘못된 JWT 토큰입니다." + e);
        }
        return false;
    }

    // [정보 추출] 토큰에서 내용물 꺼내기
    public Claims getUserInfoFromToken(String token) {
        // 앞 3개(parserBuilder → setSigningKey → build → parseClaimsJws)는 validateToken과 동일
        // .getBody()
        //   → 파싱 결과에서 페이로드 부분(Claims 객체)만 꺼냄.
        //     Claims = 토큰에 담긴 정보 묶음. claims.getSubject()로 setSubject에 넣었던 userId를 회수.
        //     즉 createToken에서 넣은 것을 여기서 도로 꺼내는, 입출구 관계.
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}


/*`createToken`은 방금 그 3단 흐름이랑 **다른 시점**에 일하는 메서드야. JWT는 인생이 두 장면으로 나뉘거든:

**장면 1 — 로그인 (1회):**
사용자가 ID/비밀번호 보냄 → 서버가 확인 → 맞으면 `createToken(userId)`으로 **토큰을 만들어서 발급** → 클라이언트가 받아서 보관

**장면 2 — 이후 모든 요청 (매번):**
클라이언트가 보관해둔 토큰을 헤더에 실어 보냄 → 필터가 `substringToken → validateToken → getUserInfoFromToken`으로 **받은 토큰을 확인**

그러니까 createToken은 **발급 창구**(로그인 API에서 호출)고, 나머지 3개는 **검문소**(필터에서 호출)야. 출입증을 만들어주는 곳과 출입증을 검사하는 곳이 다른 거지.

JwtUtil 네 개 메서드 역할 분담 정리:

| 메서드 | 일하는 곳 | 시점 |
|---|---|---|
| createToken | 로그인 API | 로그인 성공 시 1회 |
| substringToken | 필터 | 매 요청 |
| validateToken | 필터 | 매 요청 |
| getUserInfoFromToken | 필터 | 매 요청 |

이제 전체 그림 보이지? 다음 작업도 자연스럽게 둘로 나뉘어 — 로그인 API(창구) 만들기랑 필터(검문소) 만들기. */
/*그건 방금 표로 정리한 **창구(발급)와 검문소(검증)가 내부적으로 어떻게 짝이 맞는지**에 대한 설명이야. createToken 안의 코드와 validateToken/getUserInfoFromToken 안의 코드가 1:1로 대응된다는 거지:

```
[발급 쪽 — createToken]              [검증 쪽 — validate/getUserInfo]

Jwts.builder()          ←거울→      Jwts.parserBuilder()
  (토큰 만들기 시작)                    (토큰 해석하기 시작)

.setSubject(userId)     ←거울→      .getBody().getSubject()
  (userId를 넣음)                      (그 userId를 도로 꺼냄)

.signWith(key, ...)     ←거울→      .setSigningKey(key) + parseClaimsJws()
  (비밀키로 도장 찍음)                  (같은 키로 도장이 진짜인지 대조)
```

즉 한쪽에서 한 일을 반대쪽에서 정확히 역으로 하는 구조라는 거야. **넣은 사람이 있으면 꺼내는 사람이 있고, 도장 찍는 쪽이 있으면 대조하는 쪽이 있다** — 그래서 "거울"이라고 표현한 거고.

이게 실무에서 중요한 이유: 양쪽이 **같은 key**를 써야만 대조가 성립해. 만약 서버가 재시작하면서 키가 바뀌면? 이전에 발급한 토큰들이 전부 "유효하지 않은 서명"으로 거부돼. properties에 키를 고정해두는 이유 중 하나가 이거야.

정리하면 — 아까 네가 이걸 "메서드 호출 순서" 질문의 답으로 냈었잖아. 이건 순서가 아니라 **대응 관계**고, 호출 순서는 substring → validate → getUserInfo. 두 개념이 이제 구분돼? */