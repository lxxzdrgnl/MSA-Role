<template>
  <div>
    <!-- Filter bar -->
    <div class="ol-filters">
      <button
        v-for="f in filters" :key="f.value"
        class="ol-filter"
        :class="{ active: activeFilter === f.value }"
        @click="activeFilter = f.value"
      >
        <span v-if="f.dot" class="ol-fdot" :class="`dot-${f.value}`"></span>
        {{ f.label }}
        <span v-if="filterCount(f.value)" class="ol-fcount">{{ filterCount(f.value) }}</span>
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="ol-grid">
      <div v-for="i in 4" :key="i" class="ol-skel">
        <div class="skel shimmer" style="height:120px;border-radius:12px 12px 0 0"></div>
        <div style="padding:14px;display:flex;flex-direction:column;gap:6px">
          <div class="skel shimmer" style="height:13px;width:60%;border-radius:4px"></div>
          <div class="skel shimmer" style="height:11px;width:40%;border-radius:3px"></div>
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else-if="filteredOrders.length === 0" class="ol-empty">
      <svg width="36" height="36" viewBox="0 0 36 36" fill="none" style="opacity:0.2">
        <rect x="5" y="3" width="26" height="30" rx="4" stroke="currentColor" stroke-width="1.5"/>
        <path d="M12 12h12M12 18h8M12 24h10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
      </svg>
      <span class="ol-empty-t">{{ activeFilter === '' ? '주문 내역이 없습니다' : '해당 주문이 없습니다' }}</span>
      <span class="ol-empty-s">{{ activeFilter === '' ? '메뉴를 둘러보고 첫 주문을 해보세요' : '다른 필터를 선택해보세요' }}</span>
    </div>

    <!-- Grid -->
    <div v-else class="ol-grid">
      <div
        v-for="(o, idx) in filteredOrders" :key="o.id"
        class="ol-card"
        :style="{ animationDelay: `${idx * 35}ms` }"
        @click="$emit('select', o.id)"
      >
        <!-- Image banner -->
        <div class="ol-banner">
          <img
            v-if="o.items?.length && menuImages[o.items[0].menuId]"
            :src="menuImages[o.items[0].menuId]" alt=""
            class="ol-banner-img"
            @error="e => e.target.style.display='none'"
          />
          <div v-else class="ol-banner-ph">
            <span>{{ o.items?.[0]?.menuName?.charAt(0) || '?' }}</span>
          </div>
          <div class="ol-banner-overlay"></div>
          <!-- Overlay badges -->
          <span class="ol-status" :class="`st-${(o.status||'').toLowerCase()}`">{{ SL[o.status] || o.status }}</span>
          <span v-if="(o.items||[]).length > 1" class="ol-count">{{ o.items.length }}개 메뉴</span>
        </div>

        <!-- Body -->
        <div class="ol-body">
          <div class="ol-head">
            <span class="ol-id mono">#{{ String(o.id).padStart(4,'0') }}</span>
            <span class="ol-date">{{ fmtDate(o.createdAt) }}</span>
          </div>
          <div class="ol-menus">{{ o.items?.map(i => i.menuName).join(', ') || '—' }}</div>
          <div class="ol-foot">
            <span class="ol-price mono">{{ fmtPrice(o.totalPrice) }}원</span>
            <svg class="ol-arrow" width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M5 3l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api'
import { useFormatting } from '../composables/useFormatting'
import { STATUS_LABELS } from '../constants'

defineEmits(['select'])

const orders = ref([])
const loading = ref(true)
const menuImages = ref({})
const activeFilter = ref('')
const SL = STATUS_LABELS
const { formatPrice: fmtPrice, formatDate: fmtDate } = useFormatting()

const filters = [
  { value: '', label: '전체' },
  { value: 'active', label: '진행중', dot: true },
  { value: 'COMPLETED', label: '완료', dot: true },
  { value: 'CANCELLED', label: '취소', dot: true },
]

function isActive(status) {
  return ['PENDING', 'ACCEPTED', 'COOKING', 'READY'].includes(status)
}

function filterCount(f) {
  if (f === '') return orders.value.length || 0
  if (f === 'active') return orders.value.filter(o => isActive(o.status)).length
  return orders.value.filter(o => o.status === f).length
}

const filteredOrders = computed(() => {
  if (activeFilter.value === '') return orders.value
  if (activeFilter.value === 'active') return orders.value.filter(o => isActive(o.status))
  return orders.value.filter(o => o.status === activeFilter.value)
})

async function fetchMenuImages(items) {
  const ids = [...new Set(items.flatMap(o => (o.items || []).map(i => i.menuId)).filter(Boolean))]
  const map = {}
  await Promise.all(ids.map(async id => {
    try {
      const res = await api.get(`/menus/${id}`)
      if (res.data?.imageUrl) map[id] = res.data.imageUrl
    } catch {}
  }))
  menuImages.value = map
}

onMounted(async () => {
  try {
    const r = await api.get('/orders', { params: { page: 0, size: 50 } })
    orders.value = Array.isArray(r.data) ? r.data : (r.data.content || [])
    if (orders.value.length) fetchMenuImages(orders.value)
  } catch {} finally { loading.value = false }
})
</script>

<style scoped>
/* ── Filters ── */
.ol-filters {
  display: flex; gap: 6px; margin-bottom: 16px;
  flex-wrap: wrap;
}
.ol-filter {
  display: flex; align-items: center; gap: 6px;
  padding: 7px 14px; border-radius: 99px;
  border: 1px solid var(--border);
  background: transparent; color: var(--text-secondary);
  font-size: 13px; font-weight: 600; font-family: inherit;
  cursor: pointer; transition: all 0.18s;
}
.ol-filter:hover {
  border-color: rgba(255,255,255,0.1);
  color: var(--text-primary);
}
.ol-filter.active {
  background: var(--accent-glow);
  border-color: var(--accent);
  color: var(--accent-soft);
}
.ol-fdot { width: 6px; height: 6px; border-radius: 50%; }
.dot-active { background: var(--accent); }
.dot-COMPLETED { background: var(--text-muted); }
.dot-CANCELLED { background: #f87171; }

.ol-fcount {
  font-size: 11px; font-weight: 700;
  background: rgba(255,255,255,0.06);
  padding: 0 6px; border-radius: 99px;
  line-height: 18px; min-width: 18px; text-align: center;
}
.ol-filter.active .ol-fcount {
  background: rgba(212,134,60,0.15);
  color: var(--accent);
}

/* ── Grid ── */
.ol-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

/* ── Skeleton ── */
.ol-skel {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius-lg); overflow: hidden;
}

/* ── Empty ── */
.ol-empty {
  display: flex; flex-direction: column; align-items: center;
  padding: 72px 0; gap: 8px; color: var(--text-muted);
}
.ol-empty-t { font-size: 15px; font-weight: 600; color: var(--text-secondary); }
.ol-empty-s { font-size: 13px; }

/* ── Card ── */
.ol-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  animation: cardIn 0.35s cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.ol-card:hover {
  border-color: var(--border-hover);
  box-shadow: 0 0 0 1px var(--border-hover), var(--shadow-glow);
  transform: translateY(-3px);
}

/* ── Banner ── */
.ol-banner {
  position: relative; height: 120px; overflow: hidden;
  background: var(--bg-subtle);
}
.ol-banner-img {
  width: 100%; height: 100%; object-fit: cover;
  display: block; transition: transform 0.3s;
}
.ol-card:hover .ol-banner-img { transform: scale(1.04); }

.ol-banner-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(to top, rgba(17,17,16,0.5) 0%, transparent 50%);
  pointer-events: none;
}

.ol-banner-ph {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 700; color: var(--text-muted); opacity: 0.3;
}

.ol-status {
  position: absolute; top: 10px; left: 10px;
  font-size: 11px; font-weight: 700;
  padding: 3px 10px; border-radius: 99px;
  backdrop-filter: blur(8px);
}
.ol-count {
  position: absolute; top: 10px; right: 10px;
  font-size: 10px; font-weight: 600;
  padding: 2px 8px; border-radius: 99px;
  background: rgba(0,0,0,0.55); color: var(--text-primary);
  backdrop-filter: blur(8px);
}

.st-pending { background: rgba(0,0,0,0.5); color: var(--text-secondary); }
.st-accepted { background: rgba(96,165,250,0.2); color: #93c5fd; }
.st-cooking { background: rgba(212,134,60,0.25); color: var(--accent-soft); }
.st-ready { background: rgba(74,222,128,0.2); color: #86efac; }
.st-completed { background: rgba(0,0,0,0.6); color: #c4bfb6; }
.st-cancelled { background: rgba(248,113,113,0.3); color: #fecaca; }

/* ── Body ── */
.ol-body { padding: 14px 16px; }

.ol-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 4px;
}
.ol-id { font-size: 14px; font-weight: 700; color: var(--text-primary); }
.ol-date { font-size: 12px; color: var(--text-muted); }

.ol-menus {
  font-size: 13px; color: var(--text-secondary); line-height: 1.3;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  margin-bottom: 10px;
}

.ol-foot {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 10px; border-top: 1px solid var(--border);
}
.ol-price { font-size: 16px; font-weight: 700; color: var(--accent-soft); }
.ol-arrow { color: var(--text-muted); transition: all 0.18s; }
.ol-card:hover .ol-arrow { color: var(--accent); transform: translateX(3px); }
</style>
