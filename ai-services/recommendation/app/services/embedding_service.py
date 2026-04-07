import logging
from typing import List, Dict, Any

import chromadb
from sentence_transformers import SentenceTransformer

from app.config import settings
from app.schemas import EmbeddingSyncRequest

logger = logging.getLogger(__name__)

# Initialize ChromaDB persistent client
chroma_client = chromadb.PersistentClient(path=settings.CHROMA_PERSIST_PATH)
collection = chroma_client.get_or_create_collection(
    name="menu_embeddings",
    metadata={"hnsw:space": "cosine"},
)

# Initialize sentence-transformers model
embedding_model = SentenceTransformer(settings.EMBEDDING_MODEL)


def _build_embedding_text(menu: EmbeddingSyncRequest) -> str:
    """Build text representation of a menu item for embedding."""
    parts = [
        menu.category or "",
        menu.name,
        menu.description or "",
        menu.tags or "",
        menu.allergens or "",
        f"맵기{menu.spicy_level}" if menu.spicy_level else "",
        f"평점 {menu.avg_rating}" if menu.avg_rating else "",
        f"주문수 {menu.order_count}" if menu.order_count else "",
        f"리뷰요약: {menu.review_summary}" if menu.review_summary else "",
    ]
    return " | ".join(p for p in parts if p)


def sync_menu(menu: EmbeddingSyncRequest) -> None:
    """Sync a single menu item embedding to ChromaDB."""
    text = _build_embedding_text(menu)
    embedding = embedding_model.encode(text).tolist()

    collection.upsert(
        ids=[str(menu.menu_id)],
        embeddings=[embedding],
        documents=[text],
        metadatas=[{
            "menu_id": menu.menu_id,
            "name": menu.name,
            "tags": menu.tags or "",
            "spicy_level": menu.spicy_level or 0,
            "avg_rating": menu.avg_rating or 0.0,
            "order_count": menu.order_count or 0,
        }],
    )
    logger.info(f"Synced embedding for menu {menu.menu_id}: {menu.name}")


def bulk_sync(menus: List[EmbeddingSyncRequest]) -> None:
    """Bulk sync menu embeddings to ChromaDB."""
    if not menus:
        logger.info("No menus to sync")
        return

    texts = [_build_embedding_text(m) for m in menus]
    embeddings = embedding_model.encode(texts).tolist()

    collection.upsert(
        ids=[str(m.menu_id) for m in menus],
        embeddings=embeddings,
        documents=texts,
        metadatas=[{
            "menu_id": m.menu_id,
            "name": m.name,
            "tags": m.tags or "",
            "spicy_level": m.spicy_level or 0,
            "avg_rating": m.avg_rating or 0.0,
            "order_count": m.order_count or 0,
        } for m in menus],
    )
    logger.info(f"Bulk synced {len(menus)} menu embeddings")


def delete_menu(menu_id: int) -> None:
    """Delete a menu embedding from ChromaDB."""
    try:
        collection.delete(ids=[str(menu_id)])
        logger.info(f"Deleted embedding for menu {menu_id}")
    except Exception as e:
        logger.warning(f"Failed to delete embedding for menu {menu_id}: {e}")


def search(query: str, top_k: int = 5) -> List[Dict[str, Any]]:
    """Search for similar menu items using query text."""
    query_embedding = embedding_model.encode(query).tolist()

    results = collection.query(
        query_embeddings=[query_embedding],
        n_results=top_k,
        include=["documents", "metadatas", "distances"],
    )

    items = []
    if results and results["ids"] and results["ids"][0]:
        for i, menu_id in enumerate(results["ids"][0]):
            items.append({
                "menu_id": int(menu_id),
                "document": results["documents"][0][i],
                "metadata": results["metadatas"][0][i],
                "distance": results["distances"][0][i],
            })

    return items
