import asyncio
import logging

import httpx
from fastapi import FastAPI

from app.config import settings
from app.routers import recommendations, embeddings
from app.schemas import EmbeddingSyncRequest
from app.services import embedding_service

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="AI Recommendation Service",
    description="RAG 기반 메뉴 추천 서비스",
    version="1.0.0",
)

app.include_router(recommendations.router)
app.include_router(embeddings.router)


@app.get("/health")
async def health_check():
    return {"status": "ok"}


@app.on_event("startup")
async def startup_event():
    """Bulk sync menus from Menu Service on startup."""
    max_retries = 10
    retry_interval = 30  # seconds

    for attempt in range(1, max_retries + 1):
        try:
            logger.info(
                f"Fetching menus from Menu Service (attempt {attempt}/{max_retries})..."
            )
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(
                    f"{settings.MENU_SERVICE_URL}/internal/menus"
                )
                response.raise_for_status()
                menus_data = response.json()

            if not menus_data:
                logger.info("No menus found in Menu Service")
                return

            menus = []
            for m in menus_data:
                menus.append(
                    EmbeddingSyncRequest(
                        menu_id=m.get("id", 0),
                        name=m.get("name", ""),
                        description=m.get("description", ""),
                        tags=m.get("tags", ""),
                        allergens=m.get("allergens", ""),
                        spicy_level=m.get("spicyLevel", m.get("spicy_level", 0)),
                        review_summary=m.get("reviewSummary", m.get("review_summary", "")),
                        avg_rating=m.get("avgRating", m.get("avg_rating", 0.0)),
                        order_count=m.get("orderCount", m.get("order_count", 0)),
                    )
                )

            embedding_service.bulk_sync(menus)
            logger.info(f"Successfully synced {len(menus)} menus on startup")
            return

        except Exception as e:
            logger.warning(
                f"Failed to fetch menus (attempt {attempt}/{max_retries}): {e}"
            )
            if attempt < max_retries:
                logger.info(f"Retrying in {retry_interval} seconds...")
                await asyncio.sleep(retry_interval)

    logger.error(
        "Failed to sync menus from Menu Service after all retries. "
        "Service will start without initial embeddings."
    )
