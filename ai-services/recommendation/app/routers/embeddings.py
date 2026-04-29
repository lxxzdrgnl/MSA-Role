from fastapi import APIRouter

from app.errors import ERROR_RESPONSES
from app.schemas import EmbeddingSyncRequest, BulkSyncRequest
from app.services import embedding_service

router = APIRouter(prefix="/embeddings", tags=["embeddings"])


@router.post("/sync", responses=ERROR_RESPONSES)
async def sync_embedding(request: EmbeddingSyncRequest):
    """단일 메뉴 임베딩 동기화"""
    embedding_service.sync_menu(request)
    return {"status": "ok", "menu_id": request.menu_id}


@router.post("/bulk-sync", responses=ERROR_RESPONSES)
async def bulk_sync_embeddings(request: BulkSyncRequest):
    """전체 메뉴 임베딩 벌크 동기화"""
    embedding_service.bulk_sync(request.menus)
    return {"status": "ok", "count": len(request.menus)}


@router.delete("/{menu_id}", responses=ERROR_RESPONSES)
async def delete_embedding(menu_id: int):
    """메뉴 임베딩 삭제"""
    embedding_service.delete_menu(menu_id)
    return {"status": "ok", "menu_id": menu_id}
