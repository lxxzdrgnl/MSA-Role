# AI Operations Service

## 개요
AI 기반 레스토랑 운영 인텔리전스 서비스. 실시간 혼잡도 모니터링, 신메뉴 제안, 서비스 품질 분석을 제공한다.

## 기술 스택
- FastAPI + Uvicorn (Python)
- OpenAI GPT-4o-mini
- httpx (비동기 HTTP)
- Pydantic

## 포트
- 8003

## API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/operations/congestion` | 현재 혼잡도 상태 조회 |
| GET | `/api/operations/congestion/menu/{menu_id}` | 특정 메뉴 예상 조리 시간 |
| POST | `/api/operations/new-menu-suggest` | 주문 이력 기반 신메뉴 3개 제안 (GPT) |
| POST | `/api/operations/quality-analysis` | 리뷰+주문 데이터 기반 서비스 품질 분석 (GPT) |
| GET | `/health` | 헬스체크 |

## AI 기능
- **신메뉴 제안**: 주문 이력 분석 → 메뉴명, 가격, 이유 포함 3개 제안
- **품질 분석**: 리뷰+주문 통계 → 강점, 약점, 개선 권고안

## 연동 서비스
- Order Service (활성 주문 수, 주문 이력)
- Menu Service (메뉴 상세)

## 환경변수
- `OPENAI_API_KEY`, `OPENAI_MODEL`
- `ORDER_SERVICE_URL`, `MENU_SERVICE_URL`
- 혼잡도 임계값 설정
