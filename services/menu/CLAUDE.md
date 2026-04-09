# Menu Service

## 개요
레스토랑 메뉴 및 카테고리 CRUD 관리 서비스. 이미지 업로드, 베스트셀러/품절 관리, AI 추천 서비스와 임베딩 동기화를 담당한다.

## 기술 스택
- Spring Boot 3.2.5 / Java 17 / Gradle
- SQLite (파일 DB: `/data/menu.db`)
- Spring JDBC (JdbcTemplate)
- SpringDoc OpenAPI 2.4.0

## 포트
- 8082

## API 엔드포인트

### 공개 API (`/api/menus`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/menus` | 메뉴 목록 (페이지네이션, 카테고리/키워드 필터) | 불필요 |
| GET | `/api/menus/{id}` | 메뉴 상세 | 불필요 |
| GET | `/api/menus/best` | 베스트셀러 목록 | 불필요 |
| GET | `/api/menus/categories` | 카테고리 목록 | 불필요 |
| POST | `/api/menus` | 메뉴 생성 (이미지 업로드) | ADMIN |
| PUT | `/api/menus/{id}` | 메뉴 수정 (이미지 업로드) | ADMIN |
| DELETE | `/api/menus/{id}` | 메뉴 삭제 | ADMIN |
| PATCH | `/api/menus/{id}/sold-out` | 품절 토글 | ADMIN |
| PATCH | `/api/menus/{id}/best-seller` | 베스트셀러 토글 | ADMIN |
| POST | `/api/menus/categories` | 카테고리 생성 | ADMIN |
| DELETE | `/api/menus/categories/{id}` | 카테고리 삭제 | ADMIN |

### 내부 API (`/internal/menus`)
- GET `/internal/menus` — 전체 메뉴 조회 (서비스 간 통신용)
- GET `/internal/menus/{id}` — ID로 메뉴 조회
- PATCH `/internal/menus/{id}/best` — 베스트셀러 플래그 업데이트

## 데이터 모델

### menus 테이블
- `id`, `category_id` (FK), `name`, `description`, `price`, `image_url`, `tags`, `allergens`, `spicy_level` (0-3), `cook_time_minutes`, `is_sold_out`, `is_best`, `created_at`, `updated_at`

### categories 테이블
- `id`, `name` (UNIQUE), `sort_order`

## 이미지 업로드
- 저장 경로: `/data/images/` (UUID 접두사 파일명)
- 최대 파일 크기: 20MB
- `/images/**` 경로로 정적 파일 서빙

## AI 임베딩 동기화
- 메뉴 생성/수정/삭제 시 AI Recommendation 서비스에 임베딩 동기화 (fire-and-forget)

## 초기 데이터
- 5개 카테고리, 12개 메뉴 아이템 시드

## 환경변수
- `AI_RECOMMENDATION_URL` (기본: http://ai-recommendation:8001)
