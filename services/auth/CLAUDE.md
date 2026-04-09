# Auth Service

## 개요
JWT 기반 사용자 인증/인가 마이크로서비스. 회원가입, 로그인, 토큰 발급/검증, 역할 관리를 담당한다.

## 기술 스택
- Spring Boot 3.2.5 / Java 17 / Gradle
- SQLite (파일 DB: `/data/auth.db`)
- Redis (리프레시 토큰 저장)
- JJWT 0.12.5 (JWT 토큰)
- BCrypt (비밀번호 해싱)
- SpringDoc OpenAPI 2.4.0

## 포트
- 8081

## API 엔드포인트

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/auth/register` | 회원가입 (USER 역할 부여) | 불필요 |
| POST | `/api/auth/login` | 로그인 → access/refresh 토큰 반환 | 불필요 |
| POST | `/api/auth/refresh` | 리프레시 토큰으로 새 액세스 토큰 발급 | 불필요 |
| POST | `/api/auth/verify` | 토큰 검증 → userId, role 반환 | 불필요 |
| POST | `/api/auth/logout` | 리프레시 토큰 무효화 | Bearer 토큰 |
| GET | `/api/auth/profile` | 프로필 조회 (id, email, nickname, role) | X-User-Id |
| PATCH | `/api/auth/profile/nickname` | 닉네임 변경 | X-User-Id |
| POST | `/api/auth/nicknames` | 내부 API: userId 리스트 → 닉네임 맵 반환 | 불필요 |
| POST | `/api/auth/promote/{userId}` | 사용자를 ADMIN으로 승격 | ADMIN 전용 |

## 데이터 모델

### users 테이블
- `id` (INTEGER PK), `email` (UNIQUE), `password` (BCrypt), `nickname`, `role` (USER/ADMIN), `createdAt`

## 토큰 정책
- Access Token: 15분 유효, HMAC-SHA256 서명, userId/email/role 포함
- Refresh Token: 7일 유효, Redis에 `refresh:{userId}` 키로 저장

## 초기 데이터
- admin 계정 시드 (email: "admin", nickname: "관리자")

## 환경변수
- `REDIS_HOST` (기본: localhost:6379)
- `JWT_SECRET` (기본: 개발용 시크릿, 최소 32자)
