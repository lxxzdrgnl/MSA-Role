<template>
  <div class="page">
    <div class="page-header">
      <button class="btn-back" @click="$router.push('/menus')">← 목록</button>
      <h2>{{ isEdit ? '메뉴 수정' : '메뉴 추가' }}</h2>
    </div>

    <form class="form-card" @submit.prevent="handleSubmit">
      <div class="form-group">
        <label>카테고리 *</label>
        <select v-model="form.categoryId" required>
          <option value="">선택하세요</option>
          <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
        </select>
      </div>

      <div class="form-group">
        <label>메뉴명 *</label>
        <input v-model="form.name" placeholder="메뉴 이름" required />
      </div>

      <div class="form-group">
        <label>가격 (원) *</label>
        <input v-model.number="form.price" type="number" min="0" placeholder="0" required />
      </div>

      <div class="form-group">
        <label>설명</label>
        <textarea v-model="form.description" rows="3" placeholder="메뉴 설명"></textarea>
      </div>

      <div class="form-row">
        <div class="form-group">
          <label>태그 (쉼표 구분)</label>
          <input v-model="form.tags" placeholder="매운, 인기, 구이" />
        </div>
        <div class="form-group">
          <label>알레르기 정보</label>
          <input v-model="form.allergens" placeholder="대두, 밀, 달걀" />
        </div>
      </div>

      <div class="form-row">
        <div class="form-group">
          <label>맵기 (0~5)</label>
          <input v-model.number="form.spicyLevel" type="number" min="0" max="5" />
        </div>
        <div class="form-group">
          <label>조리 시간 (분)</label>
          <input v-model.number="form.cookTimeMinutes" type="number" min="1" />
        </div>
      </div>

      <div class="form-group">
        <label>이미지</label>
        <input type="file" accept="image/*" @change="onFileChange" />
        <img v-if="previewUrl" :src="previewUrl" class="preview-img" />
        <img v-else-if="isEdit && form.imageUrl" :src="form.imageUrl" class="preview-img" />
      </div>

      <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

      <div class="form-actions">
        <button type="button" class="btn-secondary" @click="$router.push('/menus')">취소</button>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '처리 중...' : (isEdit ? '수정' : '추가') }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const categories = ref([])
const loading = ref(false)
const errorMsg = ref('')
const previewUrl = ref('')
const imageFile = ref(null)

const form = ref({
  categoryId: '',
  name: '',
  price: '',
  description: '',
  tags: '',
  allergens: '',
  spicyLevel: 0,
  cookTimeMinutes: 15,
  imageUrl: '',
})

function onFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  imageFile.value = file
  previewUrl.value = URL.createObjectURL(file)
}

async function loadCategories() {
  const res = await api.get('/menus/categories')
  categories.value = res.data
}

async function loadMenu() {
  if (!isEdit.value) return
  const res = await api.get(`/menus/${route.params.id}`)
  const m = res.data
  form.value = {
    categoryId: m.categoryId,
    name: m.name,
    price: m.price,
    description: m.description || '',
    tags: m.tags || '',
    allergens: m.allergens || '',
    spicyLevel: m.spicyLevel || 0,
    cookTimeMinutes: m.cookTimeMinutes || 15,
    imageUrl: m.imageUrl || '',
  }
}

async function handleSubmit() {
  if (!form.value.categoryId) { errorMsg.value = '카테고리를 선택하세요.'; return }
  loading.value = true
  errorMsg.value = ''

  try {
    const fd = new FormData()
    fd.append('categoryId', form.value.categoryId)
    fd.append('name', form.value.name)
    fd.append('price', form.value.price)
    fd.append('description', form.value.description)
    fd.append('tags', form.value.tags)
    fd.append('allergens', form.value.allergens)
    fd.append('spicyLevel', form.value.spicyLevel)
    fd.append('cookTimeMinutes', form.value.cookTimeMinutes)
    if (imageFile.value) fd.append('image', imageFile.value)

    if (isEdit.value) {
      await api.put(`/menus/${route.params.id}`, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    } else {
      await api.post('/menus', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    }

    router.push('/menus')
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '저장 실패'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadCategories()
  await loadMenu()
})
</script>

<style scoped>
.page { padding: 24px; max-width: 700px; }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.page-header h2 { margin: 0; font-size: 20px; }
.btn-back { background: none; border: none; color: #4a6cf7; cursor: pointer; font-size: 14px; }

.form-card { background: #fff; border-radius: 12px; padding: 28px; box-shadow: 0 2px 8px rgba(0,0,0,0.07); }
.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 18px; }
.form-group label { font-size: 13px; font-weight: 600; color: #555; }
.form-group input, .form-group select, .form-group textarea {
  padding: 9px 14px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px;
}
.form-group input:focus, .form-group select:focus, .form-group textarea:focus {
  outline: none; border-color: #4a6cf7;
}
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.preview-img { margin-top: 8px; width: 160px; height: 120px; object-fit: cover; border-radius: 8px; border: 1px solid #eee; }

.error-msg { background: #f8d7da; border: 1px solid #f5c2c7; border-radius: 8px; padding: 10px 14px; color: #842029; font-size: 13px; margin-bottom: 12px; }

.form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 8px; }
</style>
