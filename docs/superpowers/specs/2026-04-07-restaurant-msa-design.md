# 식당 주문 시스템 (고객용 앱 / 식당용 웹) - 설계 문서

## 개요

식당 주문/관리 시스템을 MSA(Microservice Architecture)로 구현한다. 고객용 웹에서 메뉴 탐색, 주문, AI 추천을 하고, 식당용 웹에서 메뉴/주문/후기/매출을 관리한다. 고객/관리자 모두 회원가입+로그인 필수.

---

## 1. 아키텍쳐

### 서비스 구성 (컨테이너 12개)

```
[Customer Web :3000]  [Admin Web :3001]
    (nginx, /api/** → Gateway로 reverse proxy)
          ↓                  ↓
         [Gateway Service :8080]
          ├── JWT 검증, 라우팅, WebSocket
          │
     ┌────┼──────────┬───────────┐
     ↓    ↓          ↓           ↓
  [Auth] [Menu]   [Order]    [Review]
  :8081  :8082    :8083      :8084
  auth.db menu.db order.db   review.db
     │               │
     └── [Redis] ────┘
          :6379

     ┌────────────┬──────────────┬───────────────┐
     ↓            ↓              ↓               ↓
  [AI Recommend] [AI Review]  [AI Ops]    [AI Validation]
  :8001          :8002        :8003       :8004
  ChromaDB
```

### 기술 스택

- 프론트엔드: Vue3 + Vite + Vue Router + Axios
- 서비스 백엔드: Java 17+ / Spring Boot 3.x / Spring Security / SQLite (JDBC)
- AI 백엔드: Python 3.11+ / FastAPI / OpenAI API (GPT-4o-mini) / Pydantic
- 벡터 검색: ChromaDB + sentence-transformers (all-MiniLM-L6-v2)
- 인프라: Docker Compose / Redis 7 / nginx

### 통신 규칙

- 프론트는 자기 nginx로만 요청 (:3000 또는 :3001)
- nginx가 `/api/**`, `/ws/**`를 Gateway(:8080)로 reverse proxy → CORS 문제 없음
- Gateway → 백엔드 서비스들 (내부 네트워크)
- 백엔드 서비스 → AI 서비스 (내부 네트워크)
- AI 서비스는 외부 노출 X
- 서비스 간 통신: REST API (동기 호출)
- 실시간: Gateway WebSocket만
- AI 호출 흐름: 프론트 → nginx → Gateway → 서비스 백엔드 → AI 서비스

### 서비스 간 호출 맵

```
Gateway → Auth (토큰 검증)
Order → Menu (가격 조회, 품절 확인)
Order → Menu (베스트셀러 플래그 갱신, 매일 자정 스케줄러)
Order → Gateway (WebSocket 알림)
Menu → AI Recommendation (임베딩 갱신)
Review → AI Review Writer (후기 초안, 감성 분석, 리뷰 요약)
Review → AI Recommendation (리뷰 요약 임베딩 갱신)
AI Ops → Order (진행중 주문 수, 주문 히스토리)
AI Ops → Menu (메뉴 목록, 조리시간)
```

---

## 2. 프론트엔드

### Customer Web (고객용, :3000)

페이지 구성:
- `/login` — 로그인
- `/register` — 회원가입
- `/` — 메뉴 페이지 (메인, 로그인 필수)
  - 왼쪽: 카테고리 nav bar
  - 가운데: 메뉴 카드 그리드 (이미지, 이름, 가격, 품절/베스트셀러 뱃지)
  - 오른쪽: AI 채팅 패널 (접었다 펼 수 있는 사이드바)
  - 하단: 장바구니 바 (담은 메뉴 수, 총 금액, 주문 버튼)
- `/order/{id}` — 주문 상태 추적 (WebSocket으로 실시간 업데이트)
- `/review/write/{orderId}` — 후기 작성 (키워드 선택 → AI 초안 생성 → 수정 후 등록)

상태 관리:
- 장바구니: localStorage
- 인증 토큰: localStorage (accessToken, refreshToken)
- 주문 상태: WebSocket 수신

nginx 설정:
```nginx
location /api/ { proxy_pass http://gateway:8080/api/; }
location /ws/  { proxy_pass http://gateway:8080/ws/; proxy_http_version 1.1; proxy_set_header Upgrade $http_upgrade; proxy_set_header Connection "upgrade"; }
location /images/ { proxy_pass http://menu-service:8082/images/; }
location / { try_files $uri /index.html; }
```

### Admin Web (식당용, :3001)

페이지 구성:
- `/login` — 관리자 로그인
- `/dashboard` — 대시보드 (오늘 매출, 주문 수, 평균 평점, 혼잡도)
- `/orders` — 실시간 주문 관리 (WebSocket, 상태 변경: 접수→조리중→완료)
- `/menus` — 메뉴 관리 (CRUD, 품절 토글, 이미지 업로드)
- `/reviews` — 후기 관리 + AI 감성 분석 요약 대시보드
- `/revenue` — 매출 정산 (일별/주별/월별 차트)
- `/ai/new-menu` — AI 신메뉴 추천
- `/ai/quality` — AI 서비스 품질 분석

관리자 계정:
- 시드 데이터: admin / admin1234
- 고객은 Customer Web에서 회원가입 후 사용
- Admin Web은 ADMIN 역할만 접근 가능

공통:
- Vue Router SPA, 비로그인 시 `/login`으로 리다이렉트
- Axios + JWT 자동 갱신 (interceptor)
- nginx reverse proxy로 Gateway 접근 (CORS 없음)

### 페이지네이션 공통 규칙

- 방식: offset/limit (cursor 불필요한 규모)
- 기본 page size: 20
- request: `?page=1&size=20`
- response에 `totalCount`, `totalPages`, `currentPage` 포함

---

## 3. 서비스 백엔드

### 3-1. Gateway Service (:8080)

역할:
- 프론트 단일 진입점 (라우팅만, 비즈니스 로직 X)
- JWT 파싱 및 권한 체크 → 하위 서비스에 `X-User-Id`, `X-User-Role` 헤더 전달
- WebSocket 서버 (프론트와의 실시간 연결 관리)
- 통합 Swagger UI

라우팅 규칙:
```
/api/auth/**     → Auth Service (8081)
/api/menus/**    → Menu Service (8082)
/api/orders/**   → Order Service (8083)
/api/reviews/**  → Review Service (8084)
/ws/**           → 자체 WebSocket 핸들러
```

인증 제외 경로 (JWT 검증 안 함):
- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/refresh`

WebSocket:
- 고객용: `ws://gateway/ws/orders/{userId}`
- 관리자용: `ws://gateway/ws/admin`
- 인증: 연결 시 query param으로 토큰 전달 (`?token=xxx`), Gateway에서 검증 후 연결 수립
- 재연결: 프론트에서 연결 끊김 감지 시 3초 후 재연결 시도, 최대 5회 (exponential backoff)
- 내부 API: `POST /internal/ws/notify` — Order Service가 상태 변경 시 호출

### 3-2. Auth Service (:8081) → auth.db

DB 스키마:
```sql
CREATE TABLE users (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    email       TEXT UNIQUE NOT NULL,
    password    TEXT NOT NULL,          -- BCrypt
    nickname    TEXT NOT NULL,
    role        TEXT NOT NULL DEFAULT 'USER',  -- USER | ADMIN
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

시드 데이터 (최초 기동 시):
```sql
INSERT OR IGNORE INTO users (email, password, nickname, role)
VALUES ('admin', '{BCRYPT_HASH of admin1234}', '관리자', 'ADMIN');
```

API:
- `POST /api/auth/register` — 회원가입 (role은 항상 USER)
- `POST /api/auth/login` — 로그인 (Access 15분 + Refresh 7일 발급)
- `POST /api/auth/refresh` — 토큰 갱신
- `POST /api/auth/verify` — 토큰 검증 (Gateway 내부용)
- `POST /api/auth/logout` — 로그아웃 (Redis에서 Refresh 삭제)
- `POST /api/auth/promote/{userId}` — 유저를 ADMIN으로 승격 (ADMIN만)

Redis: Refresh Token 화이트리스트 (`refresh:{userId}` → token, TTL 7일)

### 3-3. Menu Service (:8082) → menu.db

DB 스키마:
```sql
CREATE TABLE categories (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT UNIQUE NOT NULL,
    sort_order INTEGER DEFAULT 0
);

CREATE TABLE menus (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id        INTEGER NOT NULL REFERENCES categories(id),
    name               TEXT NOT NULL,
    description        TEXT,
    price              INTEGER NOT NULL,
    image_url          TEXT,
    tags               TEXT,                    -- 쉼표 구분
    allergens          TEXT,                    -- 쉼표 구분
    spicy_level        INTEGER DEFAULT 0,       -- 0~3
    cook_time_minutes  INTEGER DEFAULT 15,      -- 기본 조리시간 (분)
    is_sold_out        INTEGER DEFAULT 0,
    is_best            INTEGER DEFAULT 0,
    created_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

API:
- `GET /api/menus` — 전체 메뉴 (카테고리별, 키워드 검색, 페이지네이션)
- `GET /api/menus/{id}` — 상세
- `GET /api/menus/best` — 베스트셀러
- `GET /api/menus/categories` — 카테고리 목록
- `POST /api/menus` — 등록 (ADMIN, multipart/form-data)
- `PUT /api/menus/{id}` — 수정 (ADMIN)
- `DELETE /api/menus/{id}` — 삭제 (ADMIN)
- `PATCH /api/menus/{id}/sold-out` — 품절 토글 (ADMIN)
- `POST /api/menus/categories` — 카테고리 등록 (ADMIN)

이미지 서빙:
- 업로드: 로컬 volume (`/data/images/`)에 저장, DB에는 `/images/{filename}` 저장
- 서빙: Menu Service가 `/images/**` 경로로 static file 서빙
- 프론트 접근: nginx가 `/images/**`를 Menu Service로 프록시

AI 연동:
- 메뉴 등록/수정 시 AI Recommendation에 `POST /embeddings/sync` 호출
- 메뉴 삭제 시 `DELETE /embeddings/{menuId}` 호출
- 호출 실패해도 CRUD는 성공 처리 (로그만 남김)

내부 API:
- `GET /internal/menus` — 전체 메뉴 목록 (AI Operations가 호출)
- `GET /internal/menus/{id}` — 메뉴 상세 + cook_time_minutes (AI Operations가 호출)
- `PATCH /internal/menus/{id}/best` — 베스트셀러 플래그 갱신 (Order Service가 호출)

### 3-4. Order Service (:8083) → order.db

DB 스키마:
```sql
CREATE TABLE orders (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    status      TEXT NOT NULL DEFAULT 'PENDING',  -- PENDING | COOKING | DONE
    total_price INTEGER NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id  INTEGER NOT NULL REFERENCES orders(id),
    menu_id   INTEGER NOT NULL,
    menu_name TEXT NOT NULL,       -- 주문 시점 스냅샷
    price     INTEGER NOT NULL,    -- 주문 시점 스냅샷
    quantity  INTEGER NOT NULL DEFAULT 1
);
```

API:
- `POST /api/orders` — 주문 생성
  - Menu Service에서 가격/품절 확인 (REST)
  - 품절 메뉴 포함 시 400 에러 반환 (재고 개념 없음, 품절은 관리자 수동 토글이므로 race condition 무시)
  - 가격 스냅샷 저장 → Gateway WebSocket 알림
- `GET /api/orders` — 내 주문 목록 (페이지네이션)
- `GET /api/orders/{id}` — 상세
- `GET /api/orders/active` — 진행중 주문
- `PATCH /api/orders/{id}/status` — 상태 변경 (ADMIN, Gateway WebSocket 알림)
- `GET /api/orders/stats/revenue` — 매출 요약 (daily/weekly/monthly, from/to)
- `GET /api/orders/stats/best-sellers` — 베스트셀러 집계 (limit, period)

베스트셀러 갱신:
- `@Scheduled` 매일 자정 실행
- 최근 7일 주문 데이터 집계 → 상위 N개 메뉴 ID 추출
- Menu Service에 `PATCH /internal/menus/{id}/best` 호출로 플래그 갱신

내부 API (AI용):
- `GET /internal/orders/history?days=30` — 최근 N일 주문 히스토리 (메뉴별 주문 수, 카테고리별 비율, 시간대별 패턴)
- `GET /internal/orders/active-count` — 현재 진행중 주문 수 (`{ "activeCount": 8, "pendingCount": 3, "cookingCount": 5 }`)

### 3-5. Review Service (:8084) → review.db

DB 스키마:
```sql
CREATE TABLE reviews (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL,
    order_id        INTEGER NOT NULL,
    menu_id         INTEGER NOT NULL,
    menu_name       TEXT NOT NULL,
    rating          INTEGER NOT NULL,     -- 1~5
    content         TEXT NOT NULL,
    is_ai_generated INTEGER DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

API:
- `POST /api/reviews` — 후기 등록
  - review.db에 즉시 저장 → 사용자에게 즉시 응답 반환
  - 이후 비동기로: AI Review Writer에 리뷰 요약 요청 → AI Recommendation에 임베딩 갱신
  - 비동기 처리: `@Async` + `CompletableFuture` (실패해도 리뷰 등록은 이미 완료)
- `GET /api/reviews` — 조회 (메뉴별 필터, 페이지네이션)
- `GET /api/reviews/{id}` — 상세
- `DELETE /api/reviews/{id}` — 삭제 (본인 또는 ADMIN)
- `GET /api/reviews/summary` — 전체 평점 요약
- `GET /api/reviews/summary/menu/{menuId}` — 메뉴별 평점
- `POST /api/reviews/ai/generate` — AI 후기 초안 요청 (동기, → AI Review Writer)
- `POST /api/reviews/ai/analyze` — AI 감성 분석 요청 (동기, → AI Review Writer)

AI Validation 호출 주체:
- Review Service가 AI 서비스 호출 전에 Validation 호출 (서비스 백엔드가 오케스트레이션)
- 흐름: Review Service → AI Validation(입력 검증) → AI Review Writer → AI Validation(출력 검증) → 응답

---

## 4. AI 서비스 백엔드

### 4-1. AI Recommendation Service (:8001) + ChromaDB

RAG 기반 메뉴 추천:
1. 메뉴 데이터 + 리뷰 요약 + 평균 평점 + 주문 수를 텍스트로 결합
2. sentence-transformers (all-MiniLM-L6-v2)로 임베딩 → ChromaDB에 저장
3. 사용자 질문 임베딩 → ChromaDB 유사도 top-5 추출
4. 추출된 메뉴만 GPT context에 넣어서 최종 추천

임베딩 텍스트 예시:
```
김치찌개 | 매콤한 돼지고기 김치찌개 | 매운,고기,국물 | 대두,밀 | 맵기2
평점 4.3 | 주문수 120 | 리뷰요약: 국물이 깊고 양이 많다, 맵기가 적당하다
```

초기화:
- 서버 기동 시 (`@app.on_event("startup")`) Menu Service에서 전체 메뉴 조회 (`GET /internal/menus`)
- 전체 메뉴 벌크 임베딩 → ChromaDB에 저장
- Menu Service가 아직 안 떠있으면 30초 간격으로 재시도 (최대 10회)

API:
- `POST /recommendations/chat` — 채팅 기반 추천
- `POST /recommendations/keyword` — 키워드 기반 추천
- `POST /embeddings/sync` — 메뉴 임베딩 동기화 (Menu Service/Review Service가 호출)
- `POST /embeddings/bulk-sync` — 전체 메뉴 벌크 임베딩 (초기화용)
- `DELETE /embeddings/{menu_id}` — 임베딩 제거

임베딩 갱신 타이밍:
- 메뉴 등록/수정/삭제 시 (Menu Service가 호출)
- 리뷰 등록 시 (Review Service가 비동기로 리뷰 요약 생성 후 호출)
- 베스트셀러 갱신 시 (Order Service 스케줄러가 호출)
- 서버 기동 시 (벌크 초기화)

GPT 프롬프트:
```
시스템: 너는 식당 메뉴 추천 도우미다. 아래 메뉴 후보 중에서 사용자의 요청에 가장 적합한 메뉴를 1~3개 추천해라. 추천 이유를 간결하게 설명해라. 메뉴 후보에 없는 메뉴는 추천하지 마라.

메뉴 후보:
{ChromaDB top-k 결과}

사용자: {입력}
```

Fallback: `{ "menu_ids": [], "reason": "추천 서비스를 일시적으로 이용할 수 없습니다." }`

### 4-2. AI Review Writer Service (:8002)

기능 A: 후기 초안 생성
- 키워드 + 메뉴명 + 별점 → GPT가 자연스러운 후기 3~5문장 생성
- 별점 1~5 범위 clamp 처리

기능 B: 리뷰 감성 분석 요약
- 최근 리뷰 N개 → GPT가 긍정/부정 비율 + 키워드 + 개선점 추출

기능 C: 메뉴별 리뷰 요약 텍스트 생성
- 해당 메뉴 전체 리뷰 → GPT가 요약 → AI Recommendation 임베딩용

API:
- `POST /reviews/generate` — 후기 초안
- `POST /reviews/analyze` — 감성 분석
- `POST /reviews/summarize` — 메뉴별 리뷰 요약 (임베딩용)

Fallback:
- 후기: `{ "draft": "맛있게 잘 먹었습니다.", "rating": {입력 별점} }`
- 분석: `{ "summary": "분석을 일시적으로 이용할 수 없습니다." }`

### 4-3. AI Operations Service (:8003)

기능 A: 혼잡도 분석 (규칙 기반, GPT 호출 X)
- Order Service에서 진행중 주문 수 조회 (`GET /internal/orders/active-count`)
- 0~5: 여유, 6~15: 보통, 16+: 혼잡
- 메뉴별 예상 소요 시간: Menu Service에서 cook_time_minutes 조회 (`GET /internal/menus/{id}`) × 혼잡도 계수 (여유: 1.0, 보통: 1.5, 혼잡: 2.0)
- 임계값은 환경변수로 관리

기능 B: 신메뉴 추천
- Order Service에서 주문 히스토리 (`GET /internal/orders/history?days=30`)
- Menu Service에서 현재 메뉴 (`GET /internal/menus`)
- GPT가 신메뉴 3개 추천

기능 C: 서비스 품질 분석
- 리뷰 + 주문 데이터 → GPT가 종합 분석 (강점, 약점, 개선 제안)

API:
- `GET /operations/congestion` — 혼잡도
- `GET /operations/congestion/menu/{menu_id}` — 메뉴별 예상 시간
- `POST /operations/new-menu-suggest` — 신메뉴 추천
- `POST /operations/quality-analysis` — 품질 분석

### 4-4. AI Validation Service (:8004)

기능 A: Violation (입력 검증)
- 1차: 금지 키워드 필터 (GPT 호출 X)
- 2차: GPT 판단 (욕설, 무관한 내용 등)

기능 B: Quality (출력 검증)
- GPT 응답이 질문에 적절한지, 환각 없는지 판단

호출 주체: 서비스 백엔드 (Review Service, Menu Service 등)가 AI 서비스 호출 전후에 Validation 호출
- 요청 전: 서비스 백엔드 → AI Validation(입력) → AI 서비스
- 응답 후: AI 서비스 → AI Validation(출력) → 서비스 백엔드

API:
- `POST /validation/input` — 입력 검증
- `POST /validation/output` — 출력 검증

Fallback: 검증 실패 시 `is_valid: true`로 통과 (서비스 전체가 막히면 안 됨)

---

## 5. 인프라

### Docker Compose

```yaml
# 프론트엔드
customer-web:       nginx:alpine (:3000, reverse proxy → gateway:8080)
admin-web:          nginx:alpine (:3001, reverse proxy → gateway:8080)

# Gateway
gateway-service:    Spring Boot (:8080)

# 서비스 백엔드
auth-service:       Spring Boot (:8081) + auth_data volume
menu-service:       Spring Boot (:8082) + menu_data, image_data volume
order-service:      Spring Boot (:8083) + order_data volume
review-service:     Spring Boot (:8084) + review_data volume

# AI 백엔드
ai-recommendation:  FastAPI (:8001) + chroma_data volume
ai-review-writer:   FastAPI (:8002)
ai-operations:      FastAPI (:8003)
ai-validation:      FastAPI (:8004)

# 인프라
redis:              redis:7-alpine (:6379) + redis_data volume
```

### 네트워크

- `frontend-net`: customer-web, admin-web, gateway, menu-service(이미지 서빙)
- `backend-net`: gateway, 백엔드 서비스들, AI 서비스들, redis
- 외부 노출 포트: 3000 (고객), 3001 (관리자)만
- Gateway 8080은 외부 노출 불필요 (nginx가 프록시)

### 환경변수

- 각 서비스마다 `.env` 파일
- `OPENAI_API_KEY`는 AI 서비스들만
- Gateway: 하위 서비스 URL들
- Auth: Redis 접속 정보, JWT 시크릿
- 서비스 백엔드들: 자기 DB 경로, 연동할 서비스 URL

### 기동 순서 (depends_on)

```
redis → auth-service → gateway-service
                     → menu-service → ai-recommendation
                     → order-service
                     → review-service → ai-review-writer
                                     → ai-validation
                     → ai-operations
customer-web (gateway 의존)
admin-web (gateway 의존)
```

### 빌드/실행

```bash
docker compose up --build    # 전체 빌드 & 실행
docker compose up -d         # 백그라운드
docker compose down -v       # 종료 + 볼륨 삭제
```
