<template>
  <div class="page menus-page">
    <div class="page-header">
      <h1 class="page-title">메뉴 관리</h1>
      <div class="header-actions">
        <button class="btn btn-secondary" @click="showCategoryModal = true">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M1.5 3.5L7 1L12.5 3.5V10.5L7 13L1.5 10.5V3.5Z" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/>
            <path d="M7 6V13M1.5 3.5L7 6L12.5 3.5" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/>
          </svg>
          카테고리
        </button>
        <button class="btn btn-primary" @click="openMenuForm(null)">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M7 2V12M2 7H12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          메뉴 추가
        </button>
      </div>
    </div>

    <!-- Category Tabs -->
    <div class="cat-tabs-wrap">
      <div class="cat-tabs">
        <button
          class="cat-tab"
          :class="{ active: selectedCategory === '' }"
          @click="selectCategory('')"
        >전체</button>
        <button
          v-for="cat in categories"
          :key="cat.id"
          class="cat-tab"
          :class="{ active: selectedCategory === cat.id }"
          @click="selectCategory(cat.id)"
        >
          {{ cat.name }}
          <span class="cat-tab-count" v-if="cat.menuCount">{{ cat.menuCount }}</span>
        </button>
      </div>
    </div>

    <div v-if="error" class="alert-box alert-error">{{ error }}</div>
    <div v-if="successMsg" class="alert-box alert-success">{{ successMsg }}</div>

    <!-- Menu Table -->
    <div class="card table-wrap">
      <table>
        <thead>
          <tr>
            <th class="col-img">이미지</th>
            <th>메뉴명</th>
            <th>카테고리</th>
            <th>가격</th>
            <th>품절</th>
            <th>베스트</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="state-cell"><div class="loading-spinner"></div>불러오는 중...</td>
          </tr>
          <tr v-else-if="menus.length === 0">
            <td colspan="7" class="state-cell">등록된 메뉴가 없습니다.</td>
          </tr>
          <tr v-for="menu in menus" :key="menu.id">
            <td>
              <img v-if="menu.imageUrl" :src="menu.imageUrl" alt="" class="menu-thumb" @error="menu.imageUrl = null"/>
              <div v-else class="menu-thumb-ph">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="1" y="1" width="14" height="14" rx="3" stroke="currentColor" stroke-width="1.2"/><circle cx="5.5" cy="5.5" r="1.5" stroke="currentColor" stroke-width="1.2"/><path d="M1 11L4.5 7.5L7.5 10.5L10 8L15 13" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </div>
            </td>
            <td class="cell-name">{{ menu.name }}</td>
            <td><span class="cat-badge">{{ menu.categoryName || getCategoryName(menu.categoryId) }}</span></td>
            <td class="cell-price">{{ formatPrice(menu.price) }}</td>
            <td>
              <button
                class="status-toggle"
                :class="menu.isSoldOut ? 'st-soldout' : 'st-available'"
                @click="handleToggleSoldOut(menu)"
                :title="menu.isSoldOut ? '클릭하면 판매 재개' : '클릭하면 품절 처리'"
              >
                <span class="st-dot"></span>
                {{ menu.isSoldOut ? '품절' : '판매중' }}
                <svg class="st-icon" width="10" height="10" viewBox="0 0 10 10" fill="none">
                  <path d="M2 5h6M5 2l3 3-3 3" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
            </td>
            <td>
              <button
                class="status-toggle"
                :class="menu.isBest ? 'st-best' : 'st-normal'"
                @click="handleToggleBestSeller(menu)"
                :title="menu.isBest ? '클릭하면 베스트 해제' : '클릭하면 베스트 지정'"
              >
                <span class="st-dot"></span>
                {{ menu.isBest ? '베스트' : '일반' }}
                <svg class="st-icon" width="10" height="10" viewBox="0 0 10 10" fill="none">
                  <path d="M2 5h6M5 2l3 3-3 3" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
            </td>
            <td>
              <div class="action-group">
                <button class="btn btn-secondary btn-sm" @click="openMenuForm(menu)">수정</button>
                <button class="btn btn-danger btn-sm" @click="handleDelete(menu)">삭제</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Infinite scroll sentinel -->
    <div ref="sentinel" class="scroll-sentinel">
      <div v-if="loadingMore" class="loading-more">
        <div class="loading-spinner"></div>
        불러오는 중...
      </div>
    </div>

    <!-- ═══ Menu Form Modal ═══ -->
    <ModalWrapper :open="showMenuForm" :title="editMenuId ? '메뉴 수정' : '메뉴 추가'" size="lg" @close="closeMenuForm">
      <form @submit.prevent="handleMenuSubmit">
            <div class="form-grid">
              <div class="form-group">
                <label class="form-label">카테고리 *</label>
                <select v-model="mf.categoryId" class="form-select" required>
                  <option value="">선택하세요</option>
                  <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">메뉴명 *</label>
                <input v-model="mf.name" class="form-input" placeholder="메뉴 이름" required/>
              </div>
              <div class="form-group">
                <label class="form-label">가격 (원) *</label>
                <input v-model.number="mf.price" type="number" min="0" class="form-input" placeholder="0" required/>
              </div>
              <div class="form-group">
                <label class="form-label">맵기 (0~5)</label>
                <input v-model.number="mf.spicyLevel" type="number" min="0" max="5" class="form-input"/>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">설명</label>
              <textarea v-model="mf.description" class="form-textarea" rows="2" placeholder="메뉴 설명"></textarea>
            </div>
            <div class="form-grid">
              <div class="form-group">
                <label class="form-label">태그 (쉼표 구분)</label>
                <input v-model="mf.tags" class="form-input" placeholder="매운, 인기"/>
              </div>
              <div class="form-group">
                <label class="form-label">알레르기 정보</label>
                <input v-model="mf.allergens" class="form-input" placeholder="대두, 밀"/>
              </div>
              <div class="form-group">
                <label class="form-label">조리 시간 (분)</label>
                <input v-model.number="mf.cookTimeMinutes" type="number" min="1" class="form-input"/>
              </div>
              <div class="form-group">
                <label class="form-label">이미지</label>
                <div class="img-upload">
                  <div v-if="mfPreview || mf.imageUrl" class="img-preview">
                    <img :src="mfPreview || mf.imageUrl" alt=""/>
                    <button type="button" class="img-remove" @click="removeMenuImage">
                      <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M3 3L9 9M9 3L3 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                    </button>
                  </div>
                  <label v-else class="img-trigger">
                    <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M9 4V14M4 9H14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                    업로드
                    <input type="file" accept="image/*" @change="onMenuFileChange" hidden/>
                  </label>
                </div>
              </div>
            </div>
            <div v-if="mfError" class="alert-box alert-error">{{ mfError }}</div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeMenuForm">취소</button>
              <button type="submit" class="btn btn-primary" :disabled="mfLoading">
                {{ mfLoading ? '처리 중...' : (editMenuId ? '수정' : '추가') }}
              </button>
            </div>
      </form>
    </ModalWrapper>

    <!-- ═══ Category Modal ═══ -->
    <CategoryModal :open="showCategoryModal" :categories="categories" @close="showCategoryModal = false" @updated="fetchCategories" />

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import api from '../api.js'
import { getMenus, getCategories, toggleSoldOut, toggleBestSeller, deleteMenu } from '../api.js'
import { formatPrice, extractListData } from '../utils/formatters.js'
import { useAsync } from '../composables/useAsync.js'
import ModalWrapper from '../components/ModalWrapper.vue'
import CategoryModal from '../components/CategoryModal.vue'

const { loading, error, run } = useAsync()
const { loading: mfLoading, error: mfError, run: mfRun } = useAsync()

const menus = ref([])
const categories = ref([])
const successMsg = ref('')
const selectedCategory = ref('')
const page = ref(0)
const totalPages = ref(1)
const loadingMore = ref(false)
const sentinel = ref(null)
let observer = null

// ── Menu form modal ──
const showMenuForm = ref(false)
const editMenuId = ref(null)
const mfPreview = ref('')
const mfFile = ref(null)
const mf = ref({ categoryId: '', name: '', price: '', description: '', tags: '', allergens: '', spicyLevel: 0, cookTimeMinutes: 15, imageUrl: '' })

// ── Category modal ──
const showCategoryModal = ref(false)


// ── Helpers ──
function getCategoryName(id) { return categories.value.find(c => c.id === id)?.name ?? '—' }
function showOk(msg) { successMsg.value = msg; setTimeout(() => successMsg.value = '', 3000) }

// ── Menu list ──
function selectCategory(id) {
  selectedCategory.value = id
  page.value = 0
  menus.value = []
  fetchMenus()
}

async function fetchMenus() {
  await run(async () => {
    const params = { page: page.value, size: 20 }
    if (selectedCategory.value) params.category = selectedCategory.value
    const { list, totalPages: tp } = extractListData(await getMenus(params))
    menus.value = list
    totalPages.value = tp
  }, '메뉴 목록을 불러오지 못했습니다.')
}

async function fetchMoreMenus() {
  if (loadingMore.value || page.value >= totalPages.value - 1) return
  loadingMore.value = true
  try {
    page.value++
    const params = { page: page.value, size: 20 }
    if (selectedCategory.value) params.category = selectedCategory.value
    const { list, totalPages: tp } = extractListData(await getMenus(params))
    menus.value = [...menus.value, ...list]
    totalPages.value = tp
  } finally {
    loadingMore.value = false
  }
}

async function fetchCategories() {
  try { categories.value = (await getCategories()).data || [] } catch {}
}

function setupObserver() {
  observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting) fetchMoreMenus()
  }, { threshold: 0.1 })
  if (sentinel.value) observer.observe(sentinel.value)
}

async function handleToggleSoldOut(m) {
  await run(async () => { const v = !m.isSoldOut; await toggleSoldOut(m.id, v); m.isSoldOut = v }, '품절 상태 변경 실패')
}
async function handleToggleBestSeller(m) {
  await run(async () => { const v = !m.isBest; await toggleBestSeller(m.id, v); m.isBest = v }, '베스트셀러 변경 실패')
}
async function handleDelete(m) {
  if (!confirm(`"${m.name}" 메뉴를 삭제하시겠습니까?`)) return
  await run(async () => { await deleteMenu(m.id); showOk(`"${m.name}" 삭제됨`); fetchMenus() }, '삭제 실패')
}

// ── Menu form modal ──
function openMenuForm(menu) {
  editMenuId.value = menu?.id || null
  mfPreview.value = ''; mfFile.value = null; mfError.value = ''
  mf.value = menu
    ? { categoryId: menu.categoryId, name: menu.name, price: menu.price, description: menu.description || '', tags: menu.tags || '', allergens: menu.allergens || '', spicyLevel: menu.spicyLevel || 0, cookTimeMinutes: menu.cookTimeMinutes || 15, imageUrl: menu.imageUrl || '' }
    : { categoryId: selectedCategory.value || '', name: '', price: '', description: '', tags: '', allergens: '', spicyLevel: 0, cookTimeMinutes: 15, imageUrl: '' }
  showMenuForm.value = true
}
function closeMenuForm() { showMenuForm.value = false }
function onMenuFileChange(e) { const f = e.target.files[0]; if (!f) return; mfFile.value = f; mfPreview.value = URL.createObjectURL(f) }
function removeMenuImage() { mfFile.value = null; mfPreview.value = ''; mf.value.imageUrl = '' }

async function handleMenuSubmit() {
  if (!mf.value.categoryId) { mfError.value = '카테고리를 선택하세요.'; return }
  await mfRun(async () => {
    const fd = new FormData()
    for (const [k, v] of Object.entries(mf.value)) { if (k !== 'imageUrl') fd.append(k, v) }
    if (mfFile.value) fd.append('image', mfFile.value)
    if (editMenuId.value) await api.put(`/menus/${editMenuId.value}`, fd)
    else await api.post('/menus', fd)
    closeMenuForm(); fetchMenus()
  }, '저장 실패')
}

onMounted(() => { fetchCategories(); fetchMenus(); setupObserver() })
onUnmounted(() => { observer?.disconnect() })
</script>

<style scoped>
.menus-page {
  animation: page-enter 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes page-enter {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title { font-size: 28px; font-weight: 700; color: var(--text-primary); letter-spacing: -0.5px; }
.header-actions { display: flex; gap: 10px; }

/* ── Category Tabs ── */
.cat-tabs-wrap {
  margin-bottom: 22px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}
.cat-tabs-wrap::-webkit-scrollbar { display: none; }

.cat-tabs {
  display: flex;
  gap: 6px;
  min-width: max-content;
}

.cat-tab {
  padding: 9px 20px;
  border-radius: 20px;
  border: 1px solid var(--border);
  background: var(--bg-surface);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: inherit;
  display: flex;
  align-items: center;
  gap: 7px;
  white-space: nowrap;
}

.cat-tab:hover {
  border-color: var(--accent-brass-border);
  color: var(--text-primary);
}

.cat-tab.active {
  background: var(--accent-brass);
  border-color: var(--accent-brass);
  color: var(--text-inverse);
}

.cat-tab-count {
  font-size: 11px;
  font-weight: 600;
  opacity: 0.7;
}

/* ── Table ── */
.state-cell {
  text-align: center;
  padding: 48px;
  color: var(--text-muted);
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.col-img { width: 72px; text-align: center; }

.menu-thumb {
  width: 56px; height: 56px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--border);
  display: block;
  margin: 0 auto;
}

.menu-thumb-ph {
  width: 56px; height: 56px;
  border-radius: 8px;
  background: var(--bg-hover);
  border: 1px dashed var(--border-strong);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  margin: 0 auto;
}

.cell-name { font-weight: 600; color: var(--text-primary); }
.cell-price { font-weight: 600; font-variant-numeric: tabular-nums; }

.cat-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  background: var(--accent-brass-glow);
  color: var(--accent-brass);
}

/* ── Status Toggle ── */
.status-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px 6px 10px;
  border-radius: 6px;
  border: 1px solid transparent;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: inherit;
  white-space: nowrap;
}

.status-toggle:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
}

.status-toggle:active {
  transform: translateY(0);
}

.st-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.st-icon {
  opacity: 0.6;
  flex-shrink: 0;
}

.st-available {
  background: rgba(34, 197, 94, 0.12);
  border-color: rgba(34, 197, 94, 0.3);
  color: #4ade80;
}
.st-available .st-dot { background: #4ade80; }

.st-soldout {
  background: var(--status-danger-bg);
  border-color: rgba(239, 68, 68, 0.3);
  color: var(--status-danger-text);
}
.st-soldout .st-dot { background: var(--status-danger-text); }

.st-best {
  background: var(--accent-brass-glow);
  border-color: var(--accent-brass-border);
  color: var(--accent-brass);
}
.st-best .st-dot { background: var(--accent-brass); }

.st-normal {
  background: var(--bg-hover);
  border-color: var(--border);
  color: var(--text-muted);
}
.st-normal .st-dot { background: var(--text-muted); }

.action-group { display: flex; gap: 6px; }

/* ── Menu Form specifics ── */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--border);
}

.img-upload { display: flex; align-items: flex-start; gap: 10px; }

.img-preview {
  position: relative;
  width: 80px; height: 60px;
}
.img-preview img {
  width: 100%; height: 100%;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--border);
}
.img-remove {
  position: absolute; top: -6px; right: -6px;
  width: 22px; height: 22px;
  border-radius: 50%; border: none;
  background: var(--danger); color: #fff;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  box-shadow: var(--shadow-sm);
}

.img-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: 1px dashed var(--border-strong);
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-muted);
  font-size: 13px;
  transition: all var(--transition-fast);
}
.img-trigger:hover {
  border-color: var(--accent-brass-border);
  color: var(--accent-brass);
}

/* ── Infinite scroll ── */
.scroll-sentinel {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-more {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 13px;
}
</style>
