# Rheon Kitchen — 레스토랑 MSA 주문 시스템

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.11-3776AB?style=flat-square&logo=python&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-0.111+-009688?style=flat-square&logo=fastapi&logoColor=white)
![Vue.js](https://img.shields.io/badge/Vue.js-3.0-4FC08D?style=flat-square&logo=vue.js&logoColor=white)
![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4o--mini-412991?style=flat-square&logo=openai&logoColor=white)
![ChromaDB](https://img.shields.io/badge/ChromaDB-Vector_DB-FF6B35?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)

---

## 시스템 아키텍처

```
[Customer Web :3000]  [Admin Web :3001]
          ↓                  ↓
         [Gateway :8080]
          ├── JWT 인증 + Rate Limiting + 요청 로깅 + WebSocket
          │
    ┌─────┼──────────┬───────────┐
    ↓     ↓          ↓           ↓
  [Auth] [Menu]   [Order]    [Review]
  :8081  :8082    :8083      :8084
    │                │          │
    └── Redis ───────┘          └── Redis (리뷰 요약 캐시)

    ┌────────────┬──────────┬──────────┐
    ↓            ↓          ↓          ↓
  [AI 추천]  [AI 리뷰]  [AI 운영] [AI 검증]
  :8001      :8002       :8003    :8004
```

5개 백엔드 서비스 (Spring Boot) + 4개 AI 서비스 (FastAPI) + 2개 프론트엔드 (Vue 3)로 구성된 마이크로서비스 아키텍처입니다.

---

## 주요 기능

### 백엔드

| 서비스 | 기능 |
|---|---|
| **Gateway** | JWT 필터, Rate Limiting (로그인 10회/분, API 60회/분), 요청 로깅, Multipart 프록시, 이미지 프록시, WebSocket 알림 |
| **Auth** | 회원가입/로그인, JWT Access(15분) + Refresh(7일, Redis), 프로필 조회/닉네임 변경, 역할 기반 인가 (USER/ADMIN) |
| **Menu** | 메뉴/카테고리 CRUD, 이미지 업로드 (수정/삭제 시 파일 정리), 품절/베스트셀러 토글, AI 임베딩 동기화 |
| **Order** | 주문 생성, 상태 관리 (PENDING→ACCEPTED→COOKING→COMPLETED/CANCELLED), 매출 통계, 베스트셀러 스케줄러, 고객 주문 취소 API |
| **Review** | 리뷰 CRUD, AI 리뷰 초안 생성, 감성 분석, Redis 기반 AI 요약 캐시 (30분 TTL), 닉네임 표시 |

### AI 서비스

| 서비스 | 기능 |
|---|---|
| **AI Recommendation** | RAG 기반 메뉴 추천 (ChromaDB 벡터 검색 + GPT-4o-mini), 대화형/키워드 검색, 메뉴 임베딩 자동 동기화 |
| **AI Review Writer** | 리뷰 초안 생성 (별점/키워드 기반), 리뷰 분석 (감정/키워드/개선점), 메뉴 임베딩용 요약 |
| **AI Operations** | 실시간 혼잡도 모니터링, 주문 이력 기반 신메뉴 3개 제안, 리뷰+주문 데이터 품질 분석 |
| **AI Validation** | 2단계 입력 검증 (금지어 필터 + GPT 의미 분석), AI 출력 환각(hallucination) 탐지 |

### 프론트엔드

| 앱 | 기능 |
|---|---|
| **Admin Web** | 대시보드 (KPI, 매출 차트, 도넛 차트, 혼잡도 게이지, 날짜 범위 필터), 주문 관리 (신규 주문 대기열 + 주문 목록, 일방향 상태 버튼, 날짜별 번호), 메뉴/카테고리 CRUD (모달) |
| **Customer Web** | 3컬럼 메뉴 탐색 (카테고리 사이드바 + 메뉴 그리드 + AI 채팅), 장바구니, 주문 실시간 추적 (WebSocket), 리뷰 작성 (AI 초안), 프로필 (닉네임 변경, 내 리뷰) |

---

## 주문 상태 흐름

```
PENDING → ACCEPTED → COOKING → COMPLETED
   ↓         ↓          ↓
            CANCELLED
```

- **신규 주문 탭**: PENDING 주문을 확인(→ACCEPTED) 또는 취소(→CANCELLED)
- **주문 목록 탭**: 일방향 상태 버튼으로 ACCEPTED→COOKING→COMPLETED 진행
- 역방향 전환 불가 (예: 조리중 → 주문확인 불가)

---

## 기본 계정

| 역할 | 아이디 | 비밀번호 |
|------|--------|----------|
| 관리자 | admin | admin1234 |
| 고객 | 고객 웹에서 회원가입 | - |

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| 언어 | Java 17, Python 3.11 |
| 프론트엔드 | Vue.js 3, Vite 5, Vue Router, Axios, nginx |
| 백엔드 | Spring Boot 3.2, SQLite (JDBC), Spring Security |
| AI | FastAPI, OpenAI GPT-4o-mini |
| 인증 | JWT (JJWT), Redis (리프레시 토큰 + 리뷰 캐시) |
| 벡터 검색 | ChromaDB + sentence-transformers (ko-sroberta-multitask) |
| 인프라 | Docker Compose, Health Check, KST 타임존 |
| 디자인 | 커스텀 CSS (UI 라이브러리 미사용), CSS Variables 디자인 토큰, 다크 테마 |

---

## 프로젝트 구조

```
├── admin-web/                    # 관리자 대시보드 (Vue 3 + Vite)
│   └── src/
│       ├── styles/global.css     #   디자인 토큰 기반 글로벌 CSS
│       ├── utils/                #   formatters.js, status.js
│       ├── composables/          #   useAsync.js (비동기 래퍼)
│       ├── components/           #   ModalWrapper, CategoryModal, RevenueChart,
│       │                         #   StatusDonut, AppPagination, StatusPill, EmptyState
│       └── views/                #   Dashboard, Orders, Menus, Login
│
├── customer-web/                 # 고객 주문 앱 (Vue 3 + Vite)
│   └── src/
│       ├── style.css             #   글로벌 CSS (Izakaya Counter 테마)
│       ├── composables/          #   useAuth, useCart, useFormatting
│       ├── constants/            #   상태맵, 이모지, 키워드
│       ├── components/           #   ChatPanel, ProfileModal, OrderList, ...
│       └── views/                #   Menu, Cart, Orders, Login, Register
│
├── services/                     # 백엔드 마이크로서비스 (Spring Boot)
│   ├── gateway/                  #   API Gateway + WebSocket + JWT + Rate Limit
│   ├── auth/                     #   인증 + JWT + Redis + 프로필
│   ├── menu/                     #   메뉴/카테고리 CRUD + 이미지 업로드
│   ├── order/                    #   주문 생명주기 + 매출 통계 + 취소 API
│   └── review/                   #   리뷰 CRUD + AI 연동 + Redis 캐시
│
├── ai-services/                  # AI 마이크로서비스 (FastAPI)
│   ├── recommendation/           #   RAG 메뉴 추천 (ChromaDB)
│   ├── review-writer/            #   리뷰 생성/분석/요약
│   ├── operations/               #   혼잡도, 신메뉴 제안, 품질 분석
│   └── validation/               #   입출력 검증 + 환각 탐지
│
├── docker-compose.yml
└── .env.example
```

---

## 빠른 시작

```bash
git clone <repo> && cd MAS-Role

cp .env.example .env
# .env에 OPENAI_API_KEY, JWT_SECRET 설정

docker compose up --build
```

- 고객 앱: http://localhost:3000
- 관리자 앱: http://localhost:3001
- API Gateway: http://localhost:8080

### 개발 모드 (프론트엔드 핫리로드)

```bash
# 백엔드만 Docker로 실행
docker compose up -d redis auth-service menu-service order-service review-service gateway-service ai-recommendation ai-review-writer ai-operations ai-validation

# 프론트엔드 dev 서버 실행
cd customer-web && npm run dev -- --port 3000
cd admin-web && npm run dev -- --port 3001
```

---

## 서비스 간 통신

```
Client → Gateway(8080) → Auth(8081) / Menu(8082) / Order(8083) / Review(8084)
                        → AI Recommendation(8001) / AI Operations(8003)

Menu ←→ AI Recommendation (메뉴 임베딩 동기화)
Review → AI Review Writer(8002) → AI Recommendation (요약 임베딩)
Review → AI Validation(8004) (AI 출력 검증)
Review → Auth(8081) (닉네임 조회)
Order → Gateway (WebSocket 실시간 알림)
Order → Menu (베스트셀러 플래그 업데이트, 매일 자정 스케줄)
AI Operations → Order + Menu (운영 데이터 조회)
```

---

## 프론트엔드 설계 패턴

양쪽 프론트엔드 공통 아키텍처:

| 패턴 | 설명 |
|---|---|
| **Composables** | `useAsync` (비동기 래퍼), `useAuth` (토큰 관리), `useCart` (장바구니) |
| **Utils** | `formatters.js` (가격/날짜 포맷), `status.js` (상태 상수/헬퍼) |
| **글로벌 CSS** | CSS Variables 디자인 토큰, 공통 유틸리티 클래스 |
| **모달 시스템** | `ModalWrapper` 컴포넌트 (Teleport + slots) |
| **API 레이어** | Axios 인스턴스 + JWT 인터셉터 + 401 자동 로그아웃 |

### 디자인 시스템

- **테마**: 다크 모드 (Izakaya Counter 톤, 양쪽 앱 공유)
- **액센트**: `#d4863c` (burnt orange)
- **컬러 체계**: 3톤 — accent (활성), neutral (완료), danger (에러/취소)
- **폰트**: Pretendard Variable (admin), Playfair Display + Noto Sans KR (customer)

---

## 환경변수 (.env)

| 변수 | 설명 | 필수 |
|---|---|---|
| `OPENAI_API_KEY` | OpenAI API 키 | O |
| `JWT_SECRET` | JWT 서명 시크릿 (32자 이상) | O |

---

## Docker 서비스 포트

| 서비스 | 외부 포트 | 내부 포트 |
|---|---|---|
| Gateway | 8080 | 8080 |
| Auth | 8081 | 8081 |
| Menu | 8082 | 8082 |
| Order | 8083 | 8083 |
| Review | 8084 | 8084 |
| AI Recommendation | 8001 | 8000 |
| AI Review Writer | 8002 | 8000 |
| AI Operations | 8003 | 8000 |
| AI Validation | 8004 | 8000 |
| Redis | 6380 | 6379 |
| Customer Web | 3000 | 80 |
| Admin Web | 3001 | 80 |
