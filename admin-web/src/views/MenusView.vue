<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">메뉴 관리</h1>
      <router-link to="/menus/new" class="btn btn-primary">+ 메뉴 추가</router-link>
    </div>

    <!-- Filters -->
    <div class="filter-row">
      <select v-model="selectedCategory" class="form-select filter-select" @change="fetchMenus">
        <option value="">전체 카테고리</option>
        <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
      </select>
    </div>

    <div v-if="error" class="alert-box alert-error">{{ error }}</div>
    <div v-if="successMsg" class="alert-box alert-success">{{ successMsg }}</div>

    <div class="card table-wrap">
      <table>
        <thead>
          <tr>
            <th>이미지</th>
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
            <td colspan="7" class="loading-cell">불러오는 중...</td>
          </tr>
          <tr v-else-if="menus.length === 0">
            <td colspan="7" class="empty-cell">등록된 메뉴가 없습니다.</td>
          </tr>
          <tr v-for="menu in menus" :key="menu.id">
            <td>
              <img
                v-if="menu.imageUrl"
                :src="menu.imageUrl"
                alt="메뉴 이미지"
                class="menu-thumb"
                @error="(e) => e.target.style.display='none'"
              />
              <div v-else class="menu-thumb-placeholder">없음</div>
            </td>
            <td class="menu-name">{{ menu.name }}</td>
            <td>
              <span class="tag tag-blue">{{ menu.categoryName || getCategoryName(menu.categoryId) }}</span>
            </td>
            <td class="price">{{ formatPrice(menu.price) }}</td>
            <td>
              <button
                class="toggle-btn"
                :class="menu.isSoldOut ? 'toggle-on' : 'toggle-off'"
                @click="handleToggleSoldOut(menu)"
              >
                {{ menu.isSoldOut ? '품절' : '판매중' }}
              </button>
            </td>
            <td>
              <button
                class="toggle-btn"
                :class="menu.isBestSeller ? 'toggle-on toggle-best' : 'toggle-off'"
                @click="handleToggleBestSeller(menu)"
              >
                {{ menu.isBestSeller ? '⭐ 베스트' : '일반' }}
              </button>
            </td>
            <td>
              <div class="action-group">
                <router-link :to="`/menus/${menu.id}/edit`" class="btn btn-secondary btn-sm">수정</router-link>
                <button class="btn btn-danger btn-sm" @click="handleDelete(menu)">삭제</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
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
import { ref, onMounted } from 'vue'
import { getMenus, getCategories, toggleSoldOut, toggleBestSeller, deleteMenu } from '../api.js'

const menus = ref([])
const categories = ref([])
const loading = ref(false)
const error = ref('')
const successMsg = ref('')
const selectedCategory = ref('')
const page = ref(0)
const totalPages = ref(1)

function formatPrice(v) {
  if (v == null) return '-'
  return Number(v).toLocaleString('ko-KR') + '원'
}

function getCategoryName(id) {
  const cat = categories.value.find((c) => c.id === id)
  return cat ? cat.name : '-'
}

function showSuccess(msg) {
  successMsg.value = msg
  setTimeout(() => { successMsg.value = '' }, 3000)
}

async function fetchMenus() {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: 20 }
    if (selectedCategory.value) params.category = selectedCategory.value
    const res = await getMenus(params)
    const data = res.data
    if (Array.isArray(data)) {
      menus.value = data
      totalPages.value = 1
    } else {
      menus.value = data.content || data.menus || []
      totalPages.value = data.totalPages || 1
    }
  } catch {
    error.value = '메뉴 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch {
    // non-critical
  }
}

function changePage(p) {
  page.value = p
  fetchMenus()
}

async function handleToggleSoldOut(menu) {
  try {
    const newVal = !menu.isSoldOut
    await toggleSoldOut(menu.id, newVal)
    menu.isSoldOut = newVal
  } catch {
    error.value = '품절 상태 변경에 실패했습니다.'
  }
}

async function handleToggleBestSeller(menu) {
  try {
    const newVal = !menu.isBestSeller
    await toggleBestSeller(menu.id, newVal)
    menu.isBestSeller = newVal
  } catch {
    error.value = '베스트셀러 상태 변경에 실패했습니다.'
  }
}

async function handleDelete(menu) {
  if (!confirm(`"${menu.name}" 메뉴를 삭제하시겠습니까?`)) return
  try {
    await deleteMenu(menu.id)
    showSuccess(`"${menu.name}" 메뉴가 삭제되었습니다.`)
    fetchMenus()
  } catch {
    error.value = '메뉴 삭제에 실패했습니다.'
  }
}

onMounted(() => {
  fetchCategories()
  fetchMenus()
})
</script>

<style scoped>
.filter-row {
  margin-bottom: 20px;
}

.filter-select { max-width: 200px; }

.loading-cell, .empty-cell {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
  font-size: 14px;
}

.menu-thumb {
  width: 52px;
  height: 52px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.menu-thumb-placeholder {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #9ca3af;
}

.menu-name { font-weight: 600; color: #1a1d23; }
.price { font-weight: 600; }

.toggle-btn {
  padding: 4px 10px;
  border-radius: 6px;
  border: none;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.toggle-off { background: #f3f4f6; color: #6b7280; }
.toggle-off:hover { background: #e5e7eb; }
.toggle-on { background: #fee2e2; color: #991b1b; }
.toggle-on:hover { background: #fecaca; }
.toggle-best { background: #fef9c3; color: #854d0e; }
.toggle-best:hover { background: #fef08a; }

.action-group {
  display: flex;
  gap: 6px;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
}

.page-info { font-size: 14px; color: #6b7280; }
</style>
