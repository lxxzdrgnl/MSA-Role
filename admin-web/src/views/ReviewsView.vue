<template>
  <div class="page reviews-page">
    <div class="page-header">
      <h1 class="page-title">리뷰 관리</h1>
      <div class="header-stats" v-if="summary.totalCount">
        <div class="stat">
          <span class="stat-val">{{ summary.totalCount }}</span>
          <span class="stat-label">전체</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat">
          <span class="stat-val stat-accent">{{ summary.averageRating?.toFixed(1) || '—' }}</span>
          <span class="stat-label">평균</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat">
          <span class="stat-val">{{ repliedCount }}</span>
          <span class="stat-label">답변완료</span>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <div class="filter-left">
        <!-- Rating chips -->
        <div class="filter-chips">
          <button v-for="r in ratingOptions" :key="r.value" class="chip" :class="{ active: selectedRating === r.value }" @click="selectRating(r.value)">
            <span v-if="r.star" class="chip-star">★</span>{{ r.label }}
          </button>
        </div>

        <!-- Menu select -->
        <select class="form-select form-select-sm" v-model="selectedMenuId" @change="resetAndFetch">
          <option value="">전체 메뉴</option>
          <option v-for="m in menus" :key="m.id" :value="m.id">{{ m.name }}</option>
        </select>
      </div>

      <!-- Date range -->
      <div class="date-range-wrap" ref="dateRangeRef">
        <button class="date-range-btn" @click="showDatePicker = !showDatePicker">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><rect x="1.5" y="2.5" width="11" height="10" rx="1.5" stroke="currentColor" stroke-width="1.2"/><path d="M1.5 5.5h11M4.5 1v2.5M9.5 1v2.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
          {{ dateRangeLabel }}
        </button>
        <div v-if="showDatePicker" class="date-dropdown">
          <div class="dd-presets">
            <button class="dd-preset" :class="{ active: datePreset === 'all' }" @click="setDatePreset('all')">전체</button>
            <button class="dd-preset" :class="{ active: datePreset === '7d' }" @click="setDatePreset('7d')">7일</button>
            <button class="dd-preset" :class="{ active: datePreset === '30d' }" @click="setDatePreset('30d')">30일</button>
            <button class="dd-preset" :class="{ active: datePreset === 'custom' }" @click="datePreset = 'custom'">직접 설정</button>
          </div>
          <div v-if="datePreset === 'custom'" class="dd-custom">
            <label class="dd-label">시작일<input type="date" class="dd-input" v-model="dateFrom" /></label>
            <span class="dd-sep">~</span>
            <label class="dd-label">종료일<input type="date" class="dd-input" v-model="dateTo" /></label>
            <button class="dd-apply" @click="applyCustomDate">적용</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="error" class="alert-box alert-error">{{ error }}</div>

    <!-- Rating distribution -->
    <div class="rating-dist" v-if="summary.ratingDistribution && !selectedRating">
      <div v-for="n in [5,4,3,2,1]" :key="n" class="dist-row">
        <span class="dist-stars">{{ '★'.repeat(n) }}</span>
        <div class="dist-track"><div class="dist-fill" :style="{ width: distPercent(n) + '%' }"></div></div>
        <span class="dist-count">{{ summary.ratingDistribution[n] || 0 }}</span>
      </div>
    </div>

    <!-- States -->
    <div v-if="loading && reviews.length === 0" class="state-loading"><div class="loading-spinner"></div> 불러오는 중...</div>
    <div v-else-if="reviews.length === 0" class="state-empty">리뷰가 없습니다</div>

    <!-- Review cards -->
    <div v-else class="rv-list">
      <div v-for="rv in reviews" :key="rv.id" class="rv-card">
        <!-- Header -->
        <div class="rv-head">
          <div class="rv-head-left">
            <span class="rv-stars">{{ '★'.repeat(rv.rating) }}<span class="rv-stars-off">{{ '★'.repeat(5 - rv.rating) }}</span></span>
            <span v-for="name in (rv.menuNames || [rv.menuName])" :key="name" class="rv-menu-badge">{{ name }}</span>
          </div>
          <div class="rv-head-right">
            <span class="rv-date">{{ fmtKST(rv.createdAt) }}</span>
            <button class="rv-del-btn" @click="handleDelete(rv)" title="삭제">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M4 4l6 6m0-6-6 6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
            </button>
          </div>
        </div>

        <!-- Content -->
        <p class="rv-content">{{ rv.content }}</p>

        <!-- Meta -->
        <div class="rv-meta">
          <span class="rv-user">{{ rv.nickname || `사용자 ${rv.userId}` }}</span>
          <span v-if="rv.aiGenerated" class="rv-ai-tag">AI</span>
          <span class="rv-order">주문 #{{ rv.orderId }}</span>
        </div>

        <!-- Admin reply -->
        <div v-if="rv.adminReply" class="rv-reply">
          <div class="rv-reply-head">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M11 7L7 3M11 7L7 11M11 7H3" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <span>관리자 답변</span>
            <span class="rv-reply-date">{{ fmtKST(rv.adminReplyAt) }}</span>
          </div>
          <p class="rv-reply-text">{{ rv.adminReply }}</p>
          <button class="rv-reply-edit" @click="openReply(rv)">수정</button>
        </div>

        <!-- Reply input -->
        <div v-else-if="replyingId !== rv.id" class="rv-reply-trigger">
          <button class="rv-reply-btn" @click="openReply(rv)">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M11 7L7 3M11 7L7 11M11 7H3" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg>
            답변 작성
          </button>
        </div>

        <!-- Reply form -->
        <div v-if="replyingId === rv.id" class="rv-reply-form">
          <textarea v-model="replyText" class="rv-reply-ta" rows="2" placeholder="답변을 입력하세요..." ref="replyTa"></textarea>
          <div class="rv-reply-actions">
            <button class="btn btn-secondary btn-sm" @click="replyingId = null">취소</button>
            <button class="btn btn-primary btn-sm" :disabled="!replyText.trim() || replying" @click="submitReply(rv)">
              {{ replying ? '등록 중...' : '답변 등록' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="totalPages > 1">
      <button class="btn btn-secondary btn-sm" :disabled="page === 0" @click="changePage(page - 1)">이전</button>
      <span class="page-info">{{ page + 1 }} / {{ totalPages }}</span>
      <button class="btn btn-secondary btn-sm" :disabled="page >= totalPages - 1" @click="changePage(page + 1)">다음</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { getReviews, getReviewSummary, getMenus, deleteReview, replyToReview } from '../api.js'
import { extractListData } from '../utils/formatters.js'
import { useAsync } from '../composables/useAsync.js'

const { loading, error, run } = useAsync()

const reviews = ref([])
const menus = ref([])
const summary = ref({})
const page = ref(0)
const totalPages = ref(1)
const selectedRating = ref('')
const selectedMenuId = ref('')

// Date
const dateFrom = ref('')
const dateTo = ref('')
const datePreset = ref('all')
const showDatePicker = ref(false)
const dateRangeRef = ref(null)

// Reply
const replyingId = ref(null)
const replyText = ref('')
const replying = ref(false)

const ratingOptions = [
  { value: '', label: '전체' },
  { value: 5, label: '5점', star: true },
  { value: 4, label: '4점', star: true },
  { value: 3, label: '3점', star: true },
  { value: 2, label: '2점', star: true },
  { value: 1, label: '1점', star: true },
]

const repliedCount = computed(() => reviews.value.filter(r => r.adminReply).length)

const dateRangeLabel = computed(() => {
  if (datePreset.value === 'all') return '전체 기간'
  if (dateFrom.value === dateTo.value) return dateFrom.value
  return `${dateFrom.value} ~ ${dateTo.value}`
})

function fmtKST(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  return d.toLocaleString('ko-KR', { timeZone: 'Asia/Seoul', year: '2-digit', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function distPercent(n) {
  const dist = summary.value.ratingDistribution || {}
  const total = summary.value.totalCount || 1
  return Math.round(((dist[n] || 0) / total) * 100)
}

function todayStr() { return new Date().toISOString().substring(0, 10) }

function setDatePreset(p) {
  datePreset.value = p
  if (p === 'all') { dateFrom.value = ''; dateTo.value = '' }
  else {
    dateTo.value = todayStr()
    const t = new Date()
    if (p === '7d') t.setDate(t.getDate() - 6)
    else if (p === '30d') t.setDate(t.getDate() - 29)
    dateFrom.value = t.toISOString().substring(0, 10)
  }
  if (p !== 'custom') { showDatePicker.value = false; resetAndFetch() }
}

function applyCustomDate() {
  if (dateFrom.value > dateTo.value) { const tmp = dateFrom.value; dateFrom.value = dateTo.value; dateTo.value = tmp }
  showDatePicker.value = false; resetAndFetch()
}

function onClickOutside(e) {
  if (dateRangeRef.value && !dateRangeRef.value.contains(e.target)) showDatePicker.value = false
}

function selectRating(v) { selectedRating.value = v; resetAndFetch() }
function resetAndFetch() { page.value = 0; fetchReviews() }

// Group reviews from same order (same orderId+rating+content → merge menu names)
function dedup(list) {
  const groups = new Map()
  for (const r of list) {
    const key = `${r.orderId}_${r.rating}_${r.content}`
    if (groups.has(key)) {
      const existing = groups.get(key)
      if (!existing.menuNames.includes(r.menuName)) {
        existing.menuNames.push(r.menuName)
      }
      existing.relatedIds.push(r.id)
    } else {
      groups.set(key, { ...r, menuNames: [r.menuName], relatedIds: [r.id] })
    }
  }
  return [...groups.values()]
}

async function fetchReviews() {
  await run(async () => {
    const params = { page: page.value, size: 50 }
    if (selectedRating.value) params.rating = selectedRating.value
    if (selectedMenuId.value) params.menuId = selectedMenuId.value
    const res = await getReviews(params)
    const data = res.data
    let list = data.content || []

    // Date filter (client-side since API doesn't have date params)
    if (dateFrom.value) {
      list = list.filter(r => {
        const d = (r.createdAt || '').substring(0, 10)
        return d >= dateFrom.value && (!dateTo.value || d <= dateTo.value)
      })
    }

    reviews.value = dedup(list)
    totalPages.value = data.totalPages || 1
  }, '리뷰를 불러오지 못했습니다.')
}

async function fetchSummary() {
  try { summary.value = (await getReviewSummary()).data || {} } catch {}
}

async function fetchMenus() {
  try {
    const { list } = extractListData(await getMenus({ page: 0, size: 200 }))
    menus.value = list
  } catch {}
}

async function handleDelete(rv) {
  if (!confirm(`"${rv.menuName}" 리뷰를 삭제하시겠습니까?`)) return
  await run(async () => {
    await deleteReview(rv.id)
    reviews.value = reviews.value.filter(r => r.id !== rv.id)
    fetchSummary()
  }, '삭제 실패')
}

function openReply(rv) {
  replyingId.value = rv.id
  replyText.value = rv.adminReply || ''
  nextTick(() => {
    const ta = document.querySelector('.rv-reply-ta')
    if (ta) ta.focus()
  })
}

async function submitReply(rv) {
  if (!replyText.value.trim()) return
  replying.value = true
  try {
    const res = await replyToReview(rv.id, replyText.value.trim())
    rv.adminReply = res.data.adminReply
    rv.adminReplyAt = res.data.adminReplyAt
    replyingId.value = null
  } catch {
    error.value = '답변 등록에 실패했습니다.'
  } finally { replying.value = false }
}

function changePage(p) { page.value = p; fetchReviews() }

onMounted(() => {
  fetchMenus(); fetchSummary(); fetchReviews()
  document.addEventListener('click', onClickOutside)
})
onUnmounted(() => {
  document.removeEventListener('click', onClickOutside)
})
</script>

<style scoped>
.reviews-page { animation: page-enter 0.4s cubic-bezier(0.4, 0, 0.2, 1); }

/* ── Header stats ── */
.header-stats {
  display: flex; align-items: center; gap: 20px;
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: var(--radius-md); padding: 10px 24px;
}
.stat { display: flex; align-items: baseline; gap: 6px; }
.stat-val { font-size: 20px; font-weight: 700; color: var(--text-primary); font-variant-numeric: tabular-nums; }
.stat-label { font-size: 12px; font-weight: 600; color: var(--text-muted); }
.stat-accent { color: var(--accent-brass); }
.stat-divider { width: 1px; height: 24px; background: var(--border); }

/* ── Filters ── */
.filter-bar {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px; margin-bottom: 18px; flex-wrap: wrap;
}
.filter-left { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.filter-chips { display: flex; gap: 5px; }

.chip {
  padding: 5px 12px; border-radius: 99px; border: 1px solid var(--border-strong);
  background: transparent; font-size: 12px; font-weight: 600; color: var(--text-secondary);
  cursor: pointer; transition: all 0.15s; font-family: inherit;
  display: flex; align-items: center; gap: 3px;
}
.chip:hover { border-color: var(--accent-brass-border); color: var(--text-primary); }
.chip.active { background: var(--accent-brass-glow); border-color: var(--accent-brass-border); color: var(--accent-brass); }
.chip-star { color: #fbbf24; font-size: 11px; }

.form-select-sm { padding: 6px 30px 6px 10px; font-size: 12px; }

/* Date range (reuse from OrdersView) */
.date-range-wrap { position: relative; }
.date-range-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 12px; border: 1px solid var(--border-strong); border-radius: var(--radius-sm);
  background: transparent; color: var(--text-secondary); font-size: 12px; font-weight: 500;
  font-family: inherit; cursor: pointer; transition: all 0.15s; white-space: nowrap;
}
.date-range-btn:hover { border-color: var(--accent-brass-border); color: var(--text-primary); }
.date-dropdown {
  position: absolute; top: calc(100% + 6px); right: 0;
  background: var(--bg-elevated); border: 1px solid var(--border-strong); border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg); z-index: 50; min-width: 280px; animation: ddIn 0.15s ease;
}
@keyframes ddIn { from { opacity: 0; transform: translateY(-4px); } }
.dd-presets { display: flex; gap: 4px; padding: 10px; border-bottom: 1px solid var(--border); }
.dd-preset {
  padding: 4px 10px; border-radius: 99px; border: 1px solid var(--border-strong);
  background: transparent; color: var(--text-secondary); font-size: 11px; font-weight: 600;
  font-family: inherit; cursor: pointer; transition: all 0.15s;
}
.dd-preset:hover { border-color: var(--accent-brass-border); color: var(--text-primary); }
.dd-preset.active { background: var(--accent-brass-glow); border-color: var(--accent-brass-border); color: var(--accent-brass); }
.dd-custom { padding: 10px; display: flex; align-items: flex-end; gap: 6px; flex-wrap: wrap; }
.dd-label { display: flex; flex-direction: column; gap: 3px; font-size: 10px; color: var(--text-muted); font-weight: 600; }
.dd-input {
  padding: 5px 8px; border: 1px solid var(--border-strong); border-radius: var(--radius-sm);
  background: var(--bg-input); color: var(--text-primary); font-family: inherit; font-size: 12px;
  outline: none; color-scheme: dark; width: 130px;
}
.dd-input:focus { border-color: var(--accent-brass); }
.dd-sep { color: var(--text-muted); font-size: 12px; padding-bottom: 5px; }
.dd-apply {
  padding: 5px 14px; border-radius: var(--radius-sm); border: none;
  background: var(--accent-brass); color: var(--text-inverse); font-size: 11px; font-weight: 700;
  font-family: inherit; cursor: pointer;
}

/* ── Rating distribution ── */
.rating-dist {
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: var(--radius-lg); padding: 16px 20px;
  margin-bottom: 18px; display: flex; flex-direction: column; gap: 6px;
}
.dist-row { display: flex; align-items: center; gap: 10px; }
.dist-stars { font-size: 11px; color: #fbbf24; width: 60px; letter-spacing: 1px; }
.dist-track { flex: 1; height: 6px; background: var(--bg-hover); border-radius: 3px; overflow: hidden; }
.dist-fill { height: 100%; background: var(--accent-brass); border-radius: 3px; transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1); }
.dist-count { font-size: 12px; font-weight: 600; color: var(--text-secondary); width: 32px; text-align: right; font-variant-numeric: tabular-nums; }

/* ── States ── */
.state-loading { display: flex; align-items: center; justify-content: center; gap: 10px; padding: 48px; color: var(--text-muted); font-size: 14px; }
.state-empty { text-align: center; padding: 48px; color: var(--text-muted); font-size: 14px; }

/* ── Review cards ── */
.rv-list { display: flex; flex-direction: column; gap: 8px; }

.rv-card {
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: var(--radius-lg); padding: 18px 20px;
  transition: box-shadow 0.15s;
}
.rv-card:hover { box-shadow: var(--shadow-md); }

.rv-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; gap: 8px; }
.rv-head-left { display: flex; align-items: center; gap: 10px; min-width: 0; }
.rv-stars { font-size: 13px; color: #fbbf24; letter-spacing: 0.5px; flex-shrink: 0; }
.rv-stars-off { color: var(--bg-hover); }
.rv-menu-badge {
  font-size: 12px; font-weight: 600; color: var(--accent-brass);
  background: var(--accent-brass-glow); padding: 2px 10px;
  border-radius: 4px; border: 1px solid var(--accent-brass-border);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.rv-head-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.rv-date { font-size: 12px; color: var(--text-muted); }
.rv-del-btn {
  width: 28px; height: 28px; border-radius: var(--radius-sm);
  border: 1px solid transparent; background: transparent;
  color: var(--text-muted); cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: all 0.15s;
}
.rv-del-btn:hover { color: var(--danger); border-color: rgba(200,120,100,0.3); background: var(--danger-bg); }

.rv-content { font-size: 14px; color: var(--text-secondary); line-height: 1.6; margin-bottom: 10px; }

.rv-meta {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding-top: 10px; border-top: 1px solid var(--border-subtle);
}
.rv-user { font-size: 12px; color: var(--text-muted); font-weight: 500; }
.rv-ai-tag {
  font-size: 10px; font-weight: 700; color: var(--accent-brass);
  background: var(--accent-brass-glow); padding: 1px 6px;
  border-radius: 3px; border: 1px solid var(--accent-brass-border);
}
.rv-order { font-size: 12px; color: var(--text-muted); margin-left: auto; }

/* ── Admin reply ── */
.rv-reply {
  margin-top: 12px; padding: 14px 16px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: var(--radius-md); border-left: 3px solid var(--accent-brass);
}
.rv-reply-head {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 700; color: var(--accent-brass);
  margin-bottom: 6px;
}
.rv-reply-date { font-weight: 500; color: var(--text-muted); margin-left: auto; }
.rv-reply-text { font-size: 13px; color: var(--text-secondary); line-height: 1.5; }
.rv-reply-edit {
  margin-top: 8px; background: none; border: none; color: var(--text-muted);
  font-size: 11px; cursor: pointer; font-family: inherit; padding: 0;
  text-decoration: underline; transition: color 0.15s;
}
.rv-reply-edit:hover { color: var(--accent-brass); }

.rv-reply-trigger { margin-top: 10px; }
.rv-reply-btn {
  display: flex; align-items: center; gap: 6px;
  background: none; border: 1px dashed var(--border-strong);
  border-radius: var(--radius-sm); padding: 8px 14px;
  color: var(--text-muted); font-size: 12px; font-weight: 600;
  font-family: inherit; cursor: pointer; transition: all 0.15s;
}
.rv-reply-btn:hover { border-color: var(--accent-brass-border); color: var(--accent-brass); }

.rv-reply-form { margin-top: 10px; display: flex; flex-direction: column; gap: 8px; }
.rv-reply-ta {
  width: 100%; padding: 10px 14px;
  background: var(--bg-input); border: 1px solid var(--border-strong);
  border-radius: var(--radius-md); color: var(--text-primary);
  font-family: inherit; font-size: 13px; resize: vertical;
  min-height: 60px; outline: none; line-height: 1.5;
}
.rv-reply-ta:focus { border-color: var(--accent-brass); box-shadow: 0 0 0 3px var(--accent-brass-glow); }
.rv-reply-actions { display: flex; gap: 8px; justify-content: flex-end; }
</style>
