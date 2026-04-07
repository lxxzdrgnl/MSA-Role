import json
import logging
from typing import List

from openai import OpenAI

from app.config import settings
from app.schemas import RecommendationResponse
from app.services import embedding_service

logger = logging.getLogger(__name__)

client = OpenAI(api_key=settings.OPENAI_API_KEY)

SYSTEM_PROMPT = (
    "너는 식당 메뉴 추천 도우미다. "
    "아래 메뉴 후보 중에서 사용자의 요청에 가장 적합한 메뉴를 1~3개 추천해라. "
    "추천 이유를 간결하게 설명해라. "
    "메뉴 후보에 없는 메뉴는 추천하지 마라.\n\n"
    "반드시 아래 JSON 형식으로 응답해라:\n"
    '{"menu_ids": [1, 2], "reason": "추천 이유", "keywords": ["키워드1", "키워드2"]}'
)

FALLBACK_RESPONSE = RecommendationResponse(
    menu_ids=[],
    reason="추천 서비스를 일시적으로 이용할 수 없습니다.",
    keywords=[],
)


def _build_context(search_results: list) -> str:
    """Build menu context string from ChromaDB search results."""
    lines = []
    for item in search_results:
        lines.append(f"- [메뉴 ID: {item['menu_id']}] {item['document']}")
    return "\n".join(lines)


def _parse_gpt_response(content: str) -> RecommendationResponse:
    """Parse GPT response JSON into RecommendationResponse."""
    # Try to extract JSON from the response
    content = content.strip()
    if content.startswith("```"):
        # Remove markdown code block
        lines = content.split("\n")
        content = "\n".join(lines[1:-1]) if len(lines) > 2 else content

    data = json.loads(content)
    return RecommendationResponse(
        menu_ids=data.get("menu_ids", []),
        reason=data.get("reason", ""),
        keywords=data.get("keywords", []),
    )


def _fallback_from_search(search_results: list, message: str) -> RecommendationResponse:
    """Build recommendation directly from ChromaDB results (no GPT needed)."""
    menu_ids = [item["menu_id"] for item in search_results[:3]]
    names = [item["metadata"].get("name", "") for item in search_results[:3]]
    return RecommendationResponse(
        menu_ids=menu_ids,
        reason=f"{', '.join(names)}을(를) 추천합니다.",
        keywords=[w for w in message.split() if len(w) > 1][:3],
    )


def chat_recommend(message: str) -> RecommendationResponse:
    """Chat-based menu recommendation using RAG."""
    try:
        # Search ChromaDB for relevant menus
        search_results = embedding_service.search(message, top_k=5)

        if not search_results:
            return RecommendationResponse(
                menu_ids=[],
                reason="등록된 메뉴가 없습니다.",
                keywords=[],
            )

        context = _build_context(search_results)

        try:
            response = client.chat.completions.create(
                model=settings.OPENAI_MODEL,
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {
                        "role": "user",
                        "content": f"메뉴 후보:\n{context}\n\n사용자: {message}",
                    },
                ],
                temperature=0.7,
                max_tokens=500,
            )
            return _parse_gpt_response(response.choices[0].message.content)
        except Exception as e:
            logger.warn(f"GPT call failed, using search fallback: {e}")
            return _fallback_from_search(search_results, message)

    except Exception as e:
        logger.error(f"Chat recommendation failed: {e}")
        return FALLBACK_RESPONSE


def keyword_recommend(keywords: List[str]) -> RecommendationResponse:
    """Keyword-based menu recommendation using RAG."""
    try:
        query = " ".join(keywords)

        # Search ChromaDB for relevant menus
        search_results = embedding_service.search(query, top_k=5)

        if not search_results:
            return RecommendationResponse(
                menu_ids=[],
                reason="등록된 메뉴가 없습니다.",
                keywords=keywords,
            )

        context = _build_context(search_results)

        try:
            response = client.chat.completions.create(
                model=settings.OPENAI_MODEL,
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {
                        "role": "user",
                        "content": f"메뉴 후보:\n{context}\n\n사용자 키워드: {', '.join(keywords)}",
                    },
                ],
                temperature=0.7,
                max_tokens=500,
            )
            result = _parse_gpt_response(response.choices[0].message.content)
            result.keywords = keywords
            return result
        except Exception as e:
            logger.warn(f"GPT call failed, using search fallback: {e}")
            result = _fallback_from_search(search_results, " ".join(keywords))
            result.keywords = keywords
            return result

    except Exception as e:
        logger.error(f"Keyword recommendation failed: {e}")
        return FALLBACK_RESPONSE
