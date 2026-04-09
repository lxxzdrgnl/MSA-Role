# Order Service

## 개요
주문 생성·상태 관리·매출 통계·베스트셀러 분석을 담당하는 마이크로서비스. Menu Service로 메뉴 검증, Gateway WebSocket으로 실시간 알림을 제공한다.

## 기술 스택
- Spring Boot 3.2.5 / Java 17 / Gradle
- SQLite (파일 DB: `/data/order.db`)
- Spring JDBC (JdbcTemplate)
- Spring Scheduler (베스트셀러 갱신 크론)
- SpringDoc OpenAPI 2.4.0

## 포트
- 8083

## API 엔드포인트

### 공개 API (`/api/orders`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/orders` | 주문 생성 | X-User-Id |
| GET | `/api/orders` | 내 주문 목록 (페이지네이션) | X-User-Id |
| GET | `/api/orders/all` | 전체 주문 목록 (페이지네이션) | ADMIN |
| GET | `/api/orders/{id}` | 주문 상세 | - |
| GET | `/api/orders/active` | 활성 주문 (PENDING/COOKING) | - |
| PATCH | `/api/orders/{id}/status` | 주문 상태 변경 | ADMIN |
| GET | `/api/orders/stats/revenue` | 매출 통계 (일/주/월) | - |
| GET | `/api/orders/stats/best-sellers` | 베스트셀러 메뉴 | - |

### 내부 API (`/internal/orders`)
- GET `/internal/orders/history` — 최근 N일 주문 이력 (AI Operations용)
- GET `/internal/orders/active-count` — 활성 주문 수 (PENDING + COOKING)

## 데이터 모델

### orders 테이블
- `id`, `userId`, `status`, `totalPrice`, `createdAt`, `updatedAt`

### order_items 테이블
- `id`, `orderId` (FK), `menuId`, `menuName`, `price`, `quantity`

## 주문 상태 흐름
```
PENDING → ACCEPTED → COOKING → READY → COMPLETED
  ↓         ↓          ↓        ↓
         CANCELLED (어느 단계에서든 취소 가능)
```

## 주문 생성 플로우
1. Menu Service에서 각 메뉴 검증 (품절 체크)
2. Order + OrderItem 저장
3. Gateway WebSocket으로 ORDER_UPDATE 알림 전송

## 스케줄러
- 매일 자정: 최근 7일 기준 TOP 10 베스트셀러 계산 → Menu Service에 플래그 업데이트

## 환경변수
- `MENU_SERVICE_URL` (기본: http://localhost:8082)
- `GATEWAY_URL` (기본: http://localhost:8080)
