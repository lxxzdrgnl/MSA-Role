# Admin Web

## 개요
레스토랑 관리자용 웹 앱. 대시보드, 주문 관리, 메뉴/카테고리 CRUD, 매출 통계를 제공한다.

## 기술 스택
- Vue 3 (Composition API, `<script setup>`)
- Vue Router 4.3.0
- Axios 1.6.0
- Vite 5.0.0
- Pretendard Variable 폰트
- 커스텀 CSS (UI 라이브러리 미사용), CSS Variables 기반 디자인 토큰

## 디자인 시스템
- **테마**: 다크 모드 (customer-web과 동일한 Izakaya 톤)
- **액센트**: `#d4863c` (burnt orange)
- **컬러 체계**: 3톤 — accent(활성), neutral(완료), danger(취소/에러)
- **토큰**: `index.html` `:root`에 CSS 변수로 정의
- **글로벌 스타일**: `styles/global.css` — 버튼, 카드, 테이블, 폼, 알림, 페이지네이션 등

## 폴더 구조

```
src/
├── api.js                  API 클라이언트 (Axios, JWT 인터셉터)
├── App.vue                 레이아웃 + 사이드바 + WebSocket
├── main.js                 부트스트랩 + global.css import
├── router.js               라우트 정의 + 인증 가드
├── styles/
│   └── global.css          공통 유틸리티 CSS
├── utils/
│   ├── formatters.js       formatPrice, formatDate, timeAgo, summarizeItems, extractListData
│   └── status.js           STATUS_MAP, statusLabel, statusClass, nextStatus, congestionPercent 등
├── composables/
│   └── useAsync.js         loading/error 자동 관리 비동기 래퍼
├── components/
│   ├── ModalWrapper.vue    범용 모달 (Teleport, open/title/size props)
│   ├── AppPagination.vue   페이지네이션 (page/totalPages props)
│   ├── CategoryModal.vue   카테고리 CRUD 모달 (정렬/수정/삭제)
│   ├── RevenueChart.vue    7일 매출 SVG 라인차트
│   ├── StatusDonut.vue     주문 상태 분포 SVG 도넛차트
│   ├── StatusPill.vue      상태 뱃지 (pill-active/done/cancel)
│   └── EmptyState.vue      빈 상태 표시
└── views/
    ├── LoginView.vue       관리자 로그인
    ├── DashboardView.vue   실시간 통계 대시보드
    ├── OrdersView.vue      주문 관리 (신규주문 탭 + 주문목록 탭)
    └── MenusView.vue       메뉴/카테고리 관리
```

## 페이지/라우트

| 경로 | 뷰 | 설명 |
|------|-----|------|
| `/login` | LoginView | 관리자 로그인 (ADMIN 역할만 허용) |
| `/dashboard` | DashboardView | KPI 스트립, 매출 차트, 도넛 차트, 베스트셀러, 혼잡도 게이지, 활성 주문 피드 |
| `/orders` | OrdersView | 신규 주문 탭 (대기열 + 상세) / 주문 목록 탭 (상태필터, 날짜범위, 상태변경) |
| `/menus` | MenusView | 카테고리 탭 필터, 메뉴 테이블, 품절/베스트 토글, 메뉴 추가/수정 모달, 카테고리 관리 모달 |

## 주요 기능
- 대시보드 30초 자동 새로고침 + 카운트다운 표시
- 주문 상태 워크플로우: PENDING → ACCEPTED → COOKING → COMPLETED / CANCELLED
- WebSocket (`/ws/orders/admin`) 실시간 신규 주문 알림 + 사이드바 뱃지
- 메뉴 추가/수정은 모달 (별도 라우트 없음)
- 카테고리 관리: 추가, 수정, 삭제, 정렬 순서 변경
- SVG 차트: 7일 매출 추이 (라인/에어리어), 주문 상태 분포 (도넛)

## 인증
- ADMIN 역할만 접근 가능
- `api.js`에서 Axios 인터셉터로 토큰 자동 첨부
- 401 응답 시 자동 로그아웃 + 리다이렉트

## 공통 패턴
- **useAsync composable**: `const { loading, error, run } = useAsync()` → `await run(asyncFn, '에러메시지')`
- **extractListData**: API 응답이 배열이든 `{ content, totalPages }`이든 통일 처리
- **ModalWrapper**: 모든 모달은 `<ModalWrapper :open :title size @close>` 사용
- **AppPagination**: `<AppPagination :page :totalPages @change>` 사용

## nginx 프록시 (Docker)
- `/api/` → gateway:8080
- `/images/` → gateway:8080 (메뉴 이미지)
- `/ws/` → gateway:8080 (WebSocket)
