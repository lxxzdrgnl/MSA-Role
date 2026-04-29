# Review Service

## 개요
리뷰 CRUD, AI 리뷰 생성/분석, 리뷰 통계, 임베딩 동기화를 담당하는 마이크로서비스.

## 기술 스택
- Spring Boot 3.2.5 / Java 17 / Gradle
- SQLite (파일 DB: `/data/review.db`)
- Spring JDBC (JdbcTemplate)
- Spring Async (비동기 임베딩 동기화)
- SpringDoc OpenAPI 2.4.0

## 포트
- 8084

## API 엔드포인트

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/reviews` | 리뷰 작성 | X-User-Id |
| GET | `/api/reviews` | 리뷰 목록 (menuId 필터, 페이지네이션) | 불필요 |
| GET | `/api/reviews/{id}` | 리뷰 상세 | 불필요 |
| DELETE | `/api/reviews/{id}` | 리뷰 삭제 (작성자 또는 ADMIN) | X-User-Id/Role |
| GET | `/api/reviews/summary` | 리뷰 통계 (평균 별점, 분포) | 불필요 |
| GET | `/api/reviews/by-order` | 주문별 리뷰 조회 | 불필요 |
| GET | `/api/reviews/ai-summary` | AI 리뷰 요약 (30분 캐시, 최소 3개 리뷰) | 불필요 |
| GET | `/api/reviews/my` | 내 리뷰 목록 | X-User-Id |
| POST | `/api/reviews/generate` | AI 리뷰 초안 생성 | 불필요 |
| PATCH | `/api/reviews/{id}/reply` | 관리자 답변 등록/수정 | ADMIN |

## 데이터 모델

### reviews 테이블
- `id`, `user_id`, `order_id`, `menu_id`, `menu_name`, `rating` (1-5), `content`, `is_ai_generated`, `admin_reply`, `admin_reply_at`, `created_at`

## AI 연동
- **리뷰 생성**: AI Review Writer로 초안 생성 → AI Validation으로 검증
- **리뷰 분석**: 최근 50개 리뷰를 AI Review Writer로 분석 (감정, 키워드, 개선점)
- **임베딩 동기화**: 리뷰 작성 시 비동기로 AI Review Writer 요약 → AI Recommendation 임베딩 업데이트

## 캐싱
- AI 리뷰 요약: ConcurrentHashMap, 30분 TTL, 리뷰 수 변경 시 무효화

## 비동기 처리
- ThreadPoolTaskExecutor (core=2, max=5, queue=100)

## 서비스 간 통신
- **Auth Service** (`AUTH_SERVICE_URL`): 리뷰 응답에 닉네임 포함을 위해 `POST /api/auth/nicknames` 호출 (API Composition 패턴)

## 환경변수
- `AUTH_SERVICE_URL` (기본: http://localhost:8081)
- `AI_REVIEW_WRITER_URL` (기본: http://localhost:8002)
- `AI_RECOMMENDATION_URL` (기본: http://localhost:8001)
- `AI_VALIDATION_URL` (기본: http://localhost:8004)
