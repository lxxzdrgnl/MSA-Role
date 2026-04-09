# AI Validation Service

## 개요
이중 검증 서비스. 키워드 필터링 + GPT 의미 분석으로 사용자 입력 유효성 검사와 AI 출력 환각(hallucination) 탐지를 수행한다.

## 기술 스택
- FastAPI + Uvicorn (Python)
- OpenAI GPT-4o-mini
- Pydantic

## 포트
- 8004

## API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/validation/input` | 사용자 입력 검증 (금지어 + GPT 의미 분석) |
| POST | `/validation/output` | AI 출력 검증 (품질 + 환각 탐지) |
| GET | `/health` | 헬스체크 |

## 입력 검증 (2단계)
1. **1단계**: 금지어 키워드 필터 (13개 한국어 비속어/욕설, `/app/config/banned_keywords.txt`)
2. **2단계**: GPT 의미 분석 — 비속어, 혐오 발언, 무관한 내용, 개인정보 유출 탐지

## 출력 검증
- AI 응답의 적절성, 환각(존재하지 않는 메뉴 추천) 여부, 포맷 확인
- 사용 가능한 메뉴 ID 목록과 대화 컨텍스트를 받아 문맥 기반 검증

## 장애 대응
- 검증 실패 시 `is_valid=true`로 기본 반환 (서비스 가용성 우선)

## 환경변수
- `OPENAI_API_KEY`, `OPENAI_MODEL` (기본: gpt-4o-mini)
