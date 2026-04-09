<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="profile-modal">
      <div class="profile-modal-head">
        <h3 class="display profile-modal-title">Profile</h3>
        <button class="modal-x" @click="$emit('close')">✕</button>
      </div>
      <div class="profile-modal-body">
        <div class="profile-field">
          <label class="profile-label">닉네임</label>
          <div v-if="!editing" class="profile-value-row">
            <span class="profile-value">{{ nickname }}</span>
            <button class="profile-edit-btn" @click="startEdit">변경</button>
          </div>
          <form v-else class="profile-edit-form" @submit.prevent="save">
            <input v-model="input" class="profile-edit-input" maxlength="20" ref="inputRef" @keydown.esc="editing = false" />
            <button type="submit" class="profile-save-btn" :disabled="!input.trim()">저장</button>
            <button type="button" class="profile-cancel-btn" @click="editing = false">취소</button>
          </form>
        </div>
        <div class="profile-field">
          <label class="profile-label">이메일</label>
          <span class="profile-value">{{ email }}</span>
        </div>
      </div>

      <div class="profile-reviews-section">
        <div class="profile-reviews-header">
          <span class="profile-label">내 리뷰</span>
          <span class="profile-review-count">{{ reviews.length }}개</span>
        </div>
        <div v-if="reviewsLoading" class="profile-reviews-empty">불러오는 중...</div>
        <div v-else-if="reviews.length === 0" class="profile-reviews-empty">작성한 리뷰가 없습니다</div>
        <div v-else class="profile-reviews-list">
          <div v-for="rv in reviews" :key="rv.id" class="profile-review-card">
            <div class="profile-review-top">
              <span class="profile-review-menu">{{ rv.menuName }}</span>
              <span class="profile-review-stars">{{ '★'.repeat(rv.rating) }}{{ '☆'.repeat(5 - rv.rating) }}</span>
            </div>
            <p class="profile-review-content">{{ rv.content }}</p>
            <span class="profile-review-date">{{ new Date(rv.createdAt).toLocaleDateString('ko-KR') }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import api from '../api'

const emit = defineEmits(['close', 'toast'])

const nickname = ref('')
const email = ref('')
const editing = ref(false)
const input = ref('')
const inputRef = ref(null)
const reviews = ref([])
const reviewsLoading = ref(true)

function startEdit() {
  input.value = nickname.value
  editing.value = true
  nextTick(() => inputRef.value?.focus())
}

async function save() {
  const name = input.value.trim()
  if (!name) return
  try {
    const r = await api.patch('/auth/profile/nickname', { nickname: name })
    nickname.value = r.data.nickname
    editing.value = false
    emit('toast', '닉네임이 변경되었습니다')
  } catch {
    emit('toast', '변경에 실패했습니다')
  }
}

onMounted(async () => {
  try {
    const r = await api.get('/auth/profile')
    nickname.value = r.data.nickname
    email.value = r.data.email
  } catch {}
  try {
    const r = await api.get('/reviews/my')
    reviews.value = r.data.content || r.data
  } catch { reviews.value = [] }
  finally { reviewsLoading.value = false }
})
</script>

<style scoped>
.profile-modal { background: var(--bg-elevated); border: 1px solid var(--border); border-radius: var(--radius-xl); width: 480px; max-height: 80vh; overflow-y: auto; box-shadow: 0 24px 64px rgba(0,0,0,0.6); }
.profile-modal-head { display: flex; align-items: center; justify-content: space-between; padding: 24px 24px 16px; border-bottom: 1px solid var(--border); }
.profile-modal-title { font-size: 28px; color: var(--text-primary); margin: 0; }
.profile-modal-body { padding: 24px; display: flex; flex-direction: column; gap: 20px; }
.profile-field { width: 100%; }
.profile-label { font-size: 11px; font-weight: 600; color: var(--text-muted); letter-spacing: 0.06em; text-transform: uppercase; margin-bottom: 6px; display: block; }
.profile-value-row { display: flex; align-items: center; justify-content: space-between; }
.profile-value { font-size: 14px; color: var(--text-primary); }
.profile-edit-btn { background: none; border: 1px solid var(--border); border-radius: var(--radius-sm); color: var(--text-secondary); cursor: pointer; padding: 4px 12px; font-size: 11px; font-family: inherit; transition: var(--transition); }
.profile-edit-btn:hover { color: var(--accent); border-color: var(--accent); }
.profile-edit-form { display: flex; align-items: center; gap: 6px; }
.profile-edit-input { flex: 1; padding: 6px 10px; background: var(--bg-subtle); border: 1px solid var(--border); border-radius: var(--radius-sm); color: var(--text-primary); font-size: 13px; font-family: inherit; outline: none; }
.profile-edit-input:focus { border-color: var(--accent); }
.profile-save-btn { background: var(--accent); color: #fff; border: none; border-radius: var(--radius-sm); padding: 6px 14px; font-size: 12px; font-weight: 600; font-family: inherit; cursor: pointer; }
.profile-cancel-btn { background: none; border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 6px 14px; font-size: 12px; color: var(--text-secondary); font-family: inherit; cursor: pointer; }
.profile-reviews-section { width: 100%; padding: 0 24px 24px; }
.profile-reviews-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.profile-review-count { font-size: 12px; color: var(--text-muted); }
.profile-reviews-empty { font-size: 13px; color: var(--text-muted); text-align: center; padding: 24px 0; }
.profile-reviews-list { display: flex; flex-direction: column; gap: 10px; max-height: 300px; overflow-y: auto; }
.profile-review-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-md); padding: 12px 14px; }
.profile-review-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.profile-review-menu { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.profile-review-stars { font-size: 12px; color: var(--accent); }
.profile-review-content { font-size: 12px; color: var(--text-secondary); line-height: 1.5; margin: 0; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.profile-review-date { font-size: 10px; color: var(--text-muted); margin-top: 6px; display: block; }
</style>
