# 식당 주문 시스템

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.11-3776AB?style=flat-square&logo=python&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-0.111+-009688?style=flat-square&logo=fastapi&logoColor=white)
![Vue.js](https://img.shields.io/badge/Vue.js-3.0-4FC08D?style=flat-square&logo=vue.js&logoColor=white)
![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4o--mini-412991?style=flat-square&logo=openai&logoColor=white)
![ChromaDB](https://img.shields.io/badge/ChromaDB-Vector_DB-FF6B35?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)

---

## Architecture

```
[Customer Web :3000]  [Admin Web :3001]
          ↓                  ↓
         [Gateway :8080]
          ├── JWT Auth + Routing + WebSocket
          │
    ┌─────┼──────────┬───────────┐
    ↓     ↓          ↓           ↓
  [Auth] [Menu]   [Order]    [Review]
  :8081  :8082    :8083      :8084

    ┌────────────┬──────────┬──────────┐
    ↓            ↓          ↓          ↓
  [AI Rec.]  [AI Review] [AI Ops] [AI Valid.]
  :8001      :8002       :8003    :8004
```

---

## 주요 기능

| 기능 | 엔드포인트 | 설명 |
|---|---|---|
| 회원가입 / 로그인 | `POST /api/auth/register`, `/login` | JWT Access(15분) + Refresh(7일) |
| 메뉴 조회 / 관리 | `GET /api/menus`, `POST /api/menus` | 카테고리, 품절, 베스트셀러 |
| 주문 생성 / 추적 | `POST /api/orders`, `GET /api/orders/{id}` | WebSocket 실시간 상태 |
| 리뷰 작성 | `POST /api/reviews` | AI 초안 생성, 감성 분석 |
| AI 메뉴 추천 | `POST /api/recommendations` | RAG + ChromaDB 벡터 검색 |
| 운영 분석 | `GET /api/operations/congestion` | 혼잡도, 신메뉴 제안, 품질 분석 |

---

## 기본 계정

| 역할 | ID | 비밀번호 |
|------|----|----------|
| 관리자 | admin | admin1234 |
| 고객 | 고객 웹에서 가입 | - |

---

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 17, Python 3.11, TypeScript |
| Frontend | Vue.js 3, Vite, Vue Router, Axios, nginx |
| Backend Services | Spring Boot 3.2, SQLite (JDBC), Spring Security |
| AI Services | FastAPI, OpenAI GPT-4o-mini |
| Auth | JWT (JJWT), Redis (refresh token whitelist) |
| Vector Search | ChromaDB + sentence-transformers (all-MiniLM-L6-v2) |
| Infra | Docker Compose |

---

## 프로젝트 구조

```
├── admin-web/              # 관리자 대시보드 (Vue3 + Vite)
├── customer-web/           # 고객 주문 앱 (Vue3 + Vite)
├── services/               # 백엔드 마이크로서비스 (Spring Boot)
│   ├── gateway/            # API Gateway + WebSocket + JWT 검증
│   ├── auth/               # 회원가입·로그인 + JWT + Redis
│   ├── menu/               # 메뉴·카테고리 CRUD + 이미지 업로드
│   ├── order/              # 주문 생성·상태 추적 + 매출 통계
│   └── review/             # 리뷰 CRUD + AI 연동
├── ai-services/            # AI 마이크로서비스 (FastAPI)
│   ├── recommendation/     # RAG 기반 메뉴 추천 (ChromaDB)
│   ├── review-writer/      # 리뷰 초안 생성, 감성 분석, 요약
│   ├── operations/         # 혼잡도 분석, 신메뉴 제안, 품질 분석
│   └── validation/         # AI 입출력 유해어 필터 + GPT 검증
├── docker-compose.yml
└── .env.example
```

---

## 빠른 시작

```bash
git clone <repo> && cd MAS-Role

cp .env.example .env
# .env에 OPENAI_API_KEY, JWT_SECRET 입력

docker compose up --build
```

- 고객 앱: http://localhost:3000
- 관리자 앱: http://localhost:3001
- API Gateway: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

---

## 사용된 오픈소스 라이브러리

### Backend (Java)

| 패키지 | 라이선스 | 용도 |
|---|---|---|
| [Spring Boot 3](https://github.com/spring-projects/spring-boot) | Apache-2.0 | 마이크로서비스 프레임워크 |
| [JJWT](https://github.com/jwtk/jjwt) | Apache-2.0 | JWT 생성·검증 |
| [Spring Data Redis](https://github.com/spring-projects/spring-data-redis) | Apache-2.0 | Refresh 토큰 화이트리스트 |
| [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) | Apache-2.0 | 경량 임베디드 DB |

### AI Services (Python)

| 패키지 | 라이선스 | 용도 |
|---|---|---|
| [FastAPI](https://github.com/tiangolo/fastapi) | MIT | AI REST API 프레임워크 |
| [ChromaDB](https://github.com/chroma-core/chroma) | Apache-2.0 | 벡터 DB |
| [sentence-transformers](https://github.com/UKPLab/sentence-transformers) | Apache-2.0 | 텍스트 임베딩 |
| [openai](https://github.com/openai/openai-python) | MIT | GPT-4o-mini 호출 |

### Frontend (TypeScript)

| 패키지 | 라이선스 | 용도 |
|---|---|---|
| [Vue.js 3](https://github.com/vuejs/core) | MIT | UI 프레임워크 |
| [Vite](https://github.com/vitejs/vite) | MIT | 빌드 도구 |
| [Vue Router](https://github.com/vuejs/router) | MIT | SPA 라우팅 |
| [Axios](https://github.com/axios/axios) | MIT | HTTP 클라이언트 |
