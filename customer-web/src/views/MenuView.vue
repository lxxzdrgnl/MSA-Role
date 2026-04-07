<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-title">메뉴</h1>
      <div class="header-actions">
        <div class="search-box">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="메뉴 검색..."
            class="search-input"
            @input="handleSearch"
          />
        </div>
        <button class="btn-ai" @click="showAiModal = true">
          ✨ AI 추천
        </button>
      </div>
    </div>

    <!-- Category tabs -->
    <div class="category-tabs">
      <button
        class="tab-btn"
        :class="{ active: selectedCategory === '' }"
        @click="selectCategory('')"
      >
        전체
      </button>
      <button
        v-for="cat in categories"
        :key="cat.id"
        class="tab-btn"
        :class="{ active: selectedCategory === cat.name }"
        @click="selectCategory(cat.name)"
      >
        {{ cat.name }}
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading">불러오는 중...</div>

    <!-- Menu grid -->
    <div v-else-if="menus.length > 0" class="menu-grid">
      <div
        v-for="menu in menus"
        :key="menu.id"
        class="menu-card"
        :class="{ 'sold-out': menu.isSoldOut }"
      >
        <div class="menu-img-wrap">
          <img
            v-if="menu.imageUrl"
            :src="menu.imageUrl"
            :alt="menu.name"
            class="menu-img"
            @error="(e) => e.target.style.display='none'"
          />
          <div v-else class="menu-img-placeholder">🍴</div>
          <span v-if="menu.isBestSeller" class="badge best">베스트</span>
          <span v-if="menu.isSoldOut" class="badge soldout">품절</span>
        </div>
        <div class="menu-info">
          <div class="menu-name">{{ menu.name }}</div>
          <div class="menu-category">{{ menu.categoryName }}</div>
          <div v-if="menu.description" class="menu-desc">{{ menu.description }}</div>
          <div class="menu-footer">
            <span class="menu-price">{{ formatPrice(menu.price) }}원</span>
            <button
              class="btn-add"
              :disabled="menu.isSoldOut"
              @click="addToCart(menu)"
            >
              {{ menu.isSoldOut ? '품절' : '담기' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else class="empty-state">
      <p>메뉴가 없습니다.</p>
    </div>

    <!-- Toast -->
    <div v-if="toastMsg" class="toast">{{ toastMsg }}</div>

    <!-- AI Recommendation Modal -->
    <div v-if="showAiModal" class="modal-overlay" @click.self="showAiModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3>✨ AI 메뉴 추천</h3>
          <button class="modal-close" @click="showAiModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>어떤 음식이 드시고 싶으세요?</label>
            <textarea
              v-model="aiQuery"
              rows="3"
              placeholder="예: 오늘 기분 좋은 날인데 특별한 메뉴 추천해줘"
              class="ai-textarea"
            />
          </div>
          <div v-if="aiError" class="error-msg">{{ aiError }}</div>
          <button class="btn-primary" :disabled="aiLoading || !aiQuery.trim()" @click="getRecommendations">
            {{ aiLoading ? '추천 중...' : '추천받기' }}
          </button>

          <!-- Results -->
          <div v-if="aiResults.length > 0" class="ai-results">
            <h4 class="ai-results-title">추천 메뉴</h4>
            <div v-for="rec in aiResults" :key="rec.menuId" class="ai-result-item">
              <div class="ai-result-header">
                <span class="ai-result-name">{{ rec.menuName }}</span>
                <button
                  class="btn-add-small"
                  @click="addToCartById(rec.menuId, rec.menuName)"
                >
                  담기
                </button>
              </div>
              <p class="ai-result-reason">{{ rec.reason }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const emit = defineEmits(['cart-updated'])
const router = useRouter()

const menus = ref([])
const categories = ref([])
const selectedCategory = ref('')
const searchQuery = ref('')
const loading = ref(false)

const toastMsg = ref('')
let toastTimer = null

// AI modal
const showAiModal = ref(false)
const aiQuery = ref('')
const aiLoading = ref(false)
const aiResults = ref([])
const aiError = ref('')

onMounted(() => {
  loadCategories()
  loadMenus()
})

async function loadCategories() {
  try {
    const res = await api.get('/menus/categories')
    categories.value = res.data
  } catch (err) {
    console.error('카테고리 로드 실패', err)
  }
}

async function loadMenus() {
  loading.value = true
  try {
    const params = { page: 0, size: 50 }
    if (selectedCategory.value) params.category = selectedCategory.value
    if (searchQuery.value) params.search = searchQuery.value
    const res = await api.get('/menus', { params })
    menus.value = res.data.content || res.data
  } catch (err) {
    console.error('메뉴 로드 실패', err)
  } finally {
    loading.value = false
  }
}

function selectCategory(name) {
  selectedCategory.value = name
  loadMenus()
}

let searchTimer = null
function handleSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadMenus, 400)
}

function formatPrice(price) {
  return Number(price).toLocaleString('ko-KR')
}

function getCart() {
  try {
    return JSON.parse(localStorage.getItem('cart') || '[]')
  } catch {
    return []
  }
}

function saveCart(cart) {
  localStorage.setItem('cart', JSON.stringify(cart))
  emit('cart-updated')
}

function addToCart(menu) {
  if (menu.isSoldOut) return
  const cart = getCart()
  const idx = cart.findIndex((i) => i.menuId === menu.id)
  if (idx >= 0) {
    cart[idx].quantity += 1
  } else {
    cart.push({ menuId: menu.id, name: menu.name, price: menu.price, quantity: 1 })
  }
  saveCart(cart)
  showToast(`"${menu.name}" 장바구니에 추가됨`)
}

function addToCartById(menuId, menuName) {
  const menu = menus.value.find((m) => m.id === menuId)
  if (menu) {
    addToCart(menu)
  } else {
    // menu not in current list, add with minimal info
    const cart = getCart()
    const idx = cart.findIndex((i) => i.menuId === menuId)
    if (idx >= 0) {
      cart[idx].quantity += 1
    } else {
      cart.push({ menuId, name: menuName, price: 0, quantity: 1 })
    }
    saveCart(cart)
    showToast(`"${menuName}" 장바구니에 추가됨`)
  }
}

function showToast(msg) {
  toastMsg.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastMsg.value = '' }, 2000)
}

async function getRecommendations() {
  aiError.value = ''
  aiResults.value = []
  aiLoading.value = true
  try {
    const res = await api.post('/recommendations', { query: aiQuery.value })
    aiResults.value = res.data.menus || []
    if (aiResults.value.length === 0) {
      aiError.value = '추천 결과가 없습니다. 다른 키워드를 입력해보세요.'
    }
  } catch (err) {
    aiError.value = err.response?.data?.message || 'AI 추천 중 오류가 발생했습니다.'
  } finally {
    aiLoading.value = false
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #222;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 8px 14px;
  font-size: 14px;
  outline: none;
  width: 200px;
  transition: border-color 0.15s;
}

.search-input:focus {
  border-color: #e05c2a;
}

.btn-ai {
  background: linear-gradient(135deg, #7c3aed, #5b21b6);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
  white-space: nowrap;
}

.btn-ai:hover {
  opacity: 0.88;
}

.category-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
  margin-bottom: 20px;
  -webkit-overflow-scrolling: touch;
}

.tab-btn {
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 20px;
  padding: 6px 16px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
  color: #555;
}

.tab-btn:hover {
  border-color: #e05c2a;
  color: #e05c2a;
}

.tab-btn.active {
  background: #e05c2a;
  border-color: #e05c2a;
  color: #fff;
  font-weight: 600;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.menu-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.07);
  transition: transform 0.15s, box-shadow 0.15s;
}

.menu-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}

.menu-card.sold-out {
  opacity: 0.6;
}

.menu-img-wrap {
  position: relative;
  height: 150px;
  background: #f8f0ec;
  overflow: hidden;
}

.menu-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.menu-img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
}

.badge {
  position: absolute;
  top: 8px;
  left: 8px;
  border-radius: 6px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 700;
}

.badge.best {
  background: #e05c2a;
  color: #fff;
}

.badge.soldout {
  background: #555;
  color: #fff;
  left: auto;
  right: 8px;
}

.menu-info {
  padding: 12px;
}

.menu-name {
  font-size: 15px;
  font-weight: 700;
  color: #222;
  margin-bottom: 2px;
}

.menu-category {
  font-size: 11px;
  color: #999;
  margin-bottom: 6px;
}

.menu-desc {
  font-size: 12px;
  color: #777;
  margin-bottom: 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.menu-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.menu-price {
  font-size: 15px;
  font-weight: 700;
  color: #e05c2a;
}

.btn-add {
  background: #e05c2a;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-add:hover:not(:disabled) {
  background: #c74d20;
}

.btn-add:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #aaa;
  font-size: 15px;
}

/* Toast */
.toast {
  position: fixed;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  background: #333;
  color: #fff;
  padding: 10px 22px;
  border-radius: 24px;
  font-size: 14px;
  z-index: 999;
  animation: fadeIn 0.2s ease;
  white-space: nowrap;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateX(-50%) translateY(10px); }
  to   { opacity: 1; transform: translateX(-50%) translateY(0); }
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  padding: 20px;
}

.modal {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 480px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 8px 40px rgba(0,0,0,0.2);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: #222;
}

.modal-close {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #888;
  padding: 4px;
  line-height: 1;
}

.modal-close:hover {
  color: #333;
}

.modal-body {
  padding: 20px 24px 24px;
}

.ai-textarea {
  width: 100%;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  resize: vertical;
  outline: none;
  font-family: inherit;
  transition: border-color 0.15s;
}

.ai-textarea:focus {
  border-color: #7c3aed;
}

.ai-results {
  margin-top: 20px;
  border-top: 1px solid #eee;
  padding-top: 16px;
}

.ai-results-title {
  font-size: 14px;
  font-weight: 700;
  color: #555;
  margin-bottom: 12px;
}

.ai-result-item {
  background: #faf7ff;
  border: 1px solid #ede9fe;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 10px;
}

.ai-result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.ai-result-name {
  font-size: 14px;
  font-weight: 700;
  color: #222;
}

.btn-add-small {
  background: #7c3aed;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-add-small:hover {
  background: #5b21b6;
}

.ai-result-reason {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}
</style>
