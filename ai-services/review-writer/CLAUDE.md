# AI Review Writer Service

## 개요
AI 기반 리뷰 관리 서비스. 리뷰 초안 생성, 감정 분석/키워드 추출, 메뉴 임베딩용 요약을 제공한다.

## 기술 스택
- FastAPI + Uvicorn (Python)
- OpenAI GPT-4o-mini
- Pydantic

## 포트
- 8002

## API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/reviews/generate` | 메뉴명·키워드·별점으로 리뷰 초안 생성 (3-5문장) |
| POST | `/reviews/analyze` | 복수 리뷰 분석 (감정, 긍부정 비율, 키워드 Top5, 개선점) |
| POST | `/reviews/summarize` | 리뷰들을 메뉴 임베딩용 2-3문장으로 요약 |
| GET | `/health` | 헬스체크 |

## AI 프롬프트
- **생성**: 별점(1-5) 기반 3-5문장 리뷰 작성
- **분석**: 요약, 긍/부정 비율, 키워드 Top 5, 개선점 최대 3개
- **요약**: 메뉴 핵심 특성 중심 2-3문장 (임베딩 최적화)

## 환경변수
- `OPENAI_API_KEY`, `OPENAI_MODEL` (기본: gpt-4o-mini)
