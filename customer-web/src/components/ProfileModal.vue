<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="pm">
      <div class="pm-head">
        <div>
          <span class="pm-tag">PROFILE</span>
          <h3 class="pm-title">내 정보</h3>
        </div>
        <button class="modal-x" @click="$emit('close')">✕</button>
      </div>

      <div class="pm-body">
        <!-- Nickname -->
        <div class="pm-field">
          <label class="pm-label">닉네임</label>
          <div v-if="!editing" class="pm-value-row">
            <span class="pm-value">{{ nickname }}</span>
            <button class="pm-edit-btn" @click="startEdit">변경</button>
          </div>
          <form v-else class="pm-edit-form" @submit.prevent="save">
            <input v-model="input" class="pm-edit-input" maxlength="20" ref="inputRef" @keydown.esc="editing = false" />
            <button type="submit" class="pm-save-btn" :disabled="!input.trim()">저장</button>
            <button type="button" class="pm-cancel-btn" @click="editing = false">취소</button>
          </form>
        </div>

        <!-- Email -->
        <div class="pm-field">
          <label class="pm-label">이메일</label>
          <span class="pm-value">{{ email }}</span>
        </div>
      </div>

      <!-- My Reviews -->
      <div class="pm-reviews">
        <div class="pm-reviews-head">
          <span class="pm-label">내 리뷰</span>
          <span class="pm-rv-count">{{ groupedReviews.length }}개</span>
        </div>

        <div v-if="reviewsLoading" class="pm-rv-empty"><span class="spinner"></span></div>
        <div v-else-if="groupedReviews.length === 0" class="pm-rv-empty">작성한 리뷰가 없습니다</div>
        <div v-else class="pm-rv-list">
          <div v-for="rv in groupedReviews" :key="rv.id" class="rv-card">
            <!-- Header -->
            <div class="rv-card-head">
              <span class="rv-rating">{{ '★'.repeat(rv.rating) }}<span class="rv-rating-off">{{ '★'.repeat(5 - rv.rating) }}</span></span>
              <span class="rv-date">{{ fmtDate(rv.createdAt) }}</span>
            </div>

            <!-- Menu chips -->
            <div class="rv-menus">
              <span v-for="name in rv.menuNames" :key="name" class="rv-menu-chip">{{ name }}</span>
            </div>

            <!-- View mode -->
            <template v-if="editingReviewId !== rv.id">
              <p class="rv-text">{{ rv.content }}</p>

              <div v-if="rv.adminReply" class="rv-reply">
                <div class="rv-reply-head">
                  <span class="rv-reply-who">사장님</span>
                </div>
                <p class="rv-reply-text">{{ rv.adminReply }}</p>
              </div>

              <div class="rv-actions">
                <button class="rv-act-btn" @click="startEditReview(rv)">수정</button>
                <button class="rv-act-btn rv-act-del" @click="deleteReview(rv)">삭제</button>
              </div>
            </template>

            <!-- Edit mode -->
            <div v-else class="rv-edit-form">
              <ReviewForm
                :menu-name="rv.menuNames.join(', ')"
                :initial-rating="editRating"
                :initial-content="editContent"
                submit-label="수정 완료"
                :show-cancel="true"
                :submitting="saving"
                @submit="({ rating, content }) => saveEditReview(rv, rating, content)"
                @cancel="editingReviewId = null"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import api from '../api'
import { useFormatting } from '../composables/useFormatting'
import ReviewForm from './ReviewForm.vue'

const emit = defineEmits(['close', 'toast'])
const { formatDateShort: fmtDate } = useFormatting()

const nickname = ref('')
const email = ref('')
const editing = ref(false)
const input = ref('')
const inputRef = ref(null)
const reviews = ref([])
const groupedReviews = ref([])
const reviewsLoading = ref(true)

// Review edit state
const editingReviewId = ref(null)
const editRating = ref(0)
const editContent = ref('')
const saving = ref(false)

function groupReviews(list) {
  const groups = new Map()
  for (const r of list) {
    const key = `${r.orderId}_${r.rating}_${r.content}`
    if (groups.has(key)) {
      const g = groups.get(key)
      if (!g.menuNames.includes(r.menuName)) g.menuNames.push(r.menuName)
      g.relatedIds.push(r.id)
    } else {
      groups.set(key, { ...r, menuNames: [r.menuName], relatedIds: [r.id] })
    }
  }
  return [...groups.values()]
}

// Nickname
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

// Review edit
function startEditReview(rv) {
  editingReviewId.value = rv.id
  editRating.value = rv.rating
  editContent.value = rv.content
}

async function saveEditReview(rv, rating, content) {
  saving.value = true
  try {
    const ids = rv.relatedIds || [rv.id]
    for (const id of ids) {
      try { await api.delete(`/reviews/${id}`) } catch {}
    }
    for (const menuName of rv.menuNames) {
      const menuId = reviews.value.find(r => r.menuName === menuName)?.menuId || rv.menuId
      await api.post('/reviews', {
        orderId: rv.orderId,
        menuId,
        menuName,
        rating,
        content
      })
    }
    editingReviewId.value = null
    emit('toast', '리뷰가 수정되었습니다')
    await loadReviews()
  } catch {
    emit('toast', '수정에 실패했습니다')
  } finally { saving.value = false }
}

async function deleteReview(rv) {
  if (!confirm('리뷰를 삭제하시겠습니까?')) return
  try {
    const ids = rv.relatedIds || [rv.id]
    for (const id of ids) {
      try { await api.delete(`/reviews/${id}`) } catch {}
    }
    emit('toast', '리뷰가 삭제되었습니다')
    await loadReviews()
  } catch {
    emit('toast', '삭제에 실패했습니다')
  }
}

async function loadReviews() {
  try {
    const r = await api.get('/reviews/my')
    reviews.value = r.data.content || r.data || []
    groupedReviews.value = groupReviews(reviews.value)
  } catch { reviews.value = []; groupedReviews.value = [] }
}

onMounted(async () => {
  try {
    const r = await api.get('/auth/profile')
    nickname.value = r.data.nickname
    email.value = r.data.email
  } catch {}
  await loadReviews()
  reviewsLoading.value = false
})
</script>

<style scoped>
.pm {
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: var(--radius-xl); width: 480px; max-height: 85vh;
  display: flex; flex-direction: column;
  box-shadow: 0 32px 80px rgba(0,0,0,0.7), 0 0 0 1px rgba(255,255,255,0.03) inset;
  animation: pmIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes pmIn { from { opacity: 0; transform: translateY(16px) scale(0.97); } to { opacity: 1; transform: translateY(0) scale(1); } }

.pm-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 24px 28px 18px; border-bottom: 1px solid var(--border); flex-shrink: 0;
}
.pm-tag { font-size: 11px; font-weight: 700; letter-spacing: 0.14em; color: var(--accent); }
.pm-title { font-size: 22px; font-weight: 700; color: var(--text-primary); margin: 0; }

.pm-body { padding: 24px 28px; display: flex; flex-direction: column; gap: 18px; }

.pm-field { width: 100%; }
.pm-label { font-size: 11px; font-weight: 600; color: var(--text-muted); letter-spacing: 0.06em; text-transform: uppercase; margin-bottom: 6px; display: block; }
.pm-value-row { display: flex; align-items: center; justify-content: space-between; }
.pm-value { font-size: 14px; color: var(--text-primary); font-weight: 500; }
.pm-edit-btn { background: none; border: 1px solid var(--border); border-radius: var(--radius-sm); color: var(--text-secondary); cursor: pointer; padding: 4px 12px; font-size: 12px; font-family: inherit; transition: var(--transition); }
.pm-edit-btn:hover { color: var(--accent); border-color: var(--accent); }
.pm-edit-form { display: flex; align-items: center; gap: 6px; }
.pm-edit-input { flex: 1; padding: 7px 12px; background: var(--bg-subtle); border: 1px solid var(--border); border-radius: var(--radius-sm); color: var(--text-primary); font-size: 13px; font-family: inherit; outline: none; }
.pm-edit-input:focus { border-color: var(--accent); }
.pm-save-btn { background: var(--accent); color: #fff; border: none; border-radius: var(--radius-sm); padding: 7px 14px; font-size: 12px; font-weight: 600; font-family: inherit; cursor: pointer; }
.pm-cancel-btn { background: none; border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 7px 14px; font-size: 12px; color: var(--text-secondary); font-family: inherit; cursor: pointer; }

/* Reviews section */
.pm-reviews { padding: 0 28px 24px; flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.pm-reviews-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.pm-rv-count { font-size: 12px; color: var(--text-muted); }
.pm-rv-empty { font-size: 13px; color: var(--text-muted); text-align: center; padding: 24px 0; }
.pm-rv-list { display: flex; flex-direction: column; gap: 8px; overflow-y: auto; flex: 1; padding-right: 4px; }

/* Review card — matches OrderDetailModal style */
.rv-card {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius-md); padding: 14px 16px;
}

.rv-card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.rv-rating { font-size: 12px; color: #fbbf24; letter-spacing: 0.5px; }
.rv-rating-off { color: rgba(255,255,255,0.08); }
.rv-date { font-size: 11px; color: var(--text-muted); }

.rv-menus { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 8px; }
.rv-menu-chip {
  font-size: 11px; font-weight: 500; color: var(--text-secondary);
  background: var(--bg-subtle); padding: 2px 8px; border-radius: 4px;
}

.rv-text { font-size: 13px; color: var(--text-secondary); line-height: 1.6; margin: 0; }

/* Admin reply */
.rv-reply {
  margin-top: 10px; padding: 10px 12px;
  background: rgba(212,134,60,0.04); border-radius: var(--radius-sm);
  border-left: 2px solid var(--accent);
}
.rv-reply-head { margin-bottom: 3px; }
.rv-reply-who { font-size: 11px; font-weight: 700; color: var(--accent-soft); }
.rv-reply-text { font-size: 12px; color: var(--text-secondary); line-height: 1.5; margin: 0; }

/* Actions */
.rv-actions { display: flex; gap: 6px; margin-top: 10px; padding-top: 10px; border-top: 1px solid var(--border); }
.rv-act-btn {
  background: var(--bg-subtle); border: 1px solid var(--border);
  border-radius: var(--radius-sm); padding: 5px 12px;
  font-size: 12px; color: var(--text-secondary); font-family: inherit;
  cursor: pointer; transition: var(--transition);
}
.rv-act-btn:hover { border-color: var(--border-hover); color: var(--text-primary); }
.rv-act-del { color: #f87171; }
.rv-act-del:hover { border-color: #f87171; background: rgba(248,113,113,0.06); }

/* Edit form */
.rv-edit-form { margin-top: 10px; padding-top: 10px; border-top: 1px solid var(--border); display: flex; flex-direction: column; gap: 8px; }
.rv-edit-stars { display: flex; gap: 2px; }
.rv-star {
  font-size: 22px; color: rgba(255,255,255,0.08); background: none;
  border: none; cursor: pointer; padding: 0; line-height: 1; transition: var(--transition);
}
.rv-star.on { color: #fbbf24; text-shadow: 0 0 6px rgba(251,191,36,0.3); }
.rv-edit-ta {
  width: 100%; padding: 10px 12px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: var(--radius-sm); color: var(--text-primary);
  font-family: inherit; font-size: 13px; resize: vertical;
  min-height: 60px; outline: none; line-height: 1.5;
}
.rv-edit-ta:focus { border-color: var(--accent); }
.rv-edit-actions { display: flex; gap: 6px; justify-content: flex-end; }
.rv-save-btn {
  background: var(--accent); color: #fff; border: none;
  border-radius: var(--radius-sm); padding: 7px 14px;
  font-size: 12px; font-weight: 700; font-family: inherit;
  cursor: pointer; transition: var(--transition);
}
.rv-save-btn:hover:not(:disabled) { background: var(--accent-soft); }
.rv-save-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
