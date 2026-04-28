<template>
  <div class="page dash">
    <!-- ─── Top Bar ─── -->
    <div class="dash-header">
      <div>
        <h1 class="dash-title">Dashboard</h1>
        <p class="dash-date">{{ todayFormatted }}</p>
      </div>
      <div class="dash-header-right">
        <div class="date-range-wrap" ref="dateRangeRef">
          <button class="date-range-btn" @click="showDatePicker = !showDatePicker">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><rect x="1.5" y="2.5" width="11" height="10" rx="1.5" stroke="currentColor" stroke-width="1.2"/><path d="M1.5 5.5h11M4.5 1v2.5M9.5 1v2.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
            {{ dateRangeLabel }}
          </button>
          <div v-if="showDatePicker" class="date-dropdown">
            <div class="dd-presets">
              <button class="dd-preset" :class="{ active: datePreset === 'today' }" @click="setDatePreset('today')">오늘</button>
              <button class="dd-preset" :class="{ active: datePreset === '3d' }" @click="setDatePreset('3d')">3일</button>
              <button class="dd-preset" :class="{ active: datePreset === '7d' }" @click="setDatePreset('7d')">7일</button>
              <button class="dd-preset" :class="{ active: datePreset === '30d' }" @click="setDatePreset('30d')">30일</button>
              <button class="dd-preset" :class="{ active: datePreset === 'custom' }" @click="datePreset = 'custom'">직접 설정</button>
            </div>
            <div v-if="datePreset === 'custom'" class="dd-custom">
              <label class="dd-label">시작일<input type="date" class="dd-input" v-model="dashDateFrom" /></label>
              <span class="dd-sep">~</span>
              <label class="dd-label">종료일<input type="date" class="dd-input" v-model="dashDateTo" /></label>
              <button class="dd-apply" @click="applyCustomDate">적용</button>
            </div>
          </div>
        </div>
        <div class="live-chip">
          <span class="live-dot"></span>
          실시간 · {{ refreshCountdown }}s
        </div>
      </div>
    </div>

    <!-- ─── KPI Strip ─── -->
    <div class="kpi-strip">
      <div class="kpi">
        <span class="kpi-label">{{ datePreset === 'today' ? '오늘 매출' : '기간 매출' }}</span>
        <span class="kpi-value">{{ fmtNum(revenue.totalRevenue) }}<small>원</small></span>
      </div>
      <div class="kpi-divider"></div>
      <div class="kpi">
        <span class="kpi-label">주문 수</span>
        <span class="kpi-value">{{ revenue.orderCount ?? '—' }}<small>건</small></span>
      </div>
      <div class="kpi-divider"></div>
      <div class="kpi">
        <span class="kpi-label">진행 중</span>
        <span class="kpi-value kpi-active">{{ activeCount ?? '—' }}<small>건</small></span>
      </div>
      <div class="kpi-divider"></div>
      <div class="kpi">
        <span class="kpi-label">혼잡도</span>
        <span class="kpi-value" :class="`kpi-cong-${aiCongestionClass}`">
          {{ aiCongestionLabel }}
        </span>
      </div>
      <div class="kpi-divider"></div>
      <div class="kpi">
        <span class="kpi-label">객단가</span>
        <span class="kpi-value">{{ avgOrderPrice }}<small>원</small></span>
      </div>
    </div>

    <!-- ─── Charts Row ─── -->
    <div class="charts-row">
      <RevenueChart :data="weeklyRevenue" />
      <StatusDonut :orders="allOrders" />
    </div>

    <!-- ─── Bottom Grid ─── -->
    <div class="dash-grid">

      <!-- Best Sellers bar chart -->
      <div class="panel">
        <div class="panel-header">
          <h3>베스트셀러</h3>
          <span class="panel-badge">주간 TOP {{ bestSellers.length }}</span>
        </div>
        <div v-if="bestSellers.length === 0" class="panel-empty">데이터 없음</div>
        <div v-else class="bar-chart">
          <div v-for="(item, i) in bestSellers" :key="item.menuId" class="bar-row" :style="{ animationDelay: `${i * 60}ms` }">
            <div class="bar-rank" :class="{ 'bar-rank-top': i < 3 }">{{ i + 1 }}</div>
            <div class="bar-info">
              <div class="bar-name">{{ item.menuName }}</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: barWidth(item) + '%' }" :class="{ 'bar-fill-top': i < 3 }"></div>
              </div>
            </div>
            <div class="bar-count">{{ (item.totalQuantity ?? item.soldCount ?? 0).toLocaleString() }}</div>
          </div>
        </div>
      </div>

      <!-- Right column: gauge + orders -->
      <div class="right-col">
        <div class="panel panel-compact">
          <div class="panel-header"><h3>매장 상태</h3></div>
          <div class="gauge-row">
            <div class="gauge">
              <div class="gauge-track">
                <div class="gauge-fill" :style="{ width: aiCongestionPercent + '%' }" :class="`gauge-${aiCongestionClass}`"></div>
              </div>
              <div class="gauge-labels"><span>여유</span><span>보통</span><span>혼잡</span></div>
            </div>
            <div class="gauge-detail">
              <span class="gauge-level" :class="`kpi-cong-${aiCongestionClass}`">{{ aiCongestionLabel }}</span>
              <span class="gauge-msg">
                진행 중 {{ inProgressCount }}건 (대기 {{ pendingCount }}·확인 {{ acceptedCount }}·조리 {{ cookingCount }})
                <template v-if="congestion.estimated_wait_minutes"> · 예상 대기 {{ congestion.estimated_wait_minutes }}분</template>
              </span>
            </div>
          </div>
        </div>

        <div class="panel panel-grow">
          <div class="panel-header">
            <h3>진행 중 주문</h3>
            <span class="panel-badge">{{ activeOrders.length }}건</span>
          </div>
          <div v-if="activeOrders.length === 0" class="panel-empty">진행 중인 주문 없음</div>
          <div v-else class="order-feed">
            <div v-for="order in activeOrders.slice(0, 6)" :key="order.id" class="order-row">
              <div class="order-id">#{{ order.id }}</div>
              <div class="order-items">{{ order.items?.map(i => i.menuName).join(', ') || '—' }}</div>
              <div class="order-status" :class="orderStatusClass(order.status)">{{ ORDER_STATUS[order.status] || order.status }}</div>
              <div class="order-time">{{ timeAgo(order.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ─── AI 매장 진단 ─── -->
    <div class="panel quality-panel">
      <div class="panel-header">
        <h3>AI 매장 진단</h3>
        <div class="quality-header-right">
          <span v-if="qualityCacheInfo" class="quality-cache-info">{{ qualityCacheInfo }}</span>
          <button class="btn btn-secondary btn-sm" @click="runQualityAnalysis(true)" :disabled="qualityLoading">
            <span class="spinner-xs" v-if="qualityLoading"></span>
            <svg v-else width="13" height="13" viewBox="0 0 13 13" fill="none"><path d="M6.5 1.5C3.74 1.5 1.5 3.74 1.5 6.5S3.74 11.5 6.5 11.5 11.5 9.26 11.5 6.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/><path d="M9 1l1.5 1.5L9 4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg>
            {{ qualityLoading ? '분석 중...' : '새로 분석' }}
          </button>
        </div>
      </div>
      <div v-if="qualityError" class="alert-box alert-error">{{ qualityError }}</div>
      <div v-if="!qualityResult && qualityLoading" class="panel-empty"><div class="loading-spinner"></div> AI가 매장 상태를 분석하고 있습니다...</div>
      <div v-if="qualityResult" class="quality-result">
        <div class="quality-score">
          <span class="score-num">{{ qualityResult.overall_score.toFixed(1) }}</span>
          <span class="score-label">/ 5.0 종합 점수</span>
        </div>
        <div class="quality-cols">
          <div class="quality-col">
            <div class="quality-col-title">강점</div>
            <ul class="quality-list">
              <li v-for="(s, i) in qualityResult.strengths" :key="i">{{ s }}</li>
            </ul>
          </div>
          <div class="quality-col">
            <div class="quality-col-title">약점</div>
            <ul class="quality-list">
              <li v-for="(w, i) in qualityResult.weaknesses" :key="i">{{ w }}</li>
            </ul>
          </div>
          <div class="quality-col">
            <div class="quality-col-title">개선 제안</div>
            <ul class="quality-list">
              <li v-for="(r, i) in qualityResult.recommendations" :key="i">{{ r }}</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '../api'
import { analyzeQuality } from '../api.js'
import { formatNum as fmtNum, formatNumShort as fmtShort, timeAgo } from '../utils/formatters.js'
import { CONGESTION_LABELS } from '../utils/status.js'
import RevenueChart from '../components/RevenueChart.vue'
import StatusDonut from '../components/StatusDonut.vue'

// ── Date range ──
const todayStr = () => new Date().toISOString().substring(0, 10)
const dashDateFrom = ref(todayStr())
const dashDateTo = ref(todayStr())
const datePreset = ref('today')
const showDatePicker = ref(false)
const dateRangeRef = ref(null)

const dateRangeLabel = computed(() => {
  if (dashDateFrom.value === dashDateTo.value) return dashDateFrom.value
  return `${dashDateFrom.value} ~ ${dashDateTo.value}`
})

function setDatePreset(p) {
  datePreset.value = p
  const t = new Date()
  dashDateTo.value = todayStr()
  if (p === 'today') dashDateFrom.value = todayStr()
  else if (p === '3d') { t.setDate(t.getDate() - 2); dashDateFrom.value = t.toISOString().substring(0, 10) }
  else if (p === '7d') { t.setDate(t.getDate() - 6); dashDateFrom.value = t.toISOString().substring(0, 10) }
  else if (p === '30d') { t.setDate(t.getDate() - 29); dashDateFrom.value = t.toISOString().substring(0, 10) }
  if (p !== 'custom') { showDatePicker.value = false; loadStats() }
}

function applyCustomDate() {
  if (dashDateFrom.value > dashDateTo.value) { const tmp = dashDateFrom.value; dashDateFrom.value = dashDateTo.value; dashDateTo.value = tmp }
  showDatePicker.value = false
  loadStats()
}

function onClickOutsideDate(e) {
  if (dateRangeRef.value && !dateRangeRef.value.contains(e.target)) showDatePicker.value = false
}

// ── State ──
const revenue = ref({})
const activeCount = ref(null)
const activeOrders = ref([])
const congestion = ref({})
const bestSellers = ref([])
const weeklyRevenue = ref([])
const allOrders = ref([])
const refreshCountdown = ref(30)

// CONGESTION_LABELS imported from utils/status.js
const ORDER_STATUS = { PENDING: '대기', ACCEPTED: '접수', COOKING: '조리중', READY: '완료', COMPLETED: '완료' }

// ── Computed ──
const todayFormatted = computed(() => {
  const d = new Date()
  const days = ['일', '월', '화', '수', '목', '금', '토']
  return `${d.getFullYear()}. ${d.getMonth() + 1}. ${d.getDate()}. (${days[d.getDay()]})`
})

const avgOrderPrice = computed(() => {
  const rev = revenue.value.totalRevenue
  const cnt = revenue.value.orderCount
  if (!rev || !cnt) return '—'
  return Math.round(rev / cnt).toLocaleString()
})

// Local congestion from active orders
const pendingCount = computed(() => activeOrders.value.filter(o => o.status === 'PENDING').length)
const acceptedCount = computed(() => activeOrders.value.filter(o => o.status === 'ACCEPTED').length)
const cookingCount = computed(() => activeOrders.value.filter(o => o.status === 'COOKING').length)
const inProgressCount = computed(() => pendingCount.value + acceptedCount.value + cookingCount.value)

const localCongestionLevel = computed(() => {
  const n = inProgressCount.value
  if (n >= 8) return 'high'
  if (n >= 5) return 'medium'
  return 'low'
})

const localCongestionPercent = computed(() => {
  const n = inProgressCount.value
  if (n >= 10) return 95
  return Math.min(Math.round((n / 10) * 100), 95)
})


const AI_LEVEL_MAP = { '여유': 'low', '보통': 'medium', '혼잡': 'high' }
const AI_PERCENT_MAP = { '여유': 20, '보통': 55, '혼잡': 90 }

const aiCongestionClass = computed(() =>
  AI_LEVEL_MAP[congestion.value.level] ?? localCongestionLevel.value
)
const aiCongestionPercent = computed(() =>
  AI_PERCENT_MAP[congestion.value.level] ?? localCongestionPercent.value
)
const aiCongestionLabel = computed(() =>
  congestion.value.level ?? CONGESTION_LABELS[localCongestionLevel.value.toUpperCase()] ?? '—'
)

// ── AI 매장 진단 (1시간 캐싱) ──
const CACHE_KEY = 'quality_analysis_cache'
const CACHE_TTL = 60 * 60 * 1000 // 1시간

const qualityResult = ref(null)
const qualityLoading = ref(false)
const qualityError = ref('')
const qualityCacheInfo = ref('')

function getCachedQuality(reviewCount, orderCount) {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    if (!raw) return null
    const cached = JSON.parse(raw)
    const age = Date.now() - cached.timestamp
    const dataChanged = (reviewCount != null && cached.reviewCount !== reviewCount)
        || (orderCount != null && cached.orderCount !== orderCount)
    // 1시간 지났어도 데이터 변화 없으면 캐시 유지
    if (age > CACHE_TTL && dataChanged) return null
    return cached
  } catch { return null }
}

function saveCacheQuality(result, reviewCount, orderCount) {
  localStorage.setItem(CACHE_KEY, JSON.stringify({
    result, reviewCount, orderCount, timestamp: Date.now()
  }))
}

function formatCacheAge(ts) {
  const min = Math.floor((Date.now() - ts) / 60000)
  if (min < 1) return '방금 분석'
  if (min < 60) return `${min}분 전 분석`
  return `${Math.floor(min / 60)}시간 전 분석`
}

async function runQualityAnalysis(forceRefresh = false) {
  qualityError.value = ''

  // 캐시 확인 (강제 새로고침이 아닌 경우)
  if (!forceRefresh) {
    const cached = getCachedQuality(revenue.value.orderCount ?? 0, allOrders.value.length)
    if (cached) {
      qualityResult.value = cached.result
      qualityCacheInfo.value = formatCacheAge(cached.timestamp)
      return
    }
  }

  qualityLoading.value = true
  try {
    const [reviewsRes, summaryRes] = await Promise.allSettled([
      api.get('/reviews', { params: { page: 0, size: 50 } }),
      api.get('/reviews/summary'),
    ])
    const reviews = reviewsRes.status === 'fulfilled'
      ? (reviewsRes.value.data?.content ?? reviewsRes.value.data ?? [])
      : []
    const summary = summaryRes.status === 'fulfilled' ? summaryRes.value.data : {}
    const orderStats = {
      total_revenue: revenue.value.totalRevenue ?? 0,
      order_count: revenue.value.orderCount ?? 0,
      active_orders: activeCount.value ?? 0,
      congestion_level: congestion.value.level ?? '알 수 없음',
      review_count: summary.totalCount ?? reviews.length,
      avg_rating: summary.averageRating ?? null,
      rating_distribution: summary.ratingDistribution ?? {},
      best_sellers: bestSellers.value.map(b => ({
        menu_name: b.menuName,
        quantity: b.totalQuantity ?? b.soldCount ?? 0,
      })),
      weekly_revenue: weeklyRevenue.value.map(d => ({
        date: d.date,
        revenue: d.revenue,
        count: d.count,
      })),
    }
    const res = await analyzeQuality(reviews, orderStats)
    qualityResult.value = res.data
    saveCacheQuality(res.data, summary.totalCount ?? 0, revenue.value.orderCount ?? 0)
    qualityCacheInfo.value = '방금 분석'
  } catch (e) {
    qualityError.value = 'AI 매장 진단에 실패했습니다.'
  } finally {
    qualityLoading.value = false
  }
}

// Chart components: RevenueChart, StatusDonut — handle their own computed

function barWidth(item) {
  const max = Math.max(...bestSellers.value.map(b => b.totalQuantity ?? b.soldCount ?? 0), 1)
  return Math.round(((item.totalQuantity ?? item.soldCount ?? 0) / max) * 100)
}

function orderStatusClass(s) {
  if (s === 'COMPLETED' || s === 'READY') return 'os-done'
  if (s === 'CANCELLED') return 'os-danger'
  return 'os-active'
}
// timeAgo imported from utils/formatters.js

function getDateRange(days) {
  const to = new Date()
  const from = new Date()
  from.setDate(from.getDate() - days + 1)
  return {
    from: from.toISOString().slice(0, 10),
    to: to.toISOString().slice(0, 10),
  }
}

function getDayLabel(dateStr) {
  const d = new Date(dateStr + 'T00:00:00')
  const days = ['일', '월', '화', '수', '목', '금', '토']
  return days[d.getDay()]
}

function getFullDayLabel(dateStr) {
  const d = new Date(dateStr + 'T00:00:00')
  return `${d.getMonth() + 1}/${d.getDate()}`
}

// ── Data Loading ──
async function loadStats() {
  refreshCountdown.value = 30
  const from = dashDateFrom.value
  const to = dashDateTo.value
  const range7 = getDateRange(7)

  const [rev, active, cong, best, weekly, orders] = await Promise.allSettled([
    api.get(`/orders/stats/revenue?period=daily&from=${from}&to=${to}`),
    api.get('/orders/active'),
    api.get('/operations/congestion'),
    api.get('/orders/stats/best-sellers?period=weekly&limit=10'),
    api.get(`/orders/stats/revenue?period=daily&from=${range7.from}&to=${range7.to}`),
    api.get('/orders/all?page=0&size=100'),
  ])

  if (rev.status === 'fulfilled') {
    const revData = rev.value.data
    if (Array.isArray(revData) && revData.length > 0) {
      // Sum across date range
      const totalRevenue = revData.reduce((s, r) => s + (r.totalRevenue || 0), 0)
      const orderCount = revData.reduce((s, r) => s + (r.orderCount || 0), 0)
      revenue.value = { totalRevenue, orderCount }
    } else {
      revenue.value = {}
    }
  }
  if (active.status === 'fulfilled') {
    const ad = active.value.data
    activeOrders.value = Array.isArray(ad) ? ad : []
    activeCount.value = activeOrders.value.length
  }
  if (cong.status === 'fulfilled') congestion.value = cong.value.data
  if (best.status === 'fulfilled') bestSellers.value = best.value.data || []

  // Weekly revenue chart data
  if (weekly.status === 'fulfilled') {
    const raw = weekly.value.data || []
    // Build all 7 days, fill missing with 0
    const map = {}
    for (const r of raw) map[r.period] = r
    const days = []
    for (let i = 6; i >= 0; i--) {
      const d = new Date()
      d.setDate(d.getDate() - i)
      const key = d.toISOString().slice(0, 10)
      const entry = map[key]
      days.push({
        date: key,
        label: getDayLabel(key),
        fullLabel: getFullDayLabel(key),
        revenue: entry?.totalRevenue ?? 0,
        count: entry?.orderCount ?? 0,
      })
    }
    weeklyRevenue.value = days
  }

  // All orders for status distribution
  if (orders.status === 'fulfilled') {
    const od = orders.value.data
    allOrders.value = Array.isArray(od) ? od : (od?.content || [])
  }
}

let timer = null
let countdownTimer = null
onMounted(async () => {
  await loadStats()
  runQualityAnalysis()
  timer = setInterval(loadStats, 30000)
  countdownTimer = setInterval(() => {
    if (refreshCountdown.value > 0) refreshCountdown.value--
  }, 1000)
  document.addEventListener('click', onClickOutsideDate)
})
onUnmounted(() => {
  clearInterval(timer)
  clearInterval(countdownTimer)
  document.removeEventListener('click', onClickOutsideDate)
})
</script>

<style scoped>
.dash {
  animation: page-enter 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes page-enter {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ─── Header ─── */
.dash-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.dash-header-right { display: flex; align-items: center; gap: 12px; }

/* Date range */
.date-range-wrap { position: relative; }
.date-range-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 14px; border: 1px solid var(--border-strong); border-radius: var(--radius-sm);
  background: transparent; color: var(--text-secondary); font-size: 13px; font-weight: 500;
  font-family: inherit; cursor: pointer; transition: all 0.15s; white-space: nowrap;
}
.date-range-btn:hover { border-color: var(--accent-brass-border); color: var(--text-primary); }
.date-dropdown {
  position: absolute; top: calc(100% + 6px); right: 0;
  background: var(--bg-elevated); border: 1px solid var(--border-strong); border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg); z-index: 50; min-width: 280px; animation: ddIn 0.15s ease;
}
@keyframes ddIn { from { opacity: 0; transform: translateY(-4px); } }
.dd-presets { display: flex; gap: 4px; padding: 12px; border-bottom: 1px solid var(--border); flex-wrap: wrap; }
.dd-preset {
  padding: 5px 12px; border-radius: 99px; border: 1px solid var(--border-strong);
  background: transparent; color: var(--text-secondary); font-size: 12px; font-weight: 600;
  font-family: inherit; cursor: pointer; transition: all 0.15s;
}
.dd-preset:hover { border-color: var(--accent-brass-border); color: var(--text-primary); }
.dd-preset.active { background: var(--accent-brass-glow); border-color: var(--accent-brass-border); color: var(--accent-brass); }
.dd-custom { padding: 12px; display: flex; align-items: flex-end; gap: 8px; flex-wrap: wrap; }
.dd-label { display: flex; flex-direction: column; gap: 4px; font-size: 11px; color: var(--text-muted); font-weight: 600; }
.dd-input {
  padding: 6px 10px; border: 1px solid var(--border-strong); border-radius: var(--radius-sm);
  background: var(--bg-input); color: var(--text-primary); font-family: inherit; font-size: 13px;
  outline: none; color-scheme: dark; width: 140px;
}
.dd-input:focus { border-color: var(--accent-brass); }
.dd-sep { color: var(--text-muted); font-size: 13px; padding-bottom: 6px; }
.dd-apply {
  padding: 6px 16px; border-radius: var(--radius-sm); border: none;
  background: var(--accent-brass); color: var(--text-inverse); font-size: 12px; font-weight: 700;
  font-family: inherit; cursor: pointer; transition: all 0.15s;
}
.dd-apply:hover { filter: brightness(1.1); }

.dash-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.8px;
}

.dash-date {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.live-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  padding: 6px 14px;
  border-radius: 20px;
  font-variant-numeric: tabular-nums;
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-brass);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}

/* ─── KPI Strip ─── */
.kpi-strip {
  display: flex;
  align-items: center;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px 0;
  margin-bottom: 18px;
}

.kpi {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 0 20px;
}

.kpi-divider {
  width: 1px;
  height: 36px;
  background: var(--border);
  flex-shrink: 0;
}

.kpi-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.kpi-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.8px;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.kpi-value small {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
  margin-left: 2px;
  letter-spacing: 0;
}

.kpi-active { color: var(--accent-brass); }
.kpi-cong-low { color: var(--accent-brass); }
.kpi-cong-medium { color: var(--text-secondary); }
.kpi-cong-high { color: var(--danger); }

/* ─── Charts Row ─── */
.charts-row {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 18px;
  margin-bottom: 18px;
}

/* ─── Panels (shared) ─── */
.panel {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 22px;
}

.panel-compact { padding: 20px 22px; }

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.panel-header h3 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.2px;
}

.panel-badge {
  font-size: 11px;
  font-weight: 600;
  color: var(--accent-brass);
  background: var(--accent-brass-glow);
  padding: 3px 10px;
  border-radius: 20px;
  border: 1px solid var(--accent-brass-border);
}

.panel-empty {
  text-align: center;
  padding: 32px;
  color: var(--text-muted);
  font-size: 13px;
}

/* ─── Revenue Line Chart ─── */
.panel-chart {
  position: relative;
}

.chart-area {
  position: relative;
  padding-left: 36px;
}

.revenue-svg {
  width: 100%;
  height: 180px;
  display: block;
}

.area-shape {
  animation: area-reveal 1s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes area-reveal {
  from { opacity: 0; }
  to { opacity: 1; }
}

.line-shape {
  stroke-dasharray: 2000;
  stroke-dashoffset: 2000;
  animation: line-draw 1.2s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}

@keyframes line-draw {
  to { stroke-dashoffset: 0; }
}

.chart-dot {
  opacity: 0;
  animation: dot-pop 0.3s ease forwards;
  animation-delay: 1s;
}

.chart-dot:nth-child(1) { animation-delay: 0.8s; }
.chart-dot:nth-child(2) { animation-delay: 0.9s; }
.chart-dot:nth-child(3) { animation-delay: 1.0s; }
.chart-dot:nth-child(4) { animation-delay: 1.1s; }
.chart-dot:nth-child(5) { animation-delay: 1.2s; }
.chart-dot:nth-child(6) { animation-delay: 1.3s; }
.chart-dot:nth-child(7) { animation-delay: 1.4s; }

@keyframes dot-pop {
  from { opacity: 0; r: 0; }
  to { opacity: 1; r: 3.5; }
}

.chart-x-labels {
  display: flex;
  justify-content: space-between;
  padding: 8px 20px 0;
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
}

.chart-y-labels {
  position: absolute;
  top: 0;
  left: 0;
  height: 180px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 18px 0;
  font-size: 10px;
  color: var(--text-muted);
  text-align: right;
  width: 32px;
}

/* Hover tooltips */
.chart-values {
  position: absolute;
  top: 0;
  left: 36px;
  right: 0;
  height: 180px;
  pointer-events: none;
}

.chart-hover-zone {
  position: absolute;
  top: 0;
  width: 0;
  height: 100%;
  pointer-events: all;
}

.chart-hover-zone::before {
  content: '';
  position: absolute;
  left: -20px;
  top: 0;
  width: 40px;
  height: 100%;
}

.chart-tooltip {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  background: var(--bg-elevated);
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s;
  box-shadow: 0 8px 24px rgba(0,0,0,0.3);
  z-index: 10;
}

.chart-hover-zone:hover .chart-tooltip {
  opacity: 1;
}

.tt-date { font-size: 11px; color: var(--text-muted); }
.tt-val { font-size: 14px; font-weight: 700; color: var(--accent-brass); }
.tt-cnt { font-size: 11px; color: var(--text-secondary); }

/* ─── Donut Chart ─── */
.panel-donut {
  display: flex;
  flex-direction: column;
}

.donut-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  flex: 1;
}

.donut-svg {
  width: 140px;
  height: 140px;
}

.donut-seg {
  animation: donut-draw 0.8s cubic-bezier(0.4, 0, 0.2, 1) both;
  transform-origin: center;
}

@keyframes donut-draw {
  from {
    stroke-dasharray: 0 301.6;
    opacity: 0;
  }
  to { opacity: 1; }
}

.donut-total {
  font-size: 22px;
  font-weight: 700;
  fill: var(--text-primary);
  font-family: inherit;
}

.donut-total-label {
  font-size: 10px;
  font-weight: 500;
  fill: var(--text-muted);
  font-family: inherit;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.donut-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  justify-content: center;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  flex-shrink: 0;
}

.legend-label {
  color: var(--text-secondary);
}

.legend-val {
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

/* ─── Bottom Grid ─── */
.dash-grid {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 18px;
  align-items: start;
}

.right-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-grow { flex: 1; }

/* ─── Bar Chart ─── */
.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  animation: bar-enter 0.4s cubic-bezier(0.4, 0, 0.2, 1) both;
}

@keyframes bar-enter {
  from { opacity: 0; transform: translateX(-12px); }
  to { opacity: 1; transform: translateX(0); }
}

.bar-rank {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  background: var(--bg-hover);
  color: var(--text-muted);
  flex-shrink: 0;
}

.bar-rank-top {
  background: var(--accent-brass-glow);
  color: var(--accent-brass);
  border: 1px solid var(--accent-brass-border);
}

.bar-info { flex: 1; min-width: 0; }

.bar-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bar-track {
  height: 6px;
  background: var(--bg-hover);
  border-radius: 3px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 3px;
  background: var(--text-muted);
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.bar-fill-top { background: var(--accent-brass); }

.bar-count {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
  min-width: 36px;
  text-align: right;
}

/* ─── Gauge ─── */
.gauge-row {
  display: flex;
  gap: 20px;
  align-items: center;
}

.gauge { flex: 1; }

.gauge-track {
  height: 8px;
  background: var(--bg-hover);
  border-radius: 4px;
  overflow: hidden;
}

.gauge-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.gauge-low { background: var(--accent-brass); }
.gauge-medium { background: var(--text-secondary); }
.gauge-high { background: var(--danger); }

.gauge-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 10px;
  color: var(--text-muted);
}

.gauge-detail {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  flex-shrink: 0;
}

.gauge-level {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.gauge-msg {
  font-size: 11px;
  color: var(--text-muted);
  max-width: 120px;
  text-align: right;
  line-height: 1.3;
}

/* ─── Order Feed ─── */
.order-feed {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.order-row {
  display: grid;
  grid-template-columns: 50px 1fr auto 56px;
  gap: 10px;
  align-items: center;
  padding: 10px 8px;
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}

.order-row:hover { background: var(--bg-hover); }

.order-id {
  font-family: 'SF Mono', 'Cascadia Code', monospace;
  font-size: 12px;
  font-weight: 600;
  color: var(--accent-brass);
}

.order-items {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-status {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 4px;
  white-space: nowrap;
}

.os-active { background: var(--status-active-bg); color: var(--status-active-text); }
.os-done { background: var(--status-done-bg); color: var(--status-done-text); }
.os-danger { background: var(--status-danger-bg); color: var(--status-danger-text); }

.order-time {
  font-size: 11px;
  color: var(--text-muted);
  text-align: right;
  white-space: nowrap;
}

/* ─── AI 매장 진단 ─── */
.quality-panel { margin-top: 18px; }
.quality-header-right { display: flex; align-items: center; gap: 10px; }
.quality-cache-info { font-size: 11px; color: var(--text-muted); font-weight: 500; }
.quality-result { padding-top: 12px; }
.quality-score { display: flex; align-items: baseline; gap: 8px; margin-bottom: 20px; }
.score-num { font-size: 40px; font-weight: 800; color: var(--accent-brass); letter-spacing: -1px; }
.score-label { font-size: 14px; color: var(--text-muted); }
.quality-cols { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.quality-col-title {
  font-size: 11px; font-weight: 700; letter-spacing: 0.8px; text-transform: uppercase;
  color: var(--text-muted); margin-bottom: 10px; padding-bottom: 8px; border-bottom: 1px solid var(--border);
}
.quality-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.quality-list li {
  font-size: 13px; color: var(--text-secondary); padding-left: 14px; position: relative; line-height: 1.5;
}
.quality-list li::before { content: '·'; position: absolute; left: 0; color: var(--text-muted); }
.spinner-xs {
  display: inline-block; width: 12px; height: 12px;
  border: 2px solid var(--border-strong); border-top-color: var(--accent-brass);
  border-radius: 50%; animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ─── Responsive ─── */
@media (max-width: 1000px) {
  .charts-row,
  .dash-grid {
    grid-template-columns: 1fr;
  }
  .kpi-strip {
    flex-wrap: wrap;
    gap: 12px;
    padding: 16px;
  }
  .kpi-divider { display: none; }
  .kpi { min-width: 100px; }
}
</style>
