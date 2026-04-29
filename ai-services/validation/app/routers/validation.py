from fastapi import APIRouter

from app.errors import ERROR_RESPONSES
from app.schemas import (
    InputValidationRequest,
    OutputValidationRequest,
    ValidationResponse,
)
from app.services import validation_service

router = APIRouter(prefix="/validation", tags=["validation"])


@router.post("/input", response_model=ValidationResponse, responses=ERROR_RESPONSES)
async def validate_input(request: InputValidationRequest):
    """사용자 입력 검증 (금지어 필터 + GPT 의미 분석)"""
    return validation_service.validate_input(request)


@router.post("/output", response_model=ValidationResponse, responses=ERROR_RESPONSES)
async def validate_output(request: OutputValidationRequest):
    """AI 출력 검증 (품질 + 환각 탐지)"""
    return validation_service.validate_output(request)
