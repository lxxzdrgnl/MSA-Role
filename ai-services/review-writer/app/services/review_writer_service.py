import json
import logging

from openai import OpenAI

from app.config import settings
from app.schemas import (
    GenerateRequest,
    GenerateResponse,
    AnalyzeRequest,
    AnalyzeResponse,
    SummarizeRequest,
    SummarizeResponse,
)

logger = logging.getLogger(__name__)

client = OpenAI(api_key=settings.OPENAI_API_KEY)

GENERATE_SYSTEM_PROMPT = (
    "너는 식당 후기 작성 도우미다. "
    "사용자가 선택한 키워드를 바탕으로 자연스러운 후기를 3~5문장으로 작성해라. "
    "가급적이면 긍정적인 평가를 내려주면 좋다. "
    "별점은 1-5점 정수로 평가한다."
)

ANALYZE_SYSTEM_PROMPT = (
    "너는 식당 리뷰 분석가다. "
    "아래 리뷰들을 분석해서 JSON으로 반환: "
    "summary, positive_ratio, negative_ratio, top_keywords(max5), improvement_points(max3)\n\n"
    "반드시 아래 JSON 형식으로 응답해라:\n"
    '{"summary": "요약", "positive_ratio": 0.8, "negative_ratio": 0.2, '
    '"top_keywords": ["키워드1"], "improvement_points": ["개선점1"]}'
)

SUMMARIZE_SYSTEM_PROMPT = (
    "너는 식당 리뷰 요약 전문가다. "
    "아래 리뷰들을 2~3문장으로 요약해라. "
    "이 요약은 메뉴 임베딩에 사용되므로 핵심 특징을 간결하게 담아라."
)


def generate_review(request: GenerateRequest) -> GenerateResponse:
    """Generate a review draft using GPT."""
    rating = max(1, min(5, request.rating))

    try:
        user_content = (
            f"메뉴: {request.menu_name}\n"
            f"키워드: {', '.join(request.keywords)}\n"
            f"별점: {rating}점"
        )
        if request.feedback:
            user_content += f"\n\n[이전 생성 결과에 대한 피드백]\n{request.feedback}\n위 피드백을 반영하여 다시 작성해주세요."

        response = client.chat.completions.create(
            model=settings.OPENAI_MODEL,
            messages=[
                {"role": "system", "content": GENERATE_SYSTEM_PROMPT},
                {"role": "user", "content": user_content},
            ],
            temperature=0.7,
            max_tokens=500,
        )

        draft = response.choices[0].message.content.strip()
        return GenerateResponse(draft=draft, rating=rating)

    except Exception as e:
        logger.error(f"Review generation failed: {e}")
        return GenerateResponse(draft="맛있게 잘 먹었습니다.", rating=rating)


def analyze_reviews(request: AnalyzeRequest) -> AnalyzeResponse:
    """Analyze reviews using GPT."""
    try:
        reviews_text = "\n".join(
            [f"- [{r.rating}점] {r.text}" for r in request.reviews]
        )

        response = client.chat.completions.create(
            model=settings.OPENAI_MODEL,
            messages=[
                {"role": "system", "content": ANALYZE_SYSTEM_PROMPT},
                {
                    "role": "user",
                    "content": f"리뷰 목록:\n{reviews_text}",
                },
            ],
            temperature=0.3,
            max_tokens=800,
        )

        content = response.choices[0].message.content.strip()
        if content.startswith("```"):
            lines = content.split("\n")
            content = "\n".join(lines[1:-1]) if len(lines) > 2 else content

        data = json.loads(content)
        return AnalyzeResponse(
            summary=data.get("summary", ""),
            positive_ratio=data.get("positive_ratio", 0.0),
            negative_ratio=data.get("negative_ratio", 0.0),
            top_keywords=data.get("top_keywords", []),
            improvement_points=data.get("improvement_points", []),
        )

    except Exception as e:
        logger.error(f"Review analysis failed: {e}")
        return AnalyzeResponse(
            summary="분석을 일시적으로 이용할 수 없습니다.",
            positive_ratio=0.0,
            negative_ratio=0.0,
            top_keywords=[],
            improvement_points=[],
        )


def summarize_reviews(request: SummarizeRequest) -> SummarizeResponse:
    """Summarize reviews for embedding."""
    try:
        reviews_text = "\n".join(
            [f"- [{r.rating}점] {r.text}" for r in request.reviews]
        )

        response = client.chat.completions.create(
            model=settings.OPENAI_MODEL,
            messages=[
                {"role": "system", "content": SUMMARIZE_SYSTEM_PROMPT},
                {
                    "role": "user",
                    "content": (
                        f"메뉴: {request.menu_name}\n\n"
                        f"리뷰 목록:\n{reviews_text}"
                    ),
                },
            ],
            temperature=0.3,
            max_tokens=300,
        )

        summary_text = response.choices[0].message.content.strip()
        return SummarizeResponse(summary_text=summary_text)

    except Exception as e:
        logger.error(f"Review summarization failed: {e}")
        return SummarizeResponse(summary_text="")
