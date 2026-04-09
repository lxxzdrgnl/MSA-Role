# Gateway Service

## 개요
API Gateway. 모든 클라이언트 요청의 단일 진입점으로, 라우팅·JWT 인증·WebSocket 실시간 알림을 처리한다.

## 기술 스택
- Spring Boot 3.2.5 / Java 17 / Gradle
- Apache HttpClient5 + RestTemplate (프록시)
- Spring WebSocket
- SpringDoc OpenAPI 2.4.0

## 포트
- 8080

## 라우팅 맵

| 경로 패턴 | 대상 서비스 | 기본 포트 |
|-----------|------------|----------|
| `/api/auth/*` | Auth Service | 8081 |
| `/api/menus/*` | Menu Service | 8082 |
| `/api/orders/*` | Order Service | 8083 |
| `/api/reviews/*` | Review Service | 8084 |
| `/api/recommendations/*` | AI Recommendation | 8001 |
| `/api/operations/*` | AI Operations | 8003 |

## 인증 필터 (JwtAuthFilter)
- `Authorization: Bearer <token>` 헤더에서 JWT 추출
- Auth Service `/api/auth/verify`로 토큰 검증
- 검증 후 `X-User-Id`, `X-User-Role` 헤더를 다운스트림에 전달

### 공개 경로 (인증 불필요)
- `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`
- GET `/api/menus/*` (메뉴 조회)
- GET `/api/operations/congestion` (혼잡도 조회)
- `/ws/*` WebSocket 엔드포인트
- Swagger/OpenAPI 문서 경로

## WebSocket
- `/ws/orders/{userId}` — 사용자별 주문 알림
- `/ws/admin` — 관리자 브로드캐스트 알림
- 내부 REST: `POST /internal/ws/notify` — 다른 서비스가 Gateway를 통해 알림 전송

## 프록시 동작
- 모든 HTTP 메서드 지원 (GET/POST/PUT/PATCH/DELETE)
- raw bytes로 요청/응답 전달 (JSON, multipart, form-encoded 모두 지원)
- 5초 연결 타임아웃
- 상태 코드 및 응답 헤더 투명 전달

## 환경변수
- `AUTH_SERVICE_URL` (기본: http://localhost:8081)
- `MENU_SERVICE_URL` (기본: http://localhost:8082)
- `ORDER_SERVICE_URL` (기본: http://localhost:8083)
- `REVIEW_SERVICE_URL` (기본: http://localhost:8084)
- `AI_RECOMMENDATION_URL` (기본: http://localhost:8001)
- `AI_OPERATIONS_URL` (기본: http://localhost:8003)
