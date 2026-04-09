<template>
  <Teleport to="body">
    <div v-if="open" class="modal-overlay" @click.self="$emit('close')">
      <div class="modal" :class="sizeClass" @click.stop>
        <div class="modal-header" v-if="title || $slots.header">
          <slot name="header">
            <h3>{{ title }}</h3>
          </slot>
          <button class="modal-close" @click="$emit('close')">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M5 5L13 13M13 5L5 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <slot />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  open: { type: Boolean, required: true },
  title: { type: String, default: '' },
  size: { type: String, default: 'md' },
})

defineEmits(['close'])

const sizeClass = computed(() => `modal-${props.size}`)
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fade-in 0.2s ease;
}
@keyframes fade-in { from { opacity: 0; } to { opacity: 1; } }

.modal {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  width: 100%;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-lg);
  animation: modal-up 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.modal-sm { max-width: 400px; }
.modal-md { max-width: 480px; }
.modal-lg { max-width: 640px; }
.modal-xl { max-width: 800px; }

@keyframes modal-up {
  from { opacity: 0; transform: translateY(12px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 24px 0;
}
.modal-header h3 { font-size: 17px; font-weight: 700; color: var(--text-primary); }

.modal-close {
  width: 32px; height: 32px;
  border-radius: var(--radius-sm);
  border: none; background: transparent;
  color: var(--text-muted); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all var(--transition-fast);
}
.modal-close:hover { background: var(--bg-hover); color: var(--text-primary); }

.modal-body {
  padding: 20px 24px 24px;
  overflow-y: auto;
}
</style>
