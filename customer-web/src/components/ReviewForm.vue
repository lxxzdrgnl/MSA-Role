<template>
  <div class="rvf">
    <!-- Stars -->
    <div class="rvf-stars">
      <button v-for="n in 5" :key="n" class="rvf-star" :class="{ on: rating >= n }" @click="rating = n">★</button>
    </div>

    <!-- Keywords -->
    <div class="rvf-kw-row">
      <button v-for="k in KW" :key="k" class="rvf-kw" :class="{ sel: kws.includes(k) }" @click="togKw(k)">{{ k }}</button>
    </div>
    <div class="rvf-kw-custom">
      <div v-for="k in customKws" :key="k" class="rvf-kw sel">{{ k }} <span class="rvf-kw-x" @click="removeCustom(k)">×</span></div>
      <form class="rvf-kw-input-form" @submit.prevent="addCustomKw">
        <input v-model="customKw" class="rvf-kw-input" placeholder="직접 입력..." />
        <button type="submit" class="rvf-kw-add">+</button>
      </form>
    </div>

    <!-- AI Draft -->
    <button class="rvf-draft-btn" :disabled="draftLoading || rating === 0" @click="genDraft">
      <span v-if="draftLoading" class="spinner"></span>
      {{ draftLoading ? 'AI 작성 중...' : '✦ AI 리뷰 초안' }}
    </button>

    <!-- Text -->
    <textarea v-model="text" class="rvf-ta" rows="3" placeholder="리뷰를 작성하세요..."></textarea>

    <!-- Actions -->
    <div class="rvf-actions">
      <button v-if="showCancel" class="rvf-cancel" @click="$emit('cancel')">취소</button>
      <button class="rvf-submit" :disabled="!text.trim() || submitting" @click="handleSubmit">
        {{ submitting ? '처리 중...' : submitLabel }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '../api'
import { REVIEW_KEYWORDS } from '../constants'

const props = defineProps({
  menuName: { type: String, default: '' },
  initialRating: { type: Number, default: 0 },
  initialContent: { type: String, default: '' },
  submitLabel: { type: String, default: '리뷰 등록' },
  showCancel: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false },
})

const emit = defineEmits(['submit', 'cancel'])

const KW = REVIEW_KEYWORDS
const rating = ref(props.initialRating)
const text = ref(props.initialContent)
const kws = ref([])
const customKws = ref([])
const customKw = ref('')
const draftLoading = ref(false)

watch(() => props.initialRating, v => { rating.value = v })
watch(() => props.initialContent, v => { text.value = v })

function togKw(k) {
  const i = kws.value.indexOf(k)
  i >= 0 ? kws.value.splice(i, 1) : kws.value.push(k)
}

function addCustomKw() {
  const v = customKw.value.trim()
  if (v && !kws.value.includes(v) && !customKws.value.includes(v)) {
    customKws.value.push(v)
    kws.value.push(v)
  }
  customKw.value = ''
}

function removeCustom(k) {
  customKws.value = customKws.value.filter(x => x !== k)
  kws.value = kws.value.filter(x => x !== k)
}

async function genDraft() {
  draftLoading.value = true
  try {
    const r = await api.post('/reviews/generate', {
      menuName: props.menuName,
      rating: rating.value,
      keywords: kws.value,
    })
    text.value = r.data.draft || r.data.review || ''
  } catch {} finally { draftLoading.value = false }
}

function handleSubmit() {
  emit('submit', { rating: rating.value, content: text.value })
}
</script>

<style scoped>
.rvf { display: flex; flex-direction: column; gap: 10px; }

/* Stars */
.rvf-stars { display: flex; gap: 2px; }
.rvf-star {
  font-size: 24px; color: rgba(255,255,255,0.08); background: none;
  border: none; cursor: pointer; padding: 0; line-height: 1; transition: var(--transition);
}
.rvf-star.on { color: #fbbf24; text-shadow: 0 0 8px rgba(251,191,36,0.3); }

/* Keywords */
.rvf-kw-row { display: flex; flex-wrap: wrap; gap: 5px; }
.rvf-kw {
  background: var(--bg-subtle); border: 1px solid var(--border);
  border-radius: 99px; padding: 5px 12px;
  font-size: 12px; color: var(--text-secondary); font-family: inherit;
  cursor: pointer; transition: var(--transition);
}
.rvf-kw.sel { background: var(--accent-bg); border-color: var(--accent); color: var(--accent-soft); }

.rvf-kw-custom { display: flex; flex-wrap: wrap; gap: 5px; align-items: center; }
.rvf-kw-custom .rvf-kw { display: flex; align-items: center; gap: 4px; }
.rvf-kw-x { cursor: pointer; font-size: 13px; opacity: 0.6; }
.rvf-kw-x:hover { opacity: 1; }
.rvf-kw-input-form { display: flex; gap: 4px; align-items: center; }
.rvf-kw-input {
  width: 100px; padding: 5px 12px;
  background: var(--bg-subtle); border: 1px dashed var(--border);
  border-radius: 99px; font-size: 12px; color: var(--text-primary);
  font-family: inherit; outline: none; transition: var(--transition);
}
.rvf-kw-input:focus { border-color: var(--accent); width: 130px; }
.rvf-kw-input::placeholder { color: var(--text-muted); }
.rvf-kw-add {
  width: 26px; height: 26px; border-radius: 99px;
  background: var(--accent); border: none; color: #fff;
  font-size: 15px; display: flex; align-items: center; justify-content: center;
  cursor: pointer; flex-shrink: 0;
}

/* AI Draft */
.rvf-draft-btn {
  background: var(--bg-elevated); border: 1px dashed var(--accent);
  color: var(--accent-soft); padding: 9px 16px;
  border-radius: var(--radius-sm); font-size: 13px; font-weight: 600;
  font-family: inherit; cursor: pointer; transition: var(--transition);
  display: flex; align-items: center; justify-content: center; gap: 6px;
}
.rvf-draft-btn:hover:not(:disabled) { background: var(--accent-dim); }
.rvf-draft-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* Textarea */
.rvf-ta {
  width: 100%; padding: 10px 14px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: var(--radius-sm); color: var(--text-primary);
  font-family: inherit; font-size: 13px; resize: vertical;
  min-height: 70px; outline: none; line-height: 1.5;
}
.rvf-ta:focus { border-color: var(--accent); }

/* Actions */
.rvf-actions { display: flex; gap: 8px; justify-content: flex-end; }
.rvf-cancel {
  background: var(--bg-subtle); border: 1px solid var(--border);
  border-radius: var(--radius-sm); padding: 7px 14px;
  font-size: 12px; color: var(--text-secondary); font-family: inherit;
  cursor: pointer; transition: var(--transition);
}
.rvf-cancel:hover { border-color: var(--border-hover); color: var(--text-primary); }
.rvf-submit {
  flex: 1; background: var(--accent); color: #fff; border: none;
  border-radius: var(--radius-sm); padding: 9px;
  font-size: 14px; font-weight: 700; font-family: inherit;
  cursor: pointer; transition: var(--transition);
}
.rvf-submit:hover:not(:disabled) { background: var(--accent-soft); }
.rvf-submit:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
