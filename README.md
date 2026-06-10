# Spring Boot 게시판 REST API

Spring Boot + JPA 학습용 게시판 API입니다.
CRUD를 기본으로 검색, 페이징, 입력값 검증, 전역 예외 처리까지 단계적으로 구현했습니다.

## 기술 스택

- Java 17, Spring Boot 3.x
- Spring Data JPA, H2 Database
- Gradle, Lombok

## 주요 기능

- 게시글 CRUD
- 제목 검색 (JPA 쿼리 메서드 `findByTitleContaining`)
- 페이징 및 최신순 정렬 (`Pageable`, `Page` 메타 정보 응답)
- 입력값 검증 (`@Valid`, `@NotBlank`) — 필드별 에러 메시지 응답
- 전역 예외 처리 (`@RestControllerAdvice`) — 일관된 에러 응답 포맷 (400/404)

## API 명세

| Method | URL | 설명 |
|--------|-----|------|
| POST | /post/newpost | 게시글 등록 |
| GET | /post/list?page=0&size=10 | 목록 조회 (페이징) |
| GET | /post/search?keyword=검색어&page=0&size=10 | 제목 검색 |
| GET | /post/{postid} | 단건 조회 |
| PUT | /post/{postid} | 수정 |
| DELETE | /post/{postid} | 삭제 |

## 실행 방법

​```bash
./gradlew bootRun
​```