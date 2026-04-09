# AI Recommendation Service

## 개요
RAG 기반 메뉴 추천 엔진. 자연어 대화와 키워드 검색으로 메뉴를 추천하며, ChromaDB 벡터 DB에 메뉴 임베딩을 관리한다.

## 기술 스택
- FastAPI + Uvicorn (Python)
- OpenAI GPT-4o-mini
- SentenceTransformers (`jhgan/ko-sroberta-multitask` — 한국어 임베딩 모델)
- ChromaDB (벡터 DB, 영속 저장: `/data/chroma`)
- httpx (비동기 HTTP)
- Pydantic

## 포트
- 8001

## API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/recommendations/chat` | 대화형 메뉴 추천 (자연어) |
| POST | `/api/recommendations/keyword` | 키워드 기반 메뉴 추천 |
| POST | `/embeddings/sync` | 단일 메뉴 임베딩 동기화 |
| POST | `/embeddings/bulk-sync` | 전체 메뉴 임베딩 벌크 동기화 |
| DELETE | `/embeddings/{menu_id}` | 메뉴 임베딩 삭제 |
| GET | `/health` | 헬스체크 |

## RAG 파이프라인
1. 사용자 쿼리 → 한국어 임베딩 모델로 벡터화
2. ChromaDB에서 유사 메뉴 Top-5 검색
3. 검색 결과를 컨텍스트로 GPT에 전달 → 최종 추천 생성
4. GPT 실패 시 직접 검색 결과로 폴백

## 시작 시 동작
- Menu Service에서 전체 메뉴 자동 동기화 (최대 10회 재시도, 30초 간격)

## 환경변수
- `OPENAI_API_KEY`, `OPENAI_MODEL` (기본: gpt-4o-mini)
- `MENU_SERVICE_URL`
