import logging
import math

import httpx

from app.config import settings
from app.schemas import CongestionResponse, MenuCongestionResponse

logger = logging.getLogger(__name__)


def _get_congestion_level(active_count: int) -> str:
    """Determine congestion level based on active order count."""
    if active_count <= settings.CONGESTION_LOW:
        return "여유"
    elif active_count <= settings.CONGESTION_HIGH:
        return "보통"
    else:
        return "혼잡"


def _get_congestion_factor(level: str) -> float:
    """Get time multiplication factor based on congestion level."""
    factors = {
        "여유": 1.0,
        "보통": 1.5,
        "혼잡": 2.0,
    }
    return factors.get(level, 1.0)


def _estimate_wait_minutes(active_count: int, level: str) -> int:
    """Estimate wait time in minutes based on active orders and congestion."""
    base_minutes = active_count * 3
    factor = _get_congestion_factor(level)
    return math.ceil(base_minutes * factor)


async def get_congestion() -> CongestionResponse:
    """Get current congestion status from Order Service."""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{settings.ORDER_SERVICE_URL}/internal/orders/active-count"
            )
            response.raise_for_status()
            data = response.json()

        active_count = data.get("activeCount", 0)
        level = _get_congestion_level(active_count)

        return CongestionResponse(
            level=level,
            active_orders=active_count,
            estimated_wait_minutes=_estimate_wait_minutes(active_count, level),
        )

    except Exception as e:
        logger.error(f"Failed to get congestion data: {e}")
        return CongestionResponse(
            level="알 수 없음",
            active_orders=0,
            estimated_wait_minutes=0,
        )


async def get_menu_congestion(menu_id: int) -> MenuCongestionResponse:
    """Get estimated time for a specific menu item considering congestion."""
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            # Get menu info
            menu_response = await client.get(
                f"{settings.MENU_SERVICE_URL}/internal/menus/{menu_id}"
            )
            menu_response.raise_for_status()
            menu_data = menu_response.json()

            # Get current congestion
            order_response = await client.get(
                f"{settings.ORDER_SERVICE_URL}/internal/orders/active-count"
            )
            order_response.raise_for_status()
            order_data = order_response.json()

        active_count = order_data.get("activeCount", 0)
        level = _get_congestion_level(active_count)
        factor = _get_congestion_factor(level)

        cook_time = menu_data.get("cookTimeMinutes", menu_data.get("cook_time_minutes", 15))
        estimated = math.ceil(cook_time * factor)

        return MenuCongestionResponse(
            menu_id=menu_id,
            menu_name=menu_data.get("name", ""),
            estimated_minutes=estimated,
        )

    except Exception as e:
        logger.error(f"Failed to get menu congestion for menu {menu_id}: {e}")
        return MenuCongestionResponse(
            menu_id=menu_id,
            menu_name="",
            estimated_minutes=0,
        )
