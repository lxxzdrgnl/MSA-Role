# MAS-Role — 레스토랑 MSA 주문 시스템

## 프로젝트 구조

레스토랑 주문 관리 시스템으로, 11개 마이크로서비스와 2개 프론트엔드로 구성된다.

## 서비스별 상세 문서

각 서비스의 기능, API, 데이터 모델 등은 해당 폴더의 CLAUDE.md를 참고한다.

### Backend Services (Spring Boot / Java 17)
- **[Auth Service](services/auth/CLAUDE.md)** — 인증/인가, JWT 토큰, 사용자 관리 (포트 8081)
- **[Gateway Service](services/gateway/CLAUDE.md)** — API Gateway, 라우팅, JWT 필터, WebSocket (포트 8080)
- **[Menu Service](services/menu/CLAUDE.md)** — 메뉴/카테고리 CRUD, 이미지 업로드 (포트 8082)
- **[Order Service](services/order/CLAUDE.md)** — 주문 생성/관리, 매출 통계, 베스트셀러 (포트 8083)
- **[Review Service](services/review/CLAUDE.md)** — 리뷰 CRUD, AI 리뷰 생성/분석 (포트 8084)

### AI Services (FastAPI / Python)
- **[AI Recommendation](ai-services/recommendation/CLAUDE.md)** — RAG 기반 메뉴 추천, ChromaDB 벡터 검색 (포트 8001)
- **[AI Review Writer](ai-services/review-writer/CLAUDE.md)** — 리뷰 생성/분석/요약 (포트 8002)
- **[AI Operations](ai-services/operations/CLAUDE.md)** — 혼잡도, 신메뉴 제안, 품질 분석 (포트 8003)
- **[AI Validation](ai-services/validation/CLAUDE.md)** — 입력 검증, 환각 탐지 (포트 8004)

### Frontend (Vue 3 / Vite)
- **[Admin Web](admin-web/CLAUDE.md)** — 관리자 대시보드 (SVG 차트), 주문 관리 (신규주문/목록 탭), 메뉴/카테고리 CRUD (모달), 컴포넌트/유틸/composable 분리 구조
- **[Customer Web](customer-web/CLAUDE.md)** — 고객 메뉴 탐색, 장바구니, 주문

## 서비스 간 통신 흐름

```
Client → Gateway(8080) → Auth(8081) / Menu(8082) / Order(8083) / Review(8084)
                        → AI Recommendation(8001) / AI Operations(8003)

Menu ←→ AI Recommendation (임베딩 동기화)
Review → AI Review Writer(8002) → AI Recommendation (요약 임베딩)
Review → AI Validation(8004) (출력 검증)
Order → Gateway (WebSocket 실시간 알림)
Order → Menu (베스트셀러 플래그 업데이트)
AI Operations → Order + Menu (운영 데이터 조회)
```

## 공통 패턴
- DB: SQLite (각 서비스 `/data/*.db`), AI 서비스는 DB 없음 (ChromaDB 제외)
- 인증: Gateway가 JWT 검증 후 `X-User-Id`, `X-User-Role` 헤더로 전달
- 문서: 각 서비스 SpringDoc OpenAPI (Swagger UI)
- 컨테이너: Docker (docker-compose.yml)

## 에러 응답 규격
- 모든 서비스 통일 JSON 포맷: `{timestamp, path, status, code, message, details}`
- 15종 표준 에러 코드 (ErrorCode enum / errors.py)
- Spring Boot: `GlobalExceptionHandler` (@RestControllerAdvice) + `BusinessException`
- FastAPI: `errors.py` (register_error_handlers) + `BusinessError`

## 페이지네이션 규격
- 0-indexed page, size (기본 20, 최대 100), sort (field,ASC|DESC)
- 응답: `{content, page, size, totalElements, totalPages, sort}`
- Menu: category, keyword 필터 / Order: status, dateFrom/dateTo 필터 / Review: menuId, rating 필터

## 로깅
- Spring Boot: `LoggingFilter` (OncePerRequestFilter) — 메서드, 경로, 상태코드, 지연시간
- FastAPI: `LoggingMiddleware` (BaseHTTPMiddleware) — 동일 포맷

## 헬스체크
- 모든 서비스: `GET /health` → `{status, version, buildTime}` (인증 불필요)

## 환경변수
- 각 서비스에 `.env.example` 제공
- 비밀키(JWT_SECRET, OPENAI_API_KEY)는 절대 코드에 하드코딩 금지
