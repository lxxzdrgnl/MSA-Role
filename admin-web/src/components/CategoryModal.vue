<template>
  <ModalWrapper :open="open" title="카테고리 관리" @close="$emit('close')">
    <div class="cat-add-form">
      <input v-model="newName" placeholder="새 카테고리 이름" class="form-input" @keydown.enter="add"/>
      <button class="btn btn-primary" @click="add" :disabled="!newName.trim()">추가</button>
    </div>
    <div v-if="error" class="alert-box alert-error" style="margin-top:12px">{{ error }}</div>
    <div class="cat-list">
      <div v-if="categories.length === 0" class="cat-empty">등록된 카테고리가 없습니다.</div>
      <div v-for="(cat, idx) in categories" :key="cat.id" class="cat-row">
        <div class="cat-sort">
          <button class="sort-btn" :disabled="idx === 0" @click="moveUp(idx)"><svg width="10" height="10" viewBox="0 0 10 10" fill="none"><path d="M5 1L1 6H9L5 1Z" fill="currentColor"/></svg></button>
          <button class="sort-btn" :disabled="idx === categories.length - 1" @click="moveDown(idx)"><svg width="10" height="10" viewBox="0 0 10 10" fill="none"><path d="M5 9L1 4H9L5 9Z" fill="currentColor"/></svg></button>
        </div>
        <div class="cat-info" v-if="editId !== cat.id">
          <span class="cat-name">{{ cat.name }}</span>
          <span class="cat-count">{{ cat.menuCount ?? 0 }}개</span>
        </div>
        <input v-else v-model="editName" class="form-input cat-edit-input" @keydown.enter="saveEdit(cat)" @keydown.escape="editId = null"/>
        <div class="cat-actions">
          <button v-if="editId !== cat.id" class="icon-btn" @click="editId = cat.id; editName = cat.name" title="수정">
            <svg width="13" height="13" viewBox="0 0 14 14" fill="none"><path d="M2 10L8.5 3.5L10.5 5.5L4 12H2V10Z" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/></svg>
          </button>
          <button v-if="editId === cat.id" class="icon-btn icon-save" @click="saveEdit(cat)">
            <svg width="13" height="13" viewBox="0 0 14 14" fill="none"><path d="M3 7L6 10L11 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
          <button v-if="editId === cat.id" class="icon-btn" @click="editId = null">
            <svg width="13" height="13" viewBox="0 0 14 14" fill="none"><path d="M4 4L10 10M10 4L4 10" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
          </button>
          <button v-if="editId !== cat.id" class="icon-btn icon-del" @click="handleDelete(cat.id)">
            <svg width="13" height="13" viewBox="0 0 14 14" fill="none"><path d="M3 4H11M5.5 4V3C5.5 2.44772 5.94772 2 6.5 2H7.5C8.05228 2 8.5 2.44772 8.5 3V4M4 4L4.5 11.5C4.5 12.0523 4.94772 12.5 5.5 12.5H8.5C9.05228 12.5 9.5 12.0523 9.5 11.5L10 4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
          </button>
        </div>
      </div>
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
.cat-add-form { display: flex; gap: 10px; }
.cat-add-form .form-input { flex: 1; }
.cat-list { margin-top: 16px; display: flex; flex-direction: column; gap: 2px; }
.cat-empty { text-align: center; padding: 28px; color: var(--text-muted); font-size: 13px; }
.cat-row { display: flex; align-items: center; gap: 8px; padding: 9px 10px; border-radius: var(--radius-sm); transition: background var(--transition-fast); }
.cat-row:hover { background: var(--bg-hover); }
.cat-sort { display: flex; flex-direction: column; gap: 1px; flex-shrink: 0; }
.sort-btn { width: 20px; height: 14px; border: none; background: transparent; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; justify-content: center; border-radius: 3px; transition: all var(--transition-fast); }
.sort-btn:hover:not(:disabled) { background: var(--bg-active); color: var(--text-primary); }
.sort-btn:disabled { opacity: 0.2; cursor: not-allowed; }
.cat-info { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; }
.cat-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.cat-count { font-size: 11px; color: var(--text-muted); background: var(--bg-hover); padding: 2px 7px; border-radius: 8px; }
.cat-edit-input { padding: 5px 10px !important; font-size: 14px !important; flex: 1; }
.cat-actions { display: flex; gap: 3px; flex-shrink: 0; }
.icon-btn { width: 28px; height: 28px; border-radius: var(--radius-sm); border: none; background: transparent; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all var(--transition-fast); }
.icon-btn:hover { background: var(--bg-active); color: var(--text-primary); }
.icon-save:hover { background: var(--success-bg); color: var(--success); }
.icon-del:hover { background: var(--danger-bg); color: var(--danger); }
</style>
