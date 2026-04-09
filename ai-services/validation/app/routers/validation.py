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
    """Validate user input (banned keywords + GPT check)."""
    return validation_service.validate_input(request)


@router.post("/output", response_model=ValidationResponse, responses=ERROR_RESPONSES)
async def validate_output(request: OutputValidationRequest):
    """Validate AI output (quality + hallucination check)."""
    return validation_service.validate_output(request)
