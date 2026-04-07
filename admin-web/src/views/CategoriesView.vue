<template>
  <div class="page">
    <h2 class="page-title">카테고리 관리</h2>

    <div class="add-form">
      <input v-model="newName" placeholder="카테고리 이름" @keydown.enter="addCategory" />
      <button class="btn-primary" @click="addCategory" :disabled="!newName.trim()">추가</button>
    </div>

    <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

    <table class="table">
      <thead>
        <tr><th>ID</th><th>이름</th><th>정렬 순서</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="cat in categories" :key="cat.id">
          <td>{{ cat.id }}</td>
          <td>{{ cat.name }}</td>
          <td>{{ cat.sortOrder ?? cat.sort_order ?? 0 }}</td>
          <td>
            <button class="btn-danger-sm" @click="deleteCategory(cat.id)">삭제</button>
          </td>
        </tr>
        <tr v-if="categories.length === 0">
          <td colspan="4" class="empty">카테고리가 없습니다.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const categories = ref([])
const newName = ref('')
const errorMsg = ref('')

async function load() {
  const res = await api.get('/menus/categories')
  categories.value = res.data
}

async function addCategory() {
  if (!newName.value.trim()) return
  errorMsg.value = ''
  try {
    await api.post('/menus/categories', { name: newName.value.trim() })
    newName.value = ''
    await load()
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '추가 실패'
  }
}

async function deleteCategory(id) {
  if (!confirm('삭제하시겠습니까?')) return
  try {
    await api.delete(`/menus/categories/${id}`)
    await load()
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '삭제 실패'
  }
}

onMounted(load)
</script>

<style scoped>
.page { padding: 24px; }
.page-title { font-size: 22px; font-weight: 700; margin-bottom: 24px; }

.add-form { display: flex; gap: 10px; margin-bottom: 16px; }
.add-form input { flex: 1; max-width: 300px; padding: 9px 14px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; }
.add-form input:focus { outline: none; border-color: #4a6cf7; }

.table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.table th, .table td { padding: 11px 16px; text-align: left; font-size: 14px; border-bottom: 1px solid #f0f0f0; }
.table th { background: #f8f8f8; font-weight: 600; color: #555; }
.empty { color: #aaa; text-align: center; }

.btn-danger-sm { padding: 4px 10px; background: #dc3545; color: #fff; border: none; border-radius: 6px; font-size: 12px; cursor: pointer; }
.btn-danger-sm:hover { background: #c82333; }

.error-msg { background: #f8d7da; border: 1px solid #f5c2c7; border-radius: 8px; padding: 10px 14px; color: #842029; font-size: 13px; margin-bottom: 12px; }
</style>
