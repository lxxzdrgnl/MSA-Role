from fastapi import APIRouter

from app.schemas import EmbeddingSyncRequest, BulkSyncRequest
from app.services import embedding_service

router = APIRouter(prefix="/embeddings", tags=["embeddings"])


@router.post("/sync")
async def sync_embedding(request: EmbeddingSyncRequest):
    """Sync a single menu item embedding."""
    embedding_service.sync_menu(request)
    return {"status": "ok", "menu_id": request.menu_id}


@router.post("/bulk-sync")
async def bulk_sync_embeddings(request: BulkSyncRequest):
    """Bulk sync menu embeddings."""
    embedding_service.bulk_sync(request.menus)
    return {"status": "ok", "count": len(request.menus)}


@router.delete("/{menu_id}")
async def delete_embedding(menu_id: int):
    """Delete a menu embedding."""
    embedding_service.delete_menu(menu_id)
    return {"status": "ok", "menu_id": menu_id}
