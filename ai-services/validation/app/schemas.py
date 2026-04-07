from typing import List, Optional

from pydantic import BaseModel


class InputValidationRequest(BaseModel):
    text: str
    context: Optional[str] = ""


class OutputValidationRequest(BaseModel):
    user_input: str
    ai_response: str
    context: Optional[str] = ""
    available_menu_ids: Optional[List[int]] = []


class ValidationResponse(BaseModel):
    is_valid: bool
    reason: str = ""
