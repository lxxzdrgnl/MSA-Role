<template>
  <span class="status-pill" :class="cls">
    <span class="pill-dot"></span>
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { statusLabel, statusClass } from '../utils/status.js'

const props = defineProps({
  status: { type: String, required: true },
})

const label = computed(() => statusLabel(props.status))
const cls = computed(() => statusClass(props.status))
</script>

<style scoped>
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}
.pill-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}
.pill-active {
  background: var(--status-active-bg);
  color: var(--status-active-text);
}
.pill-done {
  background: var(--status-done-bg);
  color: var(--status-done-text);
}
.pill-cancel {
  background: var(--status-danger-bg);
  color: var(--status-danger-text);
}
</style>
