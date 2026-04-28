<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="mdm">
      <button class="mdm-close" @click="$emit('close')">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M3 3l8 8m0-8-8 8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
      </button>

      <div class="mdm-layout">
        <!-- LEFT: Menu Info -->
        <div class="mdm-left">
          <div class="mdm-visual">
            <img v-if="menu.imageUrl" :src="menu.imageUrl" :alt="menu.name" class="mdm-img" />
            <div v-else class="mdm-img-empty"><span>{{ emoji }}</span></div>
            <div class="mdm-img-fade"></div>
            <div class="mdm-badges">
              <span v-if="menu.isBest" class="mdm-badge mdm-badge-best">BEST</span>
              <span v-if="menu.isSoldOut" class="mdm-badge mdm-badge-sold">품절</span>
            </div>
          </div>

          <div class="mdm-info">
            <div class="mdm-cat">{{ menu.categoryName }}</div>
            <h2 class="mdm-name">{{ menu.name }}</h2>
            <p v-if="menu.description" class="mdm-desc">{{ menu.description }}</p>

            <div class="mdm-chips">
              <span v-if="menu.spicyLevel > 0" class="mdm-chip">🌶 맵기 {{ menu.spicyLevel }}</span>
              <span v-if="menu.cookTimeMinutes" class="mdm-chip">⏱ {{ menu.cookTimeMinutes }}분</span>
              <span v-if="menu.allergens" class="mdm-chip">⚠ {{ menu.allergens }}</span>
              <span v-for="tag in (menu.tags || '').split(',').filter(t => t.trim())" :key="tag" class="mdm-chip mdm-chip-tag">{{ tag.trim() }}</span>
            </div>

            <div class="mdm-price-row">
              <span class="mdm-price mono">{{ formatPrice(menu.price) }}<small>원</small></span>
              <button class="mdm-cart-btn" :disabled="menu.isSoldOut" @click="$emit('add-to-cart', menu)">
                <svg v-if="!menu.isSoldOut" width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M2.5 2.5h1.2l.8 7h7l1.2-5H5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/><circle cx="6" cy="13" r="0.8" fill="currentColor"/><circle cx="11" cy="13" r="0.8" fill="currentColor"/></svg>
                {{ menu.isSoldOut ? '품절' : '담기' }}
              </button>
            </div>

            <!-- AI Summary -->
            <div v-if="aiSummary" class="mdm-ai">
              <div class="mdm-ai-head">
                <span class="mdm-ai-tag">✦ AI</span>
                <span>리뷰 요약</span>
              </div>
              <p class="mdm-ai-text">{{ aiSummary }}</p>
            </div>
          </div>
        </div>

        <!-- RIGHT: Reviews -->
        <div class="mdm-right">
          <div class="rv-head">
            <h3 class="rv-title">리뷰</h3>
            <div v-if="summary" class="rv-score">
              <span class="rv-score-num">{{ summary.averageRating }}</span>
              <div class="rv-score-detail">
                <span class="rv-score-stars">{{ '★'.repeat(Math.round(summary.averageRating || 0)) }}{{ '☆'.repeat(5 - Math.round(summary.averageRating || 0)) }}</span>
                <span class="rv-score-count">{{ summary.totalCount }}개의 리뷰</span>
              </div>
            </div>
          </div>

          <div v-if="reviewsLoading" class="rv-loading"><span class="spinner"></span></div>
          <div v-else-if="groupedReviews.length === 0" class="rv-empty">
            <span class="rv-empty-icon">💬</span>
            <span>아직 리뷰가 없어요</span>
            <span class="rv-empty-sub">첫 번째 리뷰를 남겨보세요</span>
          </div>
          <div v-else class="rv-list">
            <div v-for="(rv, idx) in groupedReviews" :key="rv.id" class="rv-card" :style="{ animationDelay: `${idx * 40}ms` }">
              <!-- Review header -->
              <div class="rv-card-head">
                <div class="rv-card-user">
                  <div class="rv-avatar">{{ (rv.nickname || '익')[0] }}</div>
                  <div>
                    <span class="rv-nickname">{{ rv.nickname || '익명' }}</span>
                    <span class="rv-date">{{ fmtDate(rv.createdAt) }}</span>
                  </div>
                </div>
                <span class="rv-rating">{{ '★'.repeat(rv.rating) }}<span class="rv-rating-off">{{ '☆'.repeat(5 - rv.rating) }}</span></span>
              </div>

              <!-- Ordered menus -->
              <div v-if="rv.orderMenus?.length > 1" class="rv-menus">
                <span class="rv-menus-label">주문</span>
                <span v-for="name in rv.orderMenus" :key="name" class="rv-menu-chip">{{ name }}</span>
              </div>

              <!-- Content -->
              <p class="rv-text">{{ rv.content }}</p>

              <span v-if="rv.isAiGenerated" class="rv-ai">✦ AI 작성</span>

              <!-- Admin reply -->
              <div v-if="rv.adminReply" class="rv-reply">
                <div class="rv-reply-head">
                  <span class="rv-reply-who">사장님</span>
                </div>
                <p class="rv-reply-text">{{ rv.adminReply }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import { useFormatting } from '../composables/useFormatting'
import { categoryEmoji } from '../constants'

const props = defineProps({ menu: Object })
defineEmits(['close', 'add-to-cart'])

const { formatPrice, formatDateShort: fmtDate } = useFormatting()
const emoji = categoryEmoji(props.menu.categoryName)

const reviews = ref([])
const groupedReviews = ref([])
const summary = ref(null)
const aiSummary = ref('')
const reviewsLoading = ref(true)

function groupReviews(list) {
  const groups = new Map()
  for (const r of list) {
    const key = `${r.orderId}_${r.rating}_${r.content}`
    if (groups.has(key)) {
      const g = groups.get(key)
      if (!g.orderMenus.includes(r.menuName)) g.orderMenus.push(r.menuName)
    } else {
      groups.set(key, { ...r, orderMenus: [r.menuName] })
    }
  }
  return [...groups.values()]
}

async function loadOrderMenus(rvList) {
  const orderIds = [...new Set(rvList.map(r => r.orderId).filter(Boolean))]
  const orderMenuMap = {}
  await Promise.all(orderIds.map(async id => {
    try {
      const res = await api.get(`/orders/${id}`)
      orderMenuMap[id] = (res.data.items || []).map(i => i.menuName)
    } catch {}
  }))
  for (const rv of rvList) {
    if (orderMenuMap[rv.orderId]?.length) {
      rv.orderMenus = orderMenuMap[rv.orderId]
    }
  }
}

onMounted(async () => {
  try {
    const [rvRes, smRes, aiRes] = await Promise.allSettled([
      api.get('/reviews', { params: { menuId: props.menu.id, page: 0, size: 50 } }),
      api.get('/reviews/summary', { params: { menuId: props.menu.id } }),
      api.get('/reviews/ai-summary', { params: { menuId: props.menu.id } })
    ])
    if (rvRes.status === 'fulfilled') {
      reviews.value = rvRes.value.data.content || []
      const grouped = groupReviews(reviews.value)
      groupedReviews.value = grouped
      loadOrderMenus(grouped).then(() => { groupedReviews.value = [...grouped] })
    }
    if (smRes.status === 'fulfilled') summary.value = smRes.value.data
    if (aiRes.status === 'fulfilled' && aiRes.value.data?.summary) aiSummary.value = aiRes.value.data.summary
  } catch {} finally { reviewsLoading.value = false }
})
</script>

<style scoped>
/* ── Modal shell ── */
.mdm {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  width: 800px; max-height: 82vh;
  box-shadow: 0 32px 80px rgba(0,0,0,0.7), 0 0 0 1px rgba(255,255,255,0.03) inset;
  position: relative; overflow: hidden;
  display: flex; flex-direction: column;
  animation: mdmIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes mdmIn { from { opacity: 0; transform: translateY(16px) scale(0.97); } to { opacity: 1; transform: translateY(0) scale(1); } }

.mdm-close {
  position: absolute; top: 14px; right: 14px; z-index: 10;
  width: 34px; height: 34px; border-radius: 99px;
  background: rgba(0,0,0,0.5); backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.08);
  color: #fff; display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s;
}
.mdm-close:hover { background: rgba(0,0,0,0.75); border-color: rgba(255,255,255,0.15); }

.mdm-layout {
  display: grid; grid-template-columns: 1fr 1fr;
  flex: 1; min-height: 0;
}

/* ── LEFT: Menu Info ── */
.mdm-left { overflow-y: auto; border-right: 1px solid var(--border); }

.mdm-visual {
  position: relative; height: 280px;
  background: var(--bg-subtle); overflow: hidden;
}
.mdm-img { width: 100%; height: 100%; object-fit: cover; display: block; }
.mdm-img-empty { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; font-size: 56px; opacity: 0.25; }
.mdm-img-fade {
  position: absolute; bottom: 0; left: 0; right: 0; height: 80px;
  background: linear-gradient(to top, var(--bg-elevated), transparent);
  pointer-events: none;
}
.mdm-badges { position: absolute; top: 12px; left: 12px; display: flex; gap: 6px; }
.mdm-badge {
  font-size: 10px; font-weight: 700; letter-spacing: 0.06em;
  padding: 3px 10px; border-radius: 99px;
  backdrop-filter: blur(8px);
}
.mdm-badge-best { background: rgba(212,134,60,0.85); color: #fff; }
.mdm-badge-sold { background: rgba(0,0,0,0.6); color: var(--text-secondary); }

.mdm-info { padding: 20px 24px 24px; }
.mdm-cat {
  font-size: 11px; color: var(--accent-soft); font-weight: 700;
  letter-spacing: 0.1em; text-transform: uppercase; margin-bottom: 4px;
}
.mdm-name { font-size: 22px; font-weight: 700; color: var(--text-primary); margin: 0 0 8px; line-height: 1.3; }
.mdm-desc { font-size: 13px; color: var(--text-secondary); line-height: 1.7; margin-bottom: 14px; }

.mdm-chips { display: flex; flex-wrap: wrap; gap: 5px; margin-bottom: 16px; }
.mdm-chip {
  font-size: 11px; color: var(--text-muted); font-weight: 500;
  background: var(--bg-subtle); border: 1px solid var(--border);
  padding: 3px 10px; border-radius: 99px;
}
.mdm-chip-tag { color: var(--text-secondary); }

.mdm-price-row {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 16px; border-top: 1px solid var(--border);
}
.mdm-price { font-size: 24px; font-weight: 800; color: var(--accent-soft); letter-spacing: -0.5px; }
.mdm-price small { font-size: 14px; font-weight: 400; opacity: 0.6; margin-left: 1px; }

.mdm-cart-btn {
  background: var(--accent); color: #fff; border: none;
  border-radius: var(--radius-md); padding: 11px 24px;
  font-size: 14px; font-weight: 700; font-family: inherit;
  cursor: pointer; transition: all 0.2s;
  display: flex; align-items: center; gap: 7px;
  box-shadow: 0 2px 12px rgba(212,134,60,0.25);
}
.mdm-cart-btn:hover:not(:disabled) { background: var(--accent-soft); box-shadow: 0 4px 20px rgba(212,134,60,0.4); transform: translateY(-1px); }
.mdm-cart-btn:active:not(:disabled) { transform: translateY(0); }
.mdm-cart-btn:disabled { background: var(--bg-subtle); color: var(--text-muted); cursor: not-allowed; box-shadow: none; }

/* AI Summary */
.mdm-ai {
  margin-top: 16px; padding: 14px 16px;
  background: rgba(212,134,60,0.04);
  border: 1px solid rgba(212,134,60,0.12);
  border-radius: var(--radius-md);
}
.mdm-ai-head { display: flex; align-items: center; gap: 6px; font-size: 11px; font-weight: 700; color: var(--accent-soft); margin-bottom: 6px; }
.mdm-ai-tag { background: var(--accent); color: #fff; padding: 1px 5px; border-radius: 3px; font-size: 9px; letter-spacing: 0.05em; }
.mdm-ai-text { font-size: 13px; color: var(--text-secondary); line-height: 1.6; margin: 0; }

/* ── RIGHT: Reviews ── */
.mdm-right {
  display: flex; flex-direction: column;
  overflow: hidden; padding: 20px 22px;
  background: var(--bg-card);
}

.rv-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-shrink: 0; }
.rv-title { font-size: 16px; font-weight: 700; color: var(--text-primary); margin: 0; }

.rv-score { display: flex; align-items: center; gap: 8px; }
.rv-score-num { font-size: 24px; font-weight: 800; color: #fbbf24; line-height: 1; }
.rv-score-detail { display: flex; flex-direction: column; gap: 1px; }
.rv-score-stars { font-size: 11px; color: #fbbf24; letter-spacing: 0.5px; }
.rv-score-count { font-size: 11px; color: var(--text-muted); }

.rv-loading { display: flex; justify-content: center; padding: 24px; }
.rv-empty {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 4px; color: var(--text-muted); font-size: 13px;
}
.rv-empty-icon { font-size: 28px; opacity: 0.3; margin-bottom: 4px; }
.rv-empty-sub { font-size: 12px; opacity: 0.6; }

/* Review list */
.rv-list {
  flex: 1; overflow-y: auto;
  display: flex; flex-direction: column; gap: 8px;
  padding-right: 4px;
}

.rv-card {
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: var(--radius-md); padding: 14px 16px;
  flex-shrink: 0;
  animation: rvIn 0.3s cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes rvIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }

/* Card header */
.rv-card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.rv-card-user { display: flex; align-items: center; gap: 8px; }
.rv-avatar {
  width: 28px; height: 28px; border-radius: 99px;
  background: var(--bg-subtle); border: 1px solid var(--border);
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; color: var(--text-muted);
}
.rv-nickname { display: block; font-size: 13px; font-weight: 600; color: var(--text-primary); line-height: 1.2; }
.rv-date { font-size: 11px; color: var(--text-muted); }
.rv-rating { font-size: 12px; color: #fbbf24; letter-spacing: 0.5px; flex-shrink: 0; }
.rv-rating-off { color: rgba(255,255,255,0.08); }

/* Ordered menus */
.rv-menus { display: flex; flex-wrap: wrap; align-items: center; gap: 4px; margin-bottom: 8px; }
.rv-menus-label { font-size: 10px; font-weight: 600; color: var(--text-muted); margin-right: 2px; }
.rv-menu-chip {
  font-size: 11px; font-weight: 500; color: var(--text-secondary);
  background: var(--bg-subtle); padding: 2px 8px; border-radius: 4px;
}

/* Content */
.rv-text { font-size: 13px; color: var(--text-secondary); line-height: 1.6; margin: 0; }
.rv-ai { display: inline-block; margin-top: 8px; font-size: 10px; color: var(--accent-soft); background: var(--accent-bg); padding: 2px 6px; border-radius: 4px; }

/* Admin reply */
.rv-reply {
  margin-top: 10px; padding: 10px 12px;
  background: rgba(212,134,60,0.04);
  border-radius: var(--radius-sm);
  border-left: 2px solid var(--accent);
}
.rv-reply-head { margin-bottom: 3px; }
.rv-reply-who { font-size: 11px; font-weight: 700; color: var(--accent-soft); }
.rv-reply-text { font-size: 12px; color: var(--text-secondary); line-height: 1.5; margin: 0; }
</style>
