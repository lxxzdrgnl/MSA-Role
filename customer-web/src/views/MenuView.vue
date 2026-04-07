<template>
  <div class="menu-page page-wrapper">
    <!-- Hero header -->
    <header class="menu-header">
      <div class="header-top">
        <div class="header-brand">
          <span class="header-fire">🔥</span>
          <div>
            <p class="header-sub">Seoul Kitchen</p>
            <h1 class="display header-title">MENU</h1>
          </div>
        </div>
        <button class="btn-ai-chip" @click="showAiModal = true">
          <span class="ai-sparkle">✦</span> AI 추천
        </button>
      </div>

      <!-- Search -->
      <div class="search-wrap">
        <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input v-model="searchQuery" type="text" placeholder="메뉴 검색..." class="search-input" @input="handleSearch" />
      </div>

      <!-- Category tabs -->
      <div class="cat-strip">
        <button class="cat-pill" :class="{ active: selectedCategory === null }" @click="selectCategory(null)">전체</button>
        <button
          v-for="cat in categories" :key="cat.id"
          class="cat-pill" :class="{ active: selectedCategory === cat.id }"
          @click="selectCategory(cat.id)"
        >{{ cat.name }}</button>
      </div>
    </header>

    <!-- Skeleton loading -->
    <div v-if="loading" class="menu-grid">
      <div v-for="i in 6" :key="i" class="menu-skeleton">
        <div class="skeleton" style="height:156px;border-radius:14px 14px 0 0"></div>
        <div style="padding:12px;display:flex;flex-direction:column;gap:8px">
          <div class="skeleton" style="height:14px;width:70%"></div>
          <div class="skeleton" style="height:11px;width:40%"></div>
          <div class="skeleton" style="height:11px;width:85%"></div>
          <div class="skeleton" style="height:11px;width:60%"></div>
        </div>
      </div>
    </div>

    <!-- Menu grid -->
    <div v-else-if="menus.length > 0" class="menu-grid">
      <div
        v-for="(menu, i) in menus" :key="menu.id"
        class="menu-card fade-up"
        :class="{ 'is-sold-out': menu.isSoldOut }"
        :style="`animation-delay:${i * 0.04}s`"
      >
        <div class="menu-img-wrap">
          <img v-if="menu.imageUrl" :src="menu.imageUrl" :alt="menu.name" class="menu-img"
            @error="e => e.target.style.display='none'" />
          <div v-else class="menu-img-placeholder">
            <span class="placeholder-emoji">{{ categoryEmoji(menu.categoryName) }}</span>
          </div>

          <!-- Badges -->
          <div class="badge-row">
            <span v-if="menu.isBestSeller" class="badge badge-best">🏆 BEST</span>
            <span v-if="menu.isSoldOut" class="badge badge-sold">품절</span>
          </div>

          <!-- Spicy indicator -->
          <div v-if="menu.spicyLevel > 0" class="spicy-row">
            <span v-for="n in menu.spicyLevel" :key="n" class="chili">🌶</span>
          </div>
        </div>

        <div class="menu-body">
          <div class="menu-cat">{{ menu.categoryName }}</div>
          <div class="menu-name">{{ menu.name }}</div>
          <div v-if="menu.description" class="menu-desc">{{ menu.description }}</div>

          <!-- Tags -->
          <div v-if="menu.tags" class="menu-tags">
            <span v-for="tag in menu.tags.split(',')" :key="tag" class="tag">{{ tag.trim() }}</span>
          </div>

          <div class="menu-footer">
            <span class="menu-price">{{ formatPrice(menu.price) }}<span class="price-unit">원</span></span>
            <button
              class="btn-add"
              :class="{ 'is-disabled': menu.isSoldOut }"
              :disabled="menu.isSoldOut"
              @click="addToCart(menu)"
            >
              <span v-if="!menu.isSoldOut">+</span>
              {{ menu.isSoldOut ? '품절' : '담기' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else class="empty-state">
      <span class="empty-icon">🍽</span>
      <span class="empty-label">메뉴가 없습니다</span>
    </div>

    <!-- Toast -->
    <transition name="toast-slide">
      <div v-if="toastMsg" class="toast">
        <span class="toast-icon">✓</span> {{ toastMsg }}
      </div>
    </transition>

    <!-- AI Modal -->
    <transition name="modal-fade">
      <div v-if="showAiModal" class="modal-backdrop" @click.self="showAiModal = false">
        <div class="ai-modal">
          <div class="ai-modal-header">
            <div>
              <p class="ai-modal-label">AI POWERED</p>
              <h3 class="display ai-modal-title">RECOMMEND</h3>
            </div>
            <button class="modal-close" @click="showAiModal = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <div class="ai-modal-body">
            <textarea
              v-model="aiQuery"
              rows="3"
              class="input ai-textarea"
              placeholder="어떤 음식이 드시고 싶으세요? 오늘 기분, 먹고 싶은 스타일을 말씀해주세요"
            ></textarea>

            <div v-if="aiError" class="alert-error">{{ aiError }}</div>

            <button class="btn btn-primary ai-submit-btn" :disabled="aiLoading || !aiQuery.trim()" @click="getRecommendations">
              <span v-if="aiLoading" class="spinner"></span>
              {{ aiLoading ? '분석 중...' : '추천 받기' }}
            </button>

            <div v-if="aiResults.length > 0" class="ai-results">
              <div v-for="rec in aiResults" :key="rec.menuId" class="ai-result">
                <div class="ai-result-top">
                  <span class="ai-result-name">{{ rec.menuName }}</span>
                  <button class="btn-add ai-add-btn" @click="addToCartById(rec.menuId, rec.menuName)">담기</button>
                </div>
                <p class="ai-result-reason">{{ rec.reason }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const emit = defineEmits(['cart-updated'])
const menus = ref([])
const categories = ref([])
const selectedCategory = ref(null) // stores category ID (Long) or null for all
const searchQuery = ref('')
const loading = ref(false)
const toastMsg = ref('')
let toastTimer = null

const showAiModal = ref(false)
const aiQuery = ref('')
const aiLoading = ref(false)
const aiResults = ref([])
const aiError = ref('')

const EMOJI_MAP = { '한식': '🥢', '중식': '🥡', '일식': '🍣', '분식': '🍢', '음료': '🧋', '디저트': '🍰' }
function categoryEmoji(name) { return EMOJI_MAP[name] || '🍴' }

onMounted(() => { loadCategories(); loadMenus() })

async function loadCategories() {
  try { const r = await api.get('/menus/categories'); categories.value = r.data } catch {}
}

async function loadMenus() {
  loading.value = true
  try {
    const params = { page: 0, size: 50 }
    if (selectedCategory.value !== null) params.category = selectedCategory.value
    if (searchQuery.value) params.keyword = searchQuery.value
    const r = await api.get('/menus', { params })
    menus.value = r.data.content || r.data
  } catch {} finally { loading.value = false }
}

function selectCategory(id) { selectedCategory.value = id; loadMenus() }

let searchTimer = null
function handleSearch() { clearTimeout(searchTimer); searchTimer = setTimeout(loadMenus, 400) }

function formatPrice(price) { return Number(price).toLocaleString('ko-KR') }

function getCart() {
  try { return JSON.parse(localStorage.getItem('cart') || '[]') } catch { return [] }
}
function saveCart(cart) { localStorage.setItem('cart', JSON.stringify(cart)); emit('cart-updated') }

function addToCart(menu) {
  if (menu.isSoldOut) return
  const cart = getCart()
  const idx = cart.findIndex(i => i.menuId === menu.id)
  idx >= 0 ? cart[idx].quantity++ : cart.push({ menuId: menu.id, name: menu.name, price: menu.price, quantity: 1 })
  saveCart(cart)
  showToast(`${menu.name} 담았어요!`)
}

function addToCartById(menuId, menuName) {
  const menu = menus.value.find(m => m.id === menuId)
  if (menu) { addToCart(menu); return }
  const cart = getCart()
  const idx = cart.findIndex(i => i.menuId === menuId)
  idx >= 0 ? cart[idx].quantity++ : cart.push({ menuId, name: menuName, price: 0, quantity: 1 })
  saveCart(cart)
  showToast(`${menuName} 담았어요!`)
}

function showToast(msg) {
  toastMsg.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastMsg.value = '' }, 2200)
}

async function getRecommendations() {
  aiError.value = ''; aiResults.value = []; aiLoading.value = true
  try {
    const r = await api.post('/recommendations/chat', { message: aiQuery.value })
    aiResults.value = r.data.recommendations || r.data.menus || []
    if (!aiResults.value.length) aiError.value = '추천 결과가 없습니다. 다른 키워드를 입력해보세요.'
  } catch (e) {
    aiError.value = e.response?.data?.message || 'AI 추천 중 오류가 발생했습니다.'
  } finally { aiLoading.value = false }
}
</script>

<style scoped>
.menu-page { padding-bottom: 88px; }

/* ── Header ────────────────────────────────────────────── */
.menu-header {
  padding: 24px 20px 0;
  position: sticky;
  top: 0;
  background: var(--bg-base);
  z-index: 100;
  padding-bottom: 0;
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.header-brand { display: flex; align-items: center; gap: 10px; }
.header-fire { font-size: 28px; filter: drop-shadow(0 0 12px rgba(232,92,30,0.5)); }
.header-sub { font-size: 10px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.1em; }
.header-title { font-size: 36px; color: var(--text-primary); line-height: 1; margin: 0; }

.btn-ai-chip {
  display: flex;
  align-items: center;
  gap: 5px;
  background: rgba(232,92,30,0.12);
  border: 1px solid rgba(232,92,30,0.3);
  color: var(--accent-soft);
  border-radius: 99px;
  padding: 7px 14px;
  font-size: 13px;
  font-weight: 600;
  transition: var(--transition);
  font-family: inherit;
}
.btn-ai-chip:hover {
  background: rgba(232,92,30,0.2);
  border-color: var(--accent);
  box-shadow: 0 0 16px rgba(232,92,30,0.2);
}
.ai-sparkle { font-style: normal; }

/* ── Search ─────────────────────────────────────────────── */
.search-wrap {
  position: relative;
  margin-bottom: 14px;
}
.search-icon {
  position: absolute;
  left: 14px; top: 50%;
  transform: translateY(-50%);
  width: 16px; height: 16px;
  color: var(--text-muted);
}
.search-input {
  width: 100%;
  padding: 11px 14px 11px 40px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-family: inherit;
  font-size: 14px;
  outline: none;
  transition: var(--transition);
}
.search-input::placeholder { color: var(--text-muted); }
.search-input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-dim); }

/* ── Category strip ─────────────────────────────────────── */
.cat-strip {
  display: flex;
  gap: 7px;
  overflow-x: auto;
  padding-bottom: 14px;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}
.cat-strip::-webkit-scrollbar { display: none; }

.cat-pill {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 99px;
  padding: 6px 16px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  white-space: nowrap;
  transition: var(--transition);
  font-family: inherit;
}
.cat-pill:hover { border-color: var(--border-hover); color: var(--text-primary); }
.cat-pill.active {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
  font-weight: 700;
  box-shadow: 0 2px 12px rgba(232,92,30,0.35);
}

/* ── Grid ───────────────────────────────────────────────── */
.menu-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 0 16px;
}

/* ── Card ───────────────────────────────────────────────── */
.menu-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: var(--transition);
  cursor: default;
}
.menu-card:hover {
  border-color: var(--border-hover);
  box-shadow: 0 4px 24px rgba(232,92,30,0.15);
  transform: translateY(-2px);
}
.menu-card.is-sold-out { opacity: 0.5; }

.menu-img-wrap {
  position: relative;
  height: 130px;
  background: var(--bg-subtle);
  overflow: hidden;
}
.menu-img {
  width: 100%; height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}
.menu-card:hover .menu-img { transform: scale(1.05); }
.menu-img-placeholder {
  width: 100%; height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.placeholder-emoji { font-size: 36px; opacity: 0.5; }

.badge-row { position: absolute; top: 8px; left: 8px; display: flex; gap: 4px; }
.badge { display: inline-flex; align-items: center; padding: 2px 7px; border-radius: 99px; font-size: 10px; font-weight: 700; }
.badge-best { background: rgba(232,92,30,0.9); color: #fff; backdrop-filter: blur(4px); }
.badge-sold { background: rgba(0,0,0,0.6); color: var(--text-secondary); }

.spicy-row { position: absolute; bottom: 6px; right: 6px; font-size: 11px; }

.menu-body { padding: 10px 12px 12px; display: flex; flex-direction: column; gap: 3px; }
.menu-cat { font-size: 10px; color: var(--text-muted); font-weight: 500; letter-spacing: 0.05em; }
.menu-name { font-size: 14px; font-weight: 700; color: var(--text-primary); line-height: 1.25; }
.menu-desc {
  font-size: 11px;
  color: var(--text-secondary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.menu-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 2px; }
.tag {
  background: var(--bg-subtle);
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 10px;
  color: var(--text-muted);
}

.menu-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.menu-price { font-size: 15px; font-weight: 800; color: var(--accent-soft); }
.price-unit { font-size: 11px; font-weight: 500; margin-left: 1px; }

.btn-add {
  background: var(--accent);
  color: #fff;
  border: none;
  border-radius: 99px;
  padding: 5px 12px;
  font-size: 12px;
  font-weight: 700;
  transition: var(--transition);
  font-family: inherit;
}
.btn-add:hover:not(.is-disabled) {
  background: var(--accent-soft);
  box-shadow: 0 2px 10px rgba(232,92,30,0.4);
  transform: scale(1.05);
}
.btn-add.is-disabled { background: var(--bg-subtle); color: var(--text-muted); cursor: not-allowed; }

/* Skeleton */
.menu-skeleton { border-radius: var(--radius-lg); overflow: hidden; background: var(--bg-card); border: 1px solid var(--border); }

/* ── Toast ──────────────────────────────────────────────── */
.toast {
  position: fixed;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(31,29,24,0.95);
  border: 1px solid var(--border-hover);
  backdrop-filter: blur(12px);
  color: var(--text-primary);
  padding: 10px 20px;
  border-radius: 99px;
  font-size: 13px;
  font-weight: 500;
  z-index: 400;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 7px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.4);
}
.toast-icon { color: var(--accent); font-style: normal; font-weight: 700; }

.toast-slide-enter-active { animation: toastIn 0.25s ease; }
.toast-slide-leave-active { animation: toastIn 0.2s ease reverse; }
@keyframes toastIn {
  from { opacity: 0; transform: translateX(-50%) translateY(12px); }
  to   { opacity: 1; transform: translateX(-50%) translateY(0); }
}

/* ── AI Modal ───────────────────────────────────────────── */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 300;
  padding: 0;
}

.ai-modal {
  width: 100%;
  max-width: 480px;
  background: var(--bg-elevated);
  border-top: 1px solid var(--border);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  max-height: 80vh;
  overflow-y: auto;
}

.ai-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 24px 0;
}
.ai-modal-label { font-size: 10px; color: var(--accent); font-weight: 700; letter-spacing: 0.12em; text-transform: uppercase; }
.ai-modal-title { font-size: 32px; color: var(--text-primary); margin: 0; }

.modal-close {
  background: var(--bg-subtle);
  border: 1px solid var(--border);
  border-radius: 99px;
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-secondary);
  transition: var(--transition);
}
.modal-close:hover { color: var(--text-primary); border-color: var(--border-hover); }

.ai-modal-body { padding: 20px 24px 32px; display: flex; flex-direction: column; gap: 12px; }
.ai-textarea { resize: none; }
.ai-submit-btn { width: 100%; font-weight: 700; }

.ai-results { display: flex; flex-direction: column; gap: 10px; border-top: 1px solid var(--border); padding-top: 16px; }
.ai-result {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 12px 14px;
}
.ai-result-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.ai-result-name { font-size: 14px; font-weight: 700; color: var(--text-primary); }
.ai-add-btn { font-size: 11px; padding: 4px 10px; }
.ai-result-reason { font-size: 12px; color: var(--text-secondary); line-height: 1.5; }

.spinner {
  width: 16px; height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.25s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
</style>
