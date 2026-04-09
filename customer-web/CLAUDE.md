# Customer Web

## 개요
고객용 주문 웹 앱. 메뉴 탐색, 장바구니, 주문, AI 추천, 프로필 관리 기능을 제공한다.

## 기술 스택
- Vue 3 (Composition API, `<script setup>`)
- Vue Router 4.2.0
- Axios 1.6.0
- Vite 5.0.0
- 커스텀 CSS + CSS Variables (UI 라이브러리 미사용)

## 프로젝트 구조

```
src/
├── api.js                  # Axios 인스턴스 (JWT 인터셉터, 401 처리)
├── App.vue                 # 루트 (WebSocket 알림, 라우터뷰)
├── main.js                 # Vue 앱 엔트리
├── router.js               # 라우터 (인증 가드)
├── style.css               # 글로벌 CSS (디자인 시스템, 공통 컴포넌트)
├── composables/
│   ├── useAuth.js           # 토큰/역할 관리 (login, logout, getToken)
│   ├── useCart.js           # 장바구니 CRUD (localStorage 중앙화)
│   └── useFormatting.js     # formatPrice, formatDate, formatDateShort
├── constants/
│   └── index.js             # STATUS_MAP, STATUS_LABELS, categoryEmoji, REVIEW_KEYWORDS, QUICK_CHAT_QUERIES
├── components/
│   ├── ChatPanel.vue        # AI 추천 채팅 패널
│   ├── MenuDetailModal.vue  # 메뉴 상세 + 리뷰 모달
│   ├── OrderDetailModal.vue # 주문 상세 + 리뷰 작성 모달
│   ├── OrderList.vue        # 주문 목록 (재사용 컴포넌트)
│   └── ProfileModal.vue     # 프로필 (닉네임 변경, 내 리뷰)
└── views/
    ├── CartView.vue         # 장바구니
    ├── LoginView.vue        # 로그인
    ├── RegisterView.vue     # 회원가입 (랜덤 닉네임)
    ├── MenuView.vue         # 메인 메뉴 페이지
    ├── OrderDetailView.vue  # 주문 상세 (독립 페이지)
    └── OrderListView.vue    # 주문 내역 (독립 페이지)
```

## 페이지/라우트

| 경로 | 설명 |
|------|------|
| `/login` | 고객 로그인 |
| `/register` | 회원가입 (랜덤 닉네임 생성) |
| `/` | 메뉴 탐색 (사이드바 + 그리드 + AI 채팅) |
| `/cart` | 장바구니 (수량 조절, 주문하기) |
| `/orders` | 주문 내역 (페이지네이션) |
| `/orders/:id` | 주문 상세 (실시간 상태 업데이트) |

## 주요 기능
- 3컬럼 레이아웃: 카테고리 사이드바 + 메뉴 그리드 + AI 채팅 패널
- 메뉴 검색 (디바운스 400ms) / 카테고리 필터링
- 메뉴 카드: 이미지, BEST 뱃지, 품절, 예상 대기시간, 맵기 표시
- 카테고리 전환 시 fade-out → fade-in 애니메이션 (스켈레톤은 초기 로딩만)
- AI 추천 채팅: 빠른 추천 버튼 항상 표시
- 프로필: 닉네임 변경, 내 리뷰 목록
- 주문 상태 실시간 알림 (WebSocket, 우하단 토스트)
- 랜덤 닉네임 생성: 형용사 + 동물 조합

## Composables 패턴
- `useAuth()` — 토큰/역할 localStorage 접근 캡슐화. api.js, App.vue, Login/Register에서 사용
- `useCart()` — 장바구니 상태 관리. CartView, MenuView에서 사용
- `useFormatting()` — 가격/날짜 포맷. 전체 컴포넌트에서 사용

## 글로벌 CSS (style.css)
- 디자인 시스템: CSS Variables (Izakaya Counter 테마)
- 폰트: Playfair Display (제목), Noto Sans KR (본문), JetBrains Mono (가격)
- 공통 컴포넌트: `.modal-backdrop`, `.modal-x`, `.pill`, `.chip`, `.spinner`, `.fade-up`, `.alert-error`, `.mono`, `.display`
- 공통 트랜지션: `.modal-fade`

## 인증
- USER 역할만 접근 가능 (ADMIN 차단)
- JWT 토큰 localStorage 저장 (useAuth composable)
- 보호된 라우트 → 미인증 시 로그인 리다이렉트

## WebSocket
- App.vue에서 로그인 시 `/ws/orders/{userId}` 연결
- 주문 상태 변경 → 우하단 알림 팝업 (X 클릭까지 유지)
- 자동 재연결 (5초)
