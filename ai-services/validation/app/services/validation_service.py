import json
import logging
from pathlib import Path

from openai import OpenAI

from app.config import settings
from app.schemas import (
    InputValidationRequest,
    OutputValidationRequest,
    ValidationResponse,
)

logger = logging.getLogger(__name__)

client = OpenAI(api_key=settings.OPENAI_API_KEY)

# Load banned keywords
BANNED_KEYWORDS: list[str] = []
banned_keywords_path = Path(__file__).parent.parent / "config" / "banned_keywords.txt"
try:
    with open(banned_keywords_path, "r", encoding="utf-8") as f:
        BANNED_KEYWORDS = [
            line.strip() for line in f if line.strip() and not line.startswith("#")
        ]
    logger.info(f"Loaded {len(BANNED_KEYWORDS)} banned keywords")
except FileNotFoundError:
    logger.warning(f"Banned keywords file not found: {banned_keywords_path}")


def _check_banned_keywords(text: str) -> tuple[bool, str]:
    """First pass: check for banned keywords (no GPT call)."""
    text_lower = text.lower()
    for keyword in BANNED_KEYWORDS:
        if keyword.lower() in text_lower:
            return False, f"금지된 표현이 포함되어 있습니다: {keyword}"
    return True, ""


def _gpt_validate_input(text: str, context: str) -> tuple[bool, str]:
    """Second pass: GPT-based input validation."""
    system_prompt = (
        "너는 식당 주문 시스템의 입력 검증기다. "
        "사용자의 입력이 다음 기준에 부합하는지 판단해라:\n"
        "1. 욕설, 비속어, 혐오 표현이 없어야 한다\n"
        "2. 식당/음식/주문과 관련 없는 부적절한 내용이 아니어야 한다\n"
        "3. 개인정보(전화번호, 주소 등)가 포함되지 않아야 한다\n\n"
        "반드시 아래 JSON 형식으로 응답해라:\n"
        '{"is_valid": true/false, "reason": "판단 이유"}'
    )

    user_content = f"입력: {text}"
    if context:
        user_content += f"\n컨텍스트: {context}"

    response = client.chat.completions.create(
        model=settings.OPENAI_MODEL,
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_content},
        ],
        temperature=0.0,
        max_tokens=200,
    )

    content = response.choices[0].message.content.strip()
    if content.startswith("```"):
        lines = content.split("\n")
        content = "\n".join(lines[1:-1]) if len(lines) > 2 else content

    data = json.loads(content)
    return data.get("is_valid", True), data.get("reason", "")


def validate_input(request: InputValidationRequest) -> ValidationResponse:
    """Validate user input: banned keyword check + GPT validation."""
    try:
        # 1st pass: banned keyword filter
        is_valid, reason = _check_banned_keywords(request.text)
        if not is_valid:
            return ValidationResponse(is_valid=False, reason=reason)

        # 2nd pass: GPT validation
        is_valid, reason = _gpt_validate_input(request.text, request.context or "")
        return ValidationResponse(is_valid=is_valid, reason=reason)

    except Exception as e:
        # Fallback: on ANY failure, return is_valid=true
        logger.error(f"Input validation failed: {e}")
        return ValidationResponse(is_valid=True, reason="검증 서비스 오류로 통과 처리됨")


def validate_output(request: OutputValidationRequest) -> ValidationResponse:
    """Validate AI output for quality and hallucination."""
    try:
        system_prompt = (
            "너는 식당 주문 시스템의 AI 출력 검증기다. "
            "AI의 응답이 다음 기준에 부합하는지 판단해라:\n"
            "1. 사용자의 질문/요청에 적절한 응답인가\n"
            "2. 환각(hallucination)이 없는가 - 존재하지 않는 메뉴를 추천하거나 거짓 정보를 포함하지 않는가\n"
            "3. 부적절한 내용이 포함되지 않았는가\n\n"
            "반드시 아래 JSON 형식으로 응답해라:\n"
            '{"is_valid": true/false, "reason": "판단 이유"}'
        )

        user_content = f"사용자 입력: {request.user_input}\nAI 응답: {request.ai_response}"
        if request.context:
            user_content += f"\n컨텍스트: {request.context}"
        if request.available_menu_ids:
            user_content += (
                f"\n사용 가능한 메뉴 ID: {request.available_menu_ids}"
            )

        response = client.chat.completions.create(
            model=settings.OPENAI_MODEL,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_content},
            ],
            temperature=0.0,
            max_tokens=200,
        )

        content = response.choices[0].message.content.strip()
        if content.startswith("```"):
            lines = content.split("\n")
            content = "\n".join(lines[1:-1]) if len(lines) > 2 else content

        data = json.loads(content)
        return ValidationResponse(
            is_valid=data.get("is_valid", True),
            reason=data.get("reason", ""),
        )

    except Exception as e:
        # Fallback: on ANY failure, return is_valid=true
        logger.error(f"Output validation failed: {e}")
        return ValidationResponse(is_valid=True, reason="검증 서비스 오류로 통과 처리됨")
