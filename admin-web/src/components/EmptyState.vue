<template>
  <div class="empty-state">
    <span v-if="icon" class="empty-icon">{{ icon }}</span>
    <slot>
      <span class="empty-msg">{{ message }}</span>
    </slot>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '' },
  message: { type: String, default: '데이터 없음' },
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 36px;
  color: var(--text-muted);
  font-size: 13px;
}
.empty-icon {
  font-size: 20px;
  opacity: 0.6;
}
</style>
