<template>
  <div class="pagination" v-if="totalPages > 1">
    <button class="btn btn-secondary btn-sm" :disabled="page === 0" @click="$emit('change', page - 1)">이전</button>
    <span class="page-info">{{ page + 1 }} / {{ totalPages }}</span>
    <button class="btn btn-secondary btn-sm" :disabled="page >= totalPages - 1" @click="$emit('change', page + 1)">다음</button>
  </div>
</template>

<script setup>
defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true },
})
defineEmits(['change'])
</script>
