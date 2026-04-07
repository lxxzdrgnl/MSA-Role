import json
import logging

from openai import OpenAI

from app.config import settings
from app.schemas import (
    NewMenuSuggestRequest,
    NewMenuResponse,
    QualityAnalysisRequest,
    QualityResponse,
)

logger = logging.getLogger(__name__)

client = OpenAI(api_key=settings.OPENAI_API_KEY)

NEW_MENU_SYSTEM_PROMPT = (
    "너는 식당 메뉴 기획 컨설턴트다. "
    "주문 히스토리와 현재 메뉴를 분석해서 새로운 메뉴 3개를 추천해라. "
    "각 메뉴에 대해 이름, 예상 가격, 추천 이유를 제시해라.\n\n"
    "반드시 아래 JSON 형식으로 응답해라:\n"
    '{"suggestions": [{"name": "메뉴명", "price": 10000, "reason": "추천 이유"}], '
    '"analysis": "전체 분석 요약"}'
)

QUALITY_SYSTEM_PROMPT = (
    "너는 식당 서비스 품질 분석 전문가다. "
    "리뷰와 주문 데이터를 종합 분석해서 강점, 약점, 개선 제안을 제시해라.\n\n"
    "반드시 아래 JSON 형식으로 응답해라:\n"
    '{"overall_score": 4.2, "strengths": ["강점1"], '
    '"weaknesses": ["약점1"], "recommendations": ["제안1"]}'
)


def suggest_new_menu(request: NewMenuSuggestRequest) -> NewMenuResponse:
    """Suggest new menu items using GPT."""
    try:
        response = client.chat.completions.create(
            model=settings.OPENAI_MODEL,
            messages=[
                {"role": "system", "content": NEW_MENU_SYSTEM_PROMPT},
                {
                    "role": "user",
                    "content": (
                        f"주문 히스토리:\n{json.dumps(request.order_history, ensure_ascii=False)}\n\n"
                        f"현재 메뉴:\n{json.dumps(request.current_menus, ensure_ascii=False)}"
                    ),
                },
            ],
            temperature=0.7,
            max_tokens=1000,
        )

        content = response.choices[0].message.content.strip()
        if content.startswith("```"):
            lines = content.split("\n")
            content = "\n".join(lines[1:-1]) if len(lines) > 2 else content

        data = json.loads(content)
        return NewMenuResponse(
            suggestions=data.get("suggestions", []),
            analysis=data.get("analysis", ""),
        )

    except Exception as e:
        logger.error(f"New menu suggestion failed: {e}")
        return NewMenuResponse(
            suggestions=[],
            analysis="신메뉴 추천 서비스를 일시적으로 이용할 수 없습니다.",
        )


def analyze_quality(request: QualityAnalysisRequest) -> QualityResponse:
    """Analyze service quality using GPT."""
    try:
        response = client.chat.completions.create(
            model=settings.OPENAI_MODEL,
            messages=[
                {"role": "system", "content": QUALITY_SYSTEM_PROMPT},
                {
                    "role": "user",
                    "content": (
                        f"리뷰 데이터:\n{json.dumps(request.reviews, ensure_ascii=False)}\n\n"
                        f"주문 통계:\n{json.dumps(request.order_stats, ensure_ascii=False)}"
                    ),
                },
            ],
            temperature=0.3,
            max_tokens=1000,
        )

        content = response.choices[0].message.content.strip()
        if content.startswith("```"):
            lines = content.split("\n")
            content = "\n".join(lines[1:-1]) if len(lines) > 2 else content

        data = json.loads(content)
        return QualityResponse(
            overall_score=data.get("overall_score", 0.0),
            strengths=data.get("strengths", []),
            weaknesses=data.get("weaknesses", []),
            recommendations=data.get("recommendations", []),
        )

    except Exception as e:
        logger.error(f"Quality analysis failed: {e}")
        return QualityResponse(
            overall_score=0.0,
            strengths=[],
            weaknesses=[],
            recommendations=["품질 분석 서비스를 일시적으로 이용할 수 없습니다."],
        )
