<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="mdm">
      <button class="mdm-close" @click="$emit('close')">✕</button>

      <div class="mdm-layout">
        <!-- LEFT: Menu Info -->
        <div class="mdm-left">
          <div class="mdm-visual">
            <img v-if="menu.imageUrl" :src="menu.imageUrl" :alt="menu.name" class="mdm-img" />
            <div v-else class="mdm-img-empty"><span>{{ emoji }}</span></div>
            <div class="mdm-badges">
              <span v-if="menu.isBestSeller" class="pill pill-best">BEST</span>
              <span v-if="menu.isSoldOut" class="pill pill-sold">품절</span>
            </div>
          </div>

          <div class="mdm-info">
            <div class="mdm-cat">{{ menu.categoryName }}</div>
            <h2 class="mdm-name">{{ menu.name }}</h2>
            <p v-if="menu.description" class="mdm-desc">{{ menu.description }}</p>

            <div class="mdm-meta">
              <span v-if="menu.spicyLevel > 0" class="meta-item">🌶 맵기 {{ menu.spicyLevel }}</span>
              <span v-if="menu.cookTimeMinutes" class="meta-item">⏱ {{ menu.cookTimeMinutes }}분</span>
              <span v-if="menu.allergens" class="meta-item">⚠ {{ menu.allergens }}</span>
            </div>

            <div v-if="menu.tags" class="mdm-tags">
              <span v-for="tag in menu.tags.split(',')" :key="tag" class="chip">{{ tag.trim() }}</span>
            </div>

            <div class="mdm-price-row">
              <span class="mdm-price mono">{{ formatPrice(menu.price) }}<small>원</small></span>
              <button class="btn-cart-lg" :disabled="menu.isSoldOut" @click="$emit('add-to-cart', menu)">
                {{ menu.isSoldOut ? '품절' : '장바구니 담기' }}
              </button>
            </div>

            <!-- AI Review Summary -->
            <div v-if="aiSummary" class="ai-summary">
              <div class="ai-summary-badge">✦ AI 리뷰 요약</div>
              <p class="ai-summary-text">{{ aiSummary }}</p>
            </div>
          </div>
        </div>

        <!-- RIGHT: Reviews -->
        <div class="mdm-right">
          <div class="rv-header">
            <h3 class="section-title">리뷰</h3>
            <div v-if="summary" class="rv-summary">
              <span class="rv-avg">★ {{ summary.averageRating }}</span>
              <span class="rv-count">({{ summary.totalCount }}개)</span>
            </div>
          </div>

          <div v-if="reviewsLoading" class="rv-loading"><span class="spinner"></span></div>
          <div v-else-if="reviews.length === 0" class="rv-empty">아직 리뷰가 없어요</div>
          <div v-else class="rv-list">
            <div v-for="rv in reviews" :key="rv.id" class="rv-card">
              <div class="rv-card-top">
                <span class="rv-user">{{ rv.nickname || '익명' }}</span>
                <span class="rv-stars">{{ '★'.repeat(rv.rating) }}{{ '☆'.repeat(5 - rv.rating) }}</span>
                <span class="rv-date">{{ fmtDate(rv.createdAt) }}</span>
              </div>
              <p class="rv-content">{{ rv.content }}</p>
              <span v-if="rv.isAiGenerated" class="rv-ai-tag">✦ AI 작성</span>
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
const summary = ref(null)
const aiSummary = ref('')
const reviewsLoading = ref(true)

onMounted(async () => {
  try {
    const [rvRes, smRes, aiRes] = await Promise.allSettled([
      api.get('/reviews', { params: { menuId: props.menu.id, page: 1, size: 50 } }),
      api.get('/reviews/summary', { params: { menuId: props.menu.id } }),
      api.get('/reviews/ai-summary', { params: { menuId: props.menu.id } })
    ])
    if (rvRes.status === 'fulfilled') reviews.value = rvRes.value.data.content || []
    if (smRes.status === 'fulfilled') summary.value = smRes.value.data
    if (aiRes.status === 'fulfilled' && aiRes.value.data?.summary) aiSummary.value = aiRes.value.data.summary
  } catch {} finally { reviewsLoading.value = false }
})
</script>

<style scoped>
.mdm {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  width: 780px;
  max-height: 80vh;
  box-shadow: 0 24px 64px rgba(0,0,0,.6);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.mdm-close { position:absolute; top:14px; right:14px; width:32px; height:32px; border-radius:99px; background:rgba(0,0,0,.45); backdrop-filter:blur(8px); border:none; color:#fff; display:flex; align-items:center; justify-content:center; font-size:14px; cursor:pointer; z-index:10; transition:var(--transition); }
.mdm-close:hover { background:rgba(0,0,0,.7); }

.mdm-layout {
  display: grid;
  grid-template-columns: 3fr 2fr;
  flex: 1;
  min-height: 0;
}

/* LEFT */
.mdm-left {
  overflow-y: auto;
  border-right: 1px solid var(--border);
}

.mdm-visual { position:relative; height:260px; background:var(--bg-subtle); overflow:hidden; flex-shrink:0; }
.mdm-img { width:100%; height:100%; object-fit:cover; }
.mdm-img-empty { width:100%; height:100%; display:flex; align-items:center; justify-content:center; font-size:56px; opacity:.3; }
.mdm-badges { position:absolute; top:10px; left:10px; display:flex; gap:5px; }
.mdm-info { padding: 20px 22px 24px; }
.mdm-cat { font-size:10px; color:var(--text-muted); font-weight:600; letter-spacing:.08em; text-transform:uppercase; margin-bottom:4px; }
.mdm-name { font-size:22px; font-weight:700; color:var(--text-primary); margin-bottom:6px; }
.mdm-desc { font-size:13px; color:var(--text-secondary); line-height:1.6; margin-bottom:12px; }

.mdm-meta { display:flex; gap:12px; margin-bottom:10px; }
.meta-item { font-size:12px; color:var(--text-muted); }

.mdm-tags { display:flex; flex-wrap:wrap; gap:4px; margin-bottom:16px; }
.mdm-price-row { display:flex; align-items:center; justify-content:space-between; padding-top:16px; border-top:1px solid var(--border); }
.mdm-price { font-size:22px; font-weight:700; color:var(--accent-soft); }
.mdm-price small { font-size:13px; font-weight:400; opacity:.7; margin-left:1px; }
.btn-cart-lg { background:var(--accent); color:#fff; border:none; border-radius:var(--radius-md); padding:10px 24px; font-size:14px; font-weight:700; font-family:inherit; cursor:pointer; transition:var(--transition); }
.btn-cart-lg:hover:not(:disabled) { background:var(--accent-soft); box-shadow:0 4px 16px rgba(212,134,60,.4); }
.btn-cart-lg:disabled { background:var(--bg-subtle); color:var(--text-muted); cursor:not-allowed; }

/* RIGHT */
.mdm-right {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 20px 22px;
}

/* AI Summary */
.ai-summary { margin-top:16px; padding:14px 16px; background:var(--accent-bg); border:1px solid rgba(212,134,60,.2); border-radius:var(--radius-md); }
.ai-summary-badge { font-size:10px; font-weight:700; color:var(--accent-soft); letter-spacing:.1em; margin-bottom:6px; }
.ai-summary-text { font-size:13px; color:var(--text-secondary); line-height:1.6; }

.rv-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; flex-shrink: 0; }
.section-title { font-size:15px; font-weight:700; color:var(--text-primary); margin:0; }
.rv-summary { display:flex; align-items:center; gap:4px; }
.rv-avg { font-size:14px; font-weight:700; color:#fbbf24; }
.rv-count { font-size:12px; color:var(--text-muted); }

.rv-loading { display:flex; justify-content:center; padding:20px; }
.rv-empty { text-align:center; padding:40px 0; font-size:13px; color:var(--text-muted); }

.rv-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 4px;
}

.rv-card { background:var(--bg-card); border:1px solid var(--border); border-radius:var(--radius-md); padding:12px 14px; flex-shrink: 0; }
.rv-card-top { display:flex; align-items:center; gap:8px; margin-bottom:6px; }
.rv-user { font-size:12px; font-weight:600; color:var(--text-primary); }
.rv-stars { font-size:12px; color:#fbbf24; letter-spacing:1px; flex:1; }
.rv-date { font-size:11px; color:var(--text-muted); }
.rv-content { font-size:13px; color:var(--text-secondary); line-height:1.5; }
.rv-ai-tag { display:inline-block; margin-top:6px; font-size:10px; color:var(--accent-soft); background:var(--accent-bg); padding:2px 6px; border-radius:4px; }
</style>
