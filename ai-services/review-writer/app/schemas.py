from pydantic import BaseModel


class GenerateRequest(BaseModel):
    menu_name: str
    keywords: list[str]
    rating: int


class GenerateResponse(BaseModel):
    draft: str
    rating: int


class ReviewItem(BaseModel):
    text: str
    rating: int


class AnalyzeRequest(BaseModel):
    reviews: list[ReviewItem]


class AnalyzeResponse(BaseModel):
    summary: str
    positive_ratio: float
    negative_ratio: float
    top_keywords: list[str]
    improvement_points: list[str]


class SummarizeRequest(BaseModel):
    menu_name: str
    reviews: list[ReviewItem]


class SummarizeResponse(BaseModel):
    summary_text: str
