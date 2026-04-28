<template>
  <ModalWrapper :open="open" title="카테고리 관리" @close="$emit('close')">
    <!-- Add form -->
    <div class="cm-add">
      <div class="cm-add-input-wrap">
        <svg class="cm-add-icon" width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M7 2v10M2 7h10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        <input v-model="newName" placeholder="새 카테고리 이름" class="cm-add-input" @keydown.enter="add"/>
      </div>
      <button class="cm-add-btn" @click="add" :disabled="!newName.trim()">추가</button>
    </div>

    <div v-if="error" class="alert-box alert-error" style="margin-top:12px">{{ error }}</div>

    <!-- Category list -->
    <div class="cm-list">
      <div v-if="categories.length === 0" class="cm-empty">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" style="opacity:0.2"><path d="M3 7l9-4 9 4v10l-9 4-9-4V7z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/><path d="M12 11v10M3 7l9 4 9-4" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
        <span>등록된 카테고리가 없습니다</span>
      </div>

      <TransitionGroup name="cat-item" tag="div" class="cm-items">
        <div v-for="(cat, idx) in categories" :key="cat.id" class="cm-row" :class="{ 'cm-row-editing': editId === cat.id }">
          <!-- Sort -->
          <div class="cm-sort">
            <button class="cm-sort-btn" :disabled="idx === 0" @click="moveUp(idx)" title="위로">
              <svg width="10" height="10" viewBox="0 0 10 10" fill="none"><path d="M2 6l3-3 3 3" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
            <span class="cm-sort-num">{{ idx + 1 }}</span>
            <button class="cm-sort-btn" :disabled="idx === categories.length - 1" @click="moveDown(idx)" title="아래로">
              <svg width="10" height="10" viewBox="0 0 10 10" fill="none"><path d="M2 4l3 3 3-3" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
          </div>

          <!-- View mode -->
          <template v-if="editId !== cat.id">
            <div class="cm-info">
              <span class="cm-name">{{ cat.name }}</span>
              <span class="cm-count">{{ cat.menuCount ?? 0 }}개 메뉴</span>
            </div>
            <div class="cm-actions">
              <button class="cm-act" @click="editId = cat.id; editName = cat.name" title="수정">
                <svg width="13" height="13" viewBox="0 0 14 14" fill="none"><path d="M2 10L8.5 3.5L10.5 5.5L4 12H2V10Z" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/></svg>
              </button>
              <button class="cm-act cm-act-del" @click="handleDelete(cat.id)" title="삭제">
                <svg width="13" height="13" viewBox="0 0 14 14" fill="none"><path d="M3 4H11M5.5 4V3C5.5 2.44772 5.94772 2 6.5 2H7.5C8.05228 2 8.5 2.44772 8.5 3V4M4 4L4.5 11.5C4.5 12.0523 4.94772 12.5 5.5 12.5H8.5C9.05228 12.5 9.5 12.0523 9.5 11.5L10 4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
              </button>
            </div>
          </template>

          <!-- Edit mode -->
          <template v-else>
            <input v-model="editName" class="cm-edit-input" @keydown.enter="saveEdit(cat)" @keydown.escape="editId = null" ref="editInput" />
            <div class="cm-actions">
              <button class="cm-act cm-act-save" @click="saveEdit(cat)" title="저장">
                <svg width="13" height="13" viewBox="0 0 14 14" fill="none"><path d="M3 7L6 10L11 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </button>
              <button class="cm-act" @click="editId = null" title="취소">
                <svg width="13" height="13" viewBox="0 0 14 14" fill="none"><path d="M4 4L10 10M10 4L4 10" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
              </button>
            </div>
          </template>
        </div>
      </TransitionGroup>
    </div>
  </ModalWrapper>
</template>

<script setup>
import { ref } from 'vue'
import { createCategory, updateCategory, deleteCategory } from '../api.js'
import ModalWrapper from './ModalWrapper.vue'

const props = defineProps({
  open: { type: Boolean, required: true },
  categories: { type: Array, default: () => [] },
})

const emit = defineEmits(['close', 'updated'])

const newName = ref('')
const error = ref('')
const editId = ref(null)
const editName = ref('')

async function add() {
  if (!newName.value.trim()) return
  error.value = ''
  try {
    await createCategory(newName.value.trim())
    newName.value = ''
    emit('updated')
  } catch (e) { error.value = e.response?.data?.message || '추가 실패' }
}

async function handleDelete(id) {
  if (!confirm('이 카테고리를 삭제하시겠습니까?\n메뉴가 있으면 삭제 불가합니다.')) return
  try { await deleteCategory(id); emit('updated') }
  catch (e) {
    error.value = (e.response?.status === 500 || e.response?.status === 409)
      ? '메뉴가 있어 삭제 불가합니다.'
      : (e.response?.data?.message || '삭제 실패')
  }
}

async function saveEdit(cat) {
  if (!editName.value.trim()) return
  error.value = ''
  try {
    await updateCategory(cat.id, { name: editName.value.trim(), sortOrder: cat.sortOrder ?? 0 })
    editId.value = null
    emit('updated')
  } catch (e) { error.value = e.response?.data?.message || '수정 실패' }
}

async function moveUp(idx) { if (idx > 0) await reorder(idx, idx - 1) }
async function moveDown(idx) { if (idx < props.categories.length - 1) await reorder(idx, idx + 1) }

async function reorder(from, to) {
  error.value = ''
  const list = [...props.categories];
  [list[from], list[to]] = [list[to], list[from]]
  try {
    for (let i = 0; i < list.length; i++) {
      if ((list[i].sortOrder ?? 0) !== i)
        await updateCategory(list[i].id, { name: list[i].name, sortOrder: i })
    }
    emit('updated')
  } catch { error.value = '정렬 변경 실패' }
}
</script>

<style scoped>
/* ── Add form ── */
.cm-add { display: flex; gap: 8px; }
.cm-add-input-wrap {
  flex: 1; position: relative;
}
.cm-add-icon {
  position: absolute; left: 12px; top: 50%; transform: translateY(-50%);
  color: var(--text-muted); pointer-events: none;
  transition: color 0.15s;
}
.cm-add-input {
  width: 100%; padding: 10px 14px 10px 34px;
  background: var(--bg-input); border: 1px solid var(--border-strong);
  border-radius: var(--radius-md); color: var(--text-primary);
  font-size: 14px; font-family: inherit; outline: none;
  transition: all 0.15s;
}
.cm-add-input::placeholder { color: var(--text-muted); }
.cm-add-input:focus { border-color: var(--accent-brass); box-shadow: 0 0 0 3px var(--accent-brass-glow); }
.cm-add-input:focus + .cm-add-icon,
.cm-add-input-wrap:focus-within .cm-add-icon { color: var(--accent-brass); }
.cm-add-btn {
  padding: 10px 20px; background: var(--accent-brass); color: var(--text-inverse);
  border: none; border-radius: var(--radius-md);
  font-size: 13px; font-weight: 700; font-family: inherit;
  cursor: pointer; transition: all 0.15s; white-space: nowrap;
}
.cm-add-btn:hover:not(:disabled) { filter: brightness(1.1); box-shadow: 0 2px 12px rgba(212,134,60,0.25); }
.cm-add-btn:disabled { opacity: 0.35; cursor: not-allowed; }

/* ── List ── */
.cm-list { margin-top: 16px; }
.cm-items { display: flex; flex-direction: column; gap: 4px; }

.cm-empty {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 32px; color: var(--text-muted); font-size: 13px;
}

/* ── Row ── */
.cm-row {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 12px; border-radius: var(--radius-md);
  border: 1px solid transparent;
  transition: all 0.15s;
}
.cm-row:hover { background: var(--bg-hover); border-color: var(--border); }
.cm-row-editing { background: var(--accent-brass-glow); border-color: var(--accent-brass-border); }

/* Transition */
.cat-item-move { transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.cat-item-enter-active { animation: catIn 0.25s ease; }
.cat-item-leave-active { animation: catIn 0.15s ease reverse; position: absolute; width: 100%; }
@keyframes catIn { from { opacity: 0; transform: translateX(-8px); } to { opacity: 1; transform: translateX(0); } }

/* Sort */
.cm-sort {
  display: flex; flex-direction: column; align-items: center; gap: 0;
  flex-shrink: 0; width: 28px;
}
.cm-sort-btn {
  width: 22px; height: 14px; border: none; background: transparent;
  color: var(--text-muted); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  border-radius: 3px; transition: all 0.15s;
}
.cm-sort-btn:hover:not(:disabled) { background: var(--bg-active); color: var(--accent-brass); }
.cm-sort-btn:disabled { opacity: 0.15; cursor: not-allowed; }
.cm-sort-num {
  font-size: 10px; font-weight: 700; color: var(--text-muted);
  font-variant-numeric: tabular-nums; line-height: 1;
}

/* Info */
.cm-info { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
.cm-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.cm-count {
  font-size: 11px; font-weight: 600; color: var(--accent-brass);
  background: var(--accent-brass-glow); border: 1px solid var(--accent-brass-border);
  padding: 2px 8px; border-radius: 99px;
}

/* Edit input */
.cm-edit-input {
  flex: 1; padding: 7px 12px;
  background: var(--bg-input); border: 1px solid var(--accent-brass);
  border-radius: var(--radius-sm); color: var(--text-primary);
  font-size: 14px; font-weight: 600; font-family: inherit; outline: none;
  box-shadow: 0 0 0 3px var(--accent-brass-glow);
}

/* Actions */
.cm-actions { display: flex; gap: 2px; flex-shrink: 0; }
.cm-act {
  width: 30px; height: 30px; border-radius: var(--radius-sm);
  border: 1px solid transparent; background: transparent;
  color: var(--text-muted); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.cm-act:hover { background: var(--bg-active); color: var(--text-primary); border-color: var(--border); }
.cm-act-save { color: var(--accent-brass); }
.cm-act-save:hover { background: var(--accent-brass-glow); color: var(--accent-brass); border-color: var(--accent-brass-border); }
.cm-act-del:hover { background: var(--danger-bg); color: var(--danger); border-color: rgba(200,120,100,0.2); }
</style>
