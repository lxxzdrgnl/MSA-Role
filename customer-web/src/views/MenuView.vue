<template>
  <div class="layout">
    <!-- ═══ LEFT: Category Sidebar ═══ -->
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-mark">炎</div>
        <div>
          <div class="brand-sub">Rheon Kitchen</div>
          <div class="display brand-title">MENU</div>
        </div>
      </div>

      <div class="sidebar-search">
        <svg class="search-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input v-model="searchQuery" type="text" placeholder="검색..." class="search-field" @input="handleSearch" />
      </div>

      <nav class="cat-nav">
        <div class="cat-section-label">CATEGORIES</div>
        <button class="cat-item" :class="{ active: selectedCategory === null }" @click="selectCategory(null)">
          <span class="cat-icon">🍽</span><span>전체 메뉴</span>
          <span class="cat-count">{{ totalCount }}</span>
        </button>
        <button v-for="cat in categories" :key="cat.id" class="cat-item" :class="{ active: selectedCategory === cat.id }" @click="selectCategory(cat.id)">
          <span class="cat-icon">{{ categoryEmoji(cat.name) }}</span><span>{{ cat.name }}</span>
        </button>
      </nav>

      <div class="sidebar-footer">
        <button class="sidebar-link" @click="showProfileModal = true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          프로필
        </button>
        <button class="sidebar-link" :class="{ 'link-active': currentTab === 'orders' }" @click="currentTab = currentTab === 'orders' ? 'menu' : 'orders'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14,2 14,8 20,8"/></svg>
          주문내역
        </button>
        <button class="sidebar-link" @click="$emit('logout')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16,17 21,12 16,7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          로그아웃
        </button>
      </div>
    </aside>

    <!-- ═══ CENTER ═══ -->
    <main class="main-content">
      <!-- ORDER LIST TAB -->
      <template v-if="currentTab === 'orders'">
        <header class="main-header">
          <div>
            <h1 class="display main-title">Orders</h1>
            <p class="main-subtitle">주문 내역</p>
          </div>
        </header>
        <OrderList @select="openOrderDetail" />
      </template>

      <!-- MENU TAB -->
      <template v-else>
      <header class="main-header">
        <div>
          <h1 class="display main-title">{{ currentCategoryName }}</h1>
          <p class="main-subtitle">{{ menus.length }}개의 메뉴</p>
        </div>
      </header>

      <!-- Skeleton (initial load) -->
      <div v-if="loading && menus.length === 0" class="menu-grid">
        <div v-for="i in 8" :key="i" class="skeleton-card" :style="`animation-delay:${i * 0.05}s`">
          <div class="skel shimmer" style="height:180px;border-radius:var(--radius-lg) var(--radius-lg) 0 0"></div>
          <div style="padding:14px 16px 16px;display:flex;flex-direction:column;gap:6px">
            <div class="skel shimmer" style="height:10px;width:40%;border-radius:3px"></div>
            <div class="skel shimmer" style="height:16px;width:70%;border-radius:4px"></div>
            <div class="skel shimmer" style="height:12px;width:95%;border-radius:3px"></div>
            <div class="skel shimmer" style="height:12px;width:60%;border-radius:3px"></div>
            <div style="display:flex;gap:4px;margin-top:4px">
              <div class="skel shimmer" style="height:18px;width:44px;border-radius:4px"></div>
              <div class="skel shimmer" style="height:18px;width:36px;border-radius:4px"></div>
            </div>
            <div style="display:flex;align-items:center;justify-content:space-between;margin-top:10px;padding-top:10px;border-top:1px solid var(--border)">
              <div class="skel shimmer" style="height:18px;width:80px;border-radius:4px"></div>
              <div class="skel shimmer" style="height:32px;width:56px;border-radius:var(--radius-sm)"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Grid -->
      <div v-else-if="menus.length > 0" class="menu-grid" :class="{ 'grid-fade-out': gridFading }">
        <div v-for="(menu, i) in menus" :key="menu.id" class="menu-card fade-up" :class="{ 'sold-out': menu.isSoldOut }" :style="`animation-delay:${i * 0.03}s`" @click="openMenuDetail(menu)">
          <div class="card-visual">
            <img v-if="menu.imageUrl" :src="menu.imageUrl" :alt="menu.name" class="card-img" @error="e => e.target.style.display='none'" />
            <div v-else class="card-img-empty"><span>{{ categoryEmoji(menu.categoryName) }}</span></div>
            <div class="card-badges">
              <span v-if="menu.isBest" class="pill pill-best">BEST</span>
              <span v-if="menu.isSoldOut" class="pill pill-sold">품절</span>
              <span v-if="waitTimes[menu.id]" class="pill pill-wait">⏱ {{ waitTimes[menu.id] }}분</span>
            </div>
            <div v-if="menu.spicyLevel > 0" class="card-spicy"><span v-for="n in menu.spicyLevel" :key="n">🌶</span></div>
          </div>
          <div class="card-body">
            <div class="card-cat">{{ menu.categoryName }}</div>
            <div class="card-name">{{ menu.name }}</div>
            <div v-if="menu.description" class="card-desc">{{ menu.description }}</div>
            <div v-if="menu.tags" class="card-tags"><span v-for="tag in menu.tags.split(',')" :key="tag" class="chip">{{ tag.trim() }}</span></div>
            <div class="card-bottom">
              <span class="card-price mono">{{ formatPrice(menu.price) }}<small>원</small></span>
              <button class="btn-cart" :disabled="menu.isSoldOut" @click.stop="addToCart(menu)">{{ menu.isSoldOut ? '품절' : '담기' }}</button>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="!loading" class="empty"><span class="empty-glyph">∅</span><span>메뉴가 없습니다</span></div>
      </template>
    </main>

    <!-- Order Detail Modal -->
    <transition name="modal-fade">
      <OrderDetailModal v-if="selectedOrderId" :orderId="selectedOrderId" @close="selectedOrderId = null" />
    </transition>

    <!-- Menu Detail Modal -->
    <transition name="modal-fade">
      <MenuDetailModal v-if="selectedMenu" :menu="selectedMenu" @close="selectedMenu = null" @add-to-cart="(m) => { addToCart(m); selectedMenu = null }" />
    </transition>

    <!-- ═══ RIGHT: AI Chat Panel ═══ -->
    <ChatPanel :menus="menus" @open-menu="openMenuDetail" @add-to-cart="addToCart" />

    <!-- ═══ BOTTOM: Cart Bar ═══ -->
    <transition name="bar-slide">
      <div class="cart-bar" v-if="cartItems.length > 0">
        <div class="cart-bar-inner">
          <div class="cart-info">
            <span class="cart-count-badge">{{ cartTotalQty }}</span>
            <span class="cart-summary">{{ cartItemNames }}</span>
          </div>
          <div class="cart-right">
            <span class="cart-total mono">{{ formatPrice(cartTotalPrice) }}원</span>
            <button class="cart-order-btn" @click="showOrderModal = true">주문하기 →</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ═══ ORDER MODAL ════════════════════════════════════ -->
    <transition name="modal-fade">
      <div v-if="showOrderModal" class="modal-backdrop" @click.self="showOrderModal = false">
        <div class="om">
          <!-- Header -->
          <div class="om-head">
            <div class="om-head-left">
              <span class="om-tag">YOUR ORDER</span>
              <h3 class="display om-title">주문 확인</h3>
            </div>
            <button class="modal-x" @click="showOrderModal = false">✕</button>
          </div>

          <!-- Items -->
          <div class="om-items">
            <div v-for="(item, idx) in cartItems" :key="item.menuId" class="om-item" :style="{ animationDelay: `${idx * 50}ms` }">
              <div class="om-item-visual">
                <img v-if="item.imageUrl" :src="item.imageUrl" alt="" class="om-item-img" @error="e => { e.target.parentElement.classList.add('no-img'); e.target.style.display='none' }" />
                <div v-else class="om-item-emoji">🍽</div>
              </div>
              <div class="om-item-detail">
                <span class="om-item-name">{{ item.name }}</span>
                <span class="om-item-unit mono">{{ formatPrice(item.price) }}원</span>
              </div>
              <span class="om-item-total mono">{{ formatPrice(item.price * item.quantity) }}원</span>
              <div class="om-qty">
                <button class="om-qty-btn" @click="changeQty(item, -1)">
                  <svg width="12" height="12" viewBox="0 0 12 12"><path d="M3 6h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                </button>
                <span class="om-qty-num">{{ item.quantity }}</span>
                <button class="om-qty-btn" @click="changeQty(item, 1)">
                  <svg width="12" height="12" viewBox="0 0 12 12"><path d="M6 3v6M3 6h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                </button>
              </div>
            </div>
          </div>

          <!-- Footer -->
          <div class="om-footer">
            <div class="om-total">
              <span class="om-total-label">총 결제금액</span>
              <span class="om-total-price mono">{{ formatPrice(cartTotalPrice) }}<small>원</small></span>
            </div>
            <button class="om-submit" :disabled="ordering || cartItems.length === 0" @click="placeOrder">
              <span v-if="ordering" class="spinner"></span>
              <template v-else>
                <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M3 3h1.5l1 9h8l1.5-6H5.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="7" cy="15" r="1" fill="currentColor"/><circle cx="13" cy="15" r="1" fill="currentColor"/></svg>
                주문하기
              </template>
            </button>
            <div v-if="orderError" class="om-error">{{ orderError }}</div>
          </div>
        </div>
      </div>
    </transition>

    <!-- Profile Modal -->
    <transition name="modal-fade">
      <ProfileModal v-if="showProfileModal" @close="showProfileModal = false" @toast="showToast" />
    </transition>

    <!-- Toast -->
    <transition name="toast-pop">
      <div v-if="toastMsg" class="toast"><span class="toast-check">✓</span> {{ toastMsg }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import OrderList from '../components/OrderList.vue'
import OrderDetailModal from '../components/OrderDetailModal.vue'
import MenuDetailModal from '../components/MenuDetailModal.vue'
import ChatPanel from '../components/ChatPanel.vue'
import ProfileModal from '../components/ProfileModal.vue'
import { useFormatting } from '../composables/useFormatting'
import { useCart } from '../composables/useCart'
import { categoryEmoji } from '../constants'

const router = useRouter()
const emit = defineEmits(['cart-updated', 'logout'])
const { formatPrice } = useFormatting()
const cart = useCart()

// ── Tab ──
const currentTab = ref('menu')
const selectedOrderId = ref(null)
const selectedMenu = ref(null)
function openOrderDetail(id) { selectedOrderId.value = id }
function openMenuDetail(menu) { selectedMenu.value = menu }

// ── Data ──
const menus = ref([])
const categories = ref([])
const selectedCategory = ref(null)
const searchQuery = ref('')
const loading = ref(false)
const gridFading = ref(false)
const toastMsg = ref('')
let toastTimer = null

// ── Profile ──
const showProfileModal = ref(false)

// ── Cart ──
const cartItems = cart.cartItems
const showOrderModal = ref(false)
const ordering = ref(false)
const orderError = ref('')
const cartTotalQty = cart.totalQty
const cartTotalPrice = cart.totalPrice
const cartItemNames = cart.itemNames

const totalCount = computed(() => menus.value.length)
const currentCategoryName = computed(() => {
  if (selectedCategory.value === null) return 'All Menu'
  const cat = categories.value.find(c => c.id === selectedCategory.value)
  return cat ? cat.name : 'Menu'
})

// ── Wait times ──
const waitTimes = ref({})

async function loadWaitTimes() {
  for (const menu of menus.value) {
    try {
      const r = await api.get(`/operations/congestion/menu/${menu.id}`)
      if (r.data?.estimated_minutes) waitTimes.value[menu.id] = r.data.estimated_minutes
    } catch {}
  }
}

// ── Load ──
onMounted(() => { loadCategories(); loadMenus() })

async function loadCategories() {
  try { categories.value = (await api.get('/menus/categories')).data } catch {}
}

const FADE_MS = 150

function fadeOutGrid() {
  if (menus.value.length === 0) return Promise.resolve()
  gridFading.value = true
  return new Promise(resolve => setTimeout(resolve, FADE_MS))
}

async function loadMenus() {
  const isInitial = menus.value.length === 0
  if (isInitial) loading.value = true
  else await fadeOutGrid()

  try {
    const params = { page: 0, size: 50 }
    if (selectedCategory.value !== null) params.category = selectedCategory.value
    if (searchQuery.value) params.keyword = searchQuery.value
    const r = await api.get('/menus', { params })
    menus.value = r.data.content || r.data
    loadWaitTimes()
  } catch {} finally { loading.value = false; gridFading.value = false }
}

function selectCategory(id) { selectedCategory.value = id; currentTab.value = 'menu'; loadMenus() }

let searchTimer = null
function handleSearch() { clearTimeout(searchTimer); searchTimer = setTimeout(loadMenus, 400) }

// ── Cart actions ──
function addToCart(menu) {
  cart.addItem(menu)
  showToast(`${menu.name} 담았어요`)
}

function changeQty(item, delta) {
  cart.changeQty(item, delta)
  if (cartItems.value.length === 0) showOrderModal.value = false
}

function showToast(msg) {
  toastMsg.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastMsg.value = '' }, 2000)
}

// ── Order ──
async function placeOrder() {
  ordering.value = true
  orderError.value = ''
  try {
    const items = cartItems.value.map(i => ({ menuId: i.menuId, quantity: i.quantity }))
    await api.post('/orders', { items })
    cart.clear()
    showOrderModal.value = false
    showToast('주문이 완료되었습니다!')
  } catch (e) {
    orderError.value = e.response?.data?.message || '주문 중 오류가 발생했습니다.'
  } finally { ordering.value = false }
}

</script>

<style scoped>
/* ═══ Layout ═══ */
.layout {
  display: grid;
  grid-template-columns: var(--sidebar-w) 1fr var(--chat-w);
  min-height: 100vh;
  transition: grid-template-columns 0.3s ease;
}
.layout:has(.chat-panel.collapsed) {
  grid-template-columns: var(--sidebar-w) 1fr 48px;
}

/* ═══ LEFT SIDEBAR ═══ */
.sidebar {
  background: var(--bg-elevated);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
}
.sidebar-brand { padding: 28px 20px 20px; display: flex; align-items: center; gap: 12px; border-bottom: 1px solid var(--border); }
.brand-mark { width: 42px; height: 42px; background: var(--accent); border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; font-size: 20px; color: #fff; font-weight: 700; box-shadow: 0 2px 12px rgba(212,134,60,0.4); }
.brand-sub { font-size: 10px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.12em; }
.brand-title { font-size: 28px; color: var(--text-primary); margin: 0; }

.sidebar-search { padding: 16px 16px 0; position: relative; }
.search-ico { position: absolute; left: 28px; top: 50%; transform: translateY(-25%); width: 14px; height: 14px; color: var(--text-muted); }
.search-field { width: 100%; padding: 9px 12px 9px 36px; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-sm); color: var(--text-primary); font-family: inherit; font-size: 13px; outline: none; transition: var(--transition); }
.search-field::placeholder { color: var(--text-muted); }
.search-field:focus { border-color: var(--accent); }

.cat-nav { flex: 1; padding: 12px; display: flex; flex-direction: column; gap: 2px; }
.cat-section-label { font-size: 10px; font-weight: 700; color: var(--text-muted); letter-spacing: 0.14em; padding: 8px 8px 6px; }
.cat-item { display: flex; align-items: center; gap: 10px; padding: 9px 12px; border-radius: var(--radius-sm); font-size: 13px; font-weight: 500; color: var(--text-secondary); transition: var(--transition); width: 100%; text-align: left; }
.cat-item:hover { background: var(--bg-hover); color: var(--text-primary); }
.cat-item.active { background: var(--accent-bg); color: var(--accent-soft); font-weight: 700; border-left: 3px solid var(--accent); padding-left: 9px; }
.cat-icon { font-size: 16px; width: 22px; text-align: center; }
.cat-count { margin-left: auto; font-size: 11px; color: var(--text-muted); background: var(--bg-subtle); padding: 1px 7px; border-radius: 99px; }

.sidebar-footer { padding: 12px; border-top: 1px solid var(--border); display: flex; flex-direction: column; gap: 2px; }

.sidebar-link { display: flex; align-items: center; gap: 8px; padding: 8px 12px; font-size: 13px; color: var(--text-muted); border-radius: var(--radius-sm); transition: var(--transition); text-decoration: none; width: 100%; text-align: left; }
.sidebar-link:hover { color: var(--text-primary); background: var(--bg-hover); }
.sidebar-link.link-active { color: var(--accent-soft); background: var(--accent-bg); }

/* ═══ MAIN ═══ */
.main-content { padding: 32px 36px; padding-bottom: 100px; overflow-y: auto; min-height: 100vh; }
.main-header { margin-bottom: 28px; }
.main-title { font-size: 38px; color: var(--text-primary); margin-bottom: 4px; }
.main-subtitle { font-size: 13px; color: var(--text-muted); }

.menu-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 20px; }
.skeleton-card { border-radius: var(--radius-lg); overflow: hidden; background: var(--bg-card); border: 1px solid var(--border); animation: skeletonFadeIn 0.3s ease both; }
.skel { background: var(--bg-subtle); }
.shimmer { animation: pulse 2.4s ease-in-out infinite; }
@keyframes pulse { 0%, 100% { opacity: 0.6; } 50% { opacity: 1; } }
@keyframes skeletonFadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

.menu-grid { transition: opacity 0.15s ease-out, transform 0.15s ease-out; }
.grid-fade-out { opacity: 0; transform: translateY(4px); pointer-events: none; }

.menu-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; transition: var(--transition); }
.menu-card:hover { border-color: var(--border-hover); box-shadow: var(--shadow-glow); transform: translateY(-3px); }
.menu-card.sold-out { opacity: 0.45; pointer-events: none; }

.card-visual { position: relative; height: 180px; background: var(--bg-subtle); overflow: hidden; display: flex; align-items: center; justify-content: center; }
.card-img { width: 100%; height: 100%; object-fit: cover; }
.card-img-empty { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; font-size: 42px; opacity: 0.35; }

.card-badges { position: absolute; top: 10px; left: 10px; display: flex; gap: 5px; }
.card-spicy { position: absolute; bottom: 8px; right: 8px; font-size: 12px; }

.card-body { padding: 14px 16px 16px; display: flex; flex-direction: column; gap: 4px; }
.card-cat { font-size: 10px; color: var(--text-muted); font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; }
.card-name { font-size: 15px; font-weight: 700; color: var(--text-primary); line-height: 1.3; }
.card-desc { font-size: 12px; color: var(--text-secondary); line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }

.card-bottom { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; padding-top: 10px; border-top: 1px solid var(--border); }
.card-price { font-size: 17px; font-weight: 700; color: var(--accent-soft); }
.card-price small { font-size: 11px; font-weight: 400; margin-left: 1px; opacity: 0.7; }

.btn-cart { background: var(--accent); color: #fff; border: none; border-radius: var(--radius-sm); padding: 7px 16px; font-size: 13px; font-weight: 600; font-family: inherit; transition: var(--transition); cursor: pointer; }
.btn-cart:hover:not(:disabled) { background: var(--accent-soft); box-shadow: 0 2px 12px rgba(212,134,60,0.4); }
.btn-cart:disabled { background: var(--bg-subtle); color: var(--text-muted); cursor: not-allowed; }
.btn-cart-sm { padding: 4px 10px; font-size: 11px; }

.empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 24px; gap: 12px; color: var(--text-muted); }
.empty-glyph { font-size: 48px; opacity: 0.3; font-family: 'Playfair Display', serif; }

/* ═══ CART BAR ═══ */
.cart-bar { position: fixed; bottom: 0; left: var(--sidebar-w); right: 0; z-index: 200; padding: 0 24px; pointer-events: none; }
.cart-bar-inner { max-width: 900px; margin: 0 auto 16px; background: rgba(26,25,24,0.95); backdrop-filter: blur(16px); border: 1px solid var(--border-hover); border-radius: var(--radius-lg); padding: 14px 20px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 8px 32px rgba(0,0,0,0.5), 0 0 0 1px rgba(212,134,60,0.15); pointer-events: auto; }
.cart-info { display: flex; align-items: center; gap: 12px; }
.cart-count-badge { background: var(--accent); color: #fff; font-size: 12px; font-weight: 700; width: 28px; height: 28px; border-radius: 99px; display: flex; align-items: center; justify-content: center; }
.cart-summary { font-size: 13px; color: var(--text-secondary); max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cart-right { display: flex; align-items: center; gap: 16px; }
.cart-total { font-size: 16px; font-weight: 700; color: var(--text-primary); }
.cart-order-btn { background: var(--accent); color: #fff; padding: 9px 20px; border-radius: var(--radius-md); font-size: 13px; font-weight: 700; font-family: inherit; transition: var(--transition); cursor: pointer; border: none; }
.cart-order-btn:hover { background: var(--accent-soft); box-shadow: 0 4px 16px rgba(212,134,60,0.4); }

.bar-slide-enter-active { animation: slideUp 0.3s ease; }
.bar-slide-leave-active { animation: slideUp 0.2s ease reverse; }
@keyframes slideUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }

/* ═══ ORDER MODAL ═══ */
.om {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  width: 480px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 32px 80px rgba(0,0,0,0.7), 0 0 0 1px rgba(255,255,255,0.03) inset;
  animation: omIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes omIn { from { opacity: 0; transform: translateY(20px) scale(0.97); } to { opacity: 1; transform: translateY(0) scale(1); } }

.om-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 24px 28px 18px;
  border-bottom: 1px solid var(--border);
}
.om-head-left { display: flex; flex-direction: column; gap: 2px; }
.om-tag {
  font-size: 11px; font-weight: 700; letter-spacing: 0.14em;
  color: var(--accent); text-transform: uppercase;
}
.om-title { font-size: 22px; color: var(--text-primary); margin: 0; font-weight: 700; letter-spacing: -0.5px; }

/* Items */
.om-items {
  flex: 1; overflow-y: auto; padding: 8px 12px;
}

.om-item {
  display: flex; align-items: center; gap: 14px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  transition: background 0.15s;
  animation: omItemIn 0.35s cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes omItemIn { from { opacity: 0; transform: translateX(-8px); } to { opacity: 1; transform: translateX(0); } }

.om-item:hover { background: rgba(255,255,255,0.02); }

.om-item-visual {
  width: 60px; height: 60px; border-radius: 12px; overflow: hidden;
  flex-shrink: 0; position: relative;
  background: var(--bg-subtle);
  border: 1px solid var(--border);
}
.om-item-img {
  width: 100%; height: 100%; object-fit: cover;
  display: block;
}
.om-item-emoji {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; opacity: 0.5;
}
.om-item-visual.no-img .om-item-emoji { display: flex; }

.om-item-detail {
  flex: 1; min-width: 0;
  display: flex; flex-direction: column; gap: 3px;
}
.om-item-name {
  font-size: 14px; font-weight: 600; color: var(--text-primary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.om-item-unit { font-size: 13px; color: var(--text-muted); }

.om-item-total {
  font-size: 14px; font-weight: 700; color: var(--accent-soft);
  flex-shrink: 0; min-width: 70px; text-align: right;
}

.om-qty {
  display: flex; align-items: center;
  background: var(--bg-subtle);
  border: 1px solid var(--border);
  border-radius: 8px; overflow: hidden;
}
.om-qty-btn {
  width: 30px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  background: transparent; border: none;
  color: var(--text-secondary); cursor: pointer;
  transition: var(--transition);
}
.om-qty-btn:hover { background: var(--accent-dim); color: var(--accent); }
.om-qty-num {
  width: 28px; text-align: center;
  font-size: 13px; font-weight: 700; color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
/* Footer */
.om-footer {
  padding: 20px 28px 24px;
  border-top: 1px solid var(--border);
  background: linear-gradient(to top, rgba(212,134,60,0.03), transparent);
}

.om-total {
  display: flex; align-items: baseline; justify-content: space-between;
  margin-bottom: 16px;
}
.om-total-label { font-size: 14px; color: var(--text-secondary); font-weight: 500; }
.om-total-price {
  font-size: 22px; font-weight: 800; color: var(--accent-soft);
  letter-spacing: -0.5px; line-height: 1;
}
.om-total-price small {
  font-size: 14px; font-weight: 500; color: var(--text-muted);
  margin-left: 2px; letter-spacing: 0;
}

.om-submit {
  width: 100%; padding: 15px;
  background: var(--accent); color: #fff;
  border: none; border-radius: var(--radius-md);
  font-size: 15px; font-weight: 700; font-family: inherit;
  cursor: pointer; transition: all 0.2s;
  display: flex; align-items: center; justify-content: center; gap: 10px;
  box-shadow: 0 2px 16px rgba(212,134,60,0.25);
  letter-spacing: -0.2px;
}
.om-submit:hover:not(:disabled) {
  background: var(--accent-soft);
  box-shadow: 0 6px 28px rgba(212,134,60,0.4);
  transform: translateY(-1px);
}
.om-submit:active:not(:disabled) { transform: translateY(0); }
.om-submit:disabled { opacity: 0.45; cursor: not-allowed; transform: none; box-shadow: none; }

.om-error {
  margin-top: 12px; padding: 10px 14px;
  background: rgba(248,113,113,0.08); border: 1px solid rgba(248,113,113,0.15);
  border-radius: var(--radius-sm); font-size: 13px; color: #f87171;
}

/* ═══ TOAST ═══ */
.toast { position: fixed; bottom: 90px; left: 50%; transform: translateX(-50%); background: rgba(26,25,24,0.95); border: 1px solid var(--border); backdrop-filter: blur(12px); color: var(--text-primary); padding: 10px 20px; border-radius: 99px; font-size: 13px; font-weight: 500; z-index: 500; display: flex; align-items: center; gap: 7px; box-shadow: 0 4px 20px rgba(0,0,0,0.5); }
.toast-check { color: var(--accent); font-weight: 700; }
.toast-pop-enter-active { animation: toastIn 0.25s ease; }
.toast-pop-leave-active { animation: toastIn 0.2s ease reverse; }
@keyframes toastIn { from { opacity: 0; transform: translateX(-50%) translateY(10px) scale(0.95); } to { opacity: 1; transform: translateX(-50%) translateY(0) scale(1); } }

</style>
