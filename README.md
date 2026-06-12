# JWT 인증 기반 게시판 REST API (Spring Boot + Spring Security)

Spring Security와 JWT로 인증/인가를 직접 구현한 게시판 API입니다.
회원가입(BCrypt) → 로그인(JWT 발급) → 커스텀 필터 토큰 검증 → 경로별 접근 제어까지
인증 전 구간을 구현했으며, 게시판 CRUD·검색·페이징·검증·전역 예외 처리를 포함합니다.

## 기술 스택

- Java 17, Spring Boot 3.5.14
- Spring Security, JWT (jjwt 0.11.5), BCrypt
- Spring Data JPA, H2 Database
- Gradle, Lombok

## 주요 기능

### 인증/인가 (Spring Security + JWT)

- 회원가입: BCrypt 비밀번호 해시 저장, 아이디 중복 검증
- 로그인: 검증 성공 시 JWT 발급 (HS256, 유효시간 1시간)
- 커스텀 인증 필터(`OncePerRequestFilter`): 매 요청 토큰 검증 → DB 회원 대조 → `SecurityContext`에 인증 등록
- 경로별 접근 제어: 조회(GET)는 공개, 글 작성/수정/삭제는 인증 필요 — 필터는 인증만 담당하고 접근 판단은 `SecurityFilterChain` 규칙에 위임하는 구조
- 인증 흐름:

```
[로그인]     ID/PW → BCrypt matches → JWT 발급
[이후 요청]  Authorization 헤더 → 필터: 서명/만료 검증 → SecurityContext 인증 등록 → 인가 규칙 통과 → 컨트롤러
```

### 게시판

- 게시글 CRUD
- 제목 검색 (JPA 쿼리 메서드 `findByTitleContaining`)
- 페이징 및 최신순 정렬 (`Pageable`, `Page` 메타 정보 응답)
- 입력값 검증 (`@Valid`, `@NotBlank`) — 필드별 에러 메시지 응답
- 전역 예외 처리 (`@RestControllerAdvice`) — 일관된 에러 응답 포맷

## API 명세

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | /user/signup | X | 회원가입 |
| POST | /user/login | X | 로그인 (JWT 발급) |
| GET | /post/list?page=0&size=10 | X | 목록 조회 (페이징) |
| GET | /post/search?keyword=검색어 | X | 제목 검색 |
| GET | /post/{postid} | X | 단건 조회 |
| POST | /post/newpost | **O** | 게시글 등록 |
| PUT | /post/{postid} | **O** | 수정 |
| DELETE | /post/{postid} | **O** | 삭제 |

인증이 필요한 요청은 로그인 응답으로 받은 토큰을 헤더에 포함합니다.

```
Authorization: Bearer {token}
```

## 테스트 (Thunder Client)

인증 플로우 전 구간을 수동 테스트로 검증했습니다.

| 시나리오 | 기대 결과 | 결과 |
|---|---|---|
| 회원가입 | 가입 성공, DB에 BCrypt 해시 저장 | ✅ |
| 중복 아이디 가입 | 거부 + "이미 존재하는 아이디" | ✅ |
| 비로그인 글 목록 조회 (GET) | 200 허용 — 읽기 공개 정책 | ✅ |
| 비로그인 글 작성 (POST) | 403 거부 — 쓰기 잠금 | ✅ |
| 로그인 | JWT 토큰 발급 | ✅ |
| 토큰 포함 글 작성 | 등록 성공 — 인증 전 구간 연결 | ✅ |
| 잘못된 비밀번호 로그인 | 거부 | ✅ |
| 변조된 토큰으로 요청 | 401 거부 (서명 검증 실패) | ✅ |

## 트러블슈팅 기록

- **컨트롤러 에러가 403으로 보이던 문제**: `/error` 경로가 시큐리티에 잠겨 있어 400 에러가 403으로 변장되어 응답됨 → `/error` permitAll로 해결, Security DEBUG 로그로 원인 추적
- **필터와 인가 규칙의 역할 분리**: 토큰이 없는 요청을 필터에서 즉시 거부하면 공개 경로(permitAll)까지 차단됨 → 토큰 부재 시 인증 없이 통과시키고 접근 판단은 `SecurityFilterChain` 규칙에 위임하도록 수정
- **Bearer 접두어 공백 누락**: `"Bearer"`(6글자) 기준 substring으로 토큰 앞에 공백이 남아 서명 검증 실패 → 공백 포함 7글자로 통일
- **토큰 변조 테스트가 통과한 사례**: 서명 마지막 글자 변경 시 Base64 인코딩의 버려지는 패딩 비트 구간이라 동일한 서명으로 디코딩됨 → 유효 비트 위치 변조로 재검증하여 거부 확인

## 실행 방법

```bash
./gradlew bootRun
```

`src/main/resources/application.properties`에 JWT 비밀키 설정이 필요합니다.

```properties
jwt.secret.key={Base64 인코딩된 32바이트 이상 키}
```