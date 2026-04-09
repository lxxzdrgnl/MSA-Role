<template>
  <div class="panel panel-donut">
    <div class="panel-header">
      <h3>주문 상태</h3>
    </div>
    <div v-if="total === 0" class="panel-empty">데이터 없음</div>
    <div v-else class="donut-wrap">
      <svg viewBox="0 0 120 120" class="donut-svg">
        <circle cx="60" cy="60" r="48" fill="none" stroke="var(--bg-hover)" stroke-width="14"/>
        <circle v-for="(seg, i) in segments" :key="i"
          cx="60" cy="60" r="48" fill="none"
          :stroke="seg.color" stroke-width="14"
          :stroke-dasharray="seg.dash" :stroke-dashoffset="seg.offset"
          stroke-linecap="round" class="donut-seg"
          :style="{ animationDelay: `${i * 100}ms` }"/>
        <text x="60" y="56" text-anchor="middle" class="donut-total">{{ total }}</text>
        <text x="60" y="72" text-anchor="middle" class="donut-total-label">전체</text>
      </svg>
      <div class="donut-legend">
        <div v-for="seg in segments" :key="seg.label" class="legend-item">
          <span class="legend-dot" :style="{ background: seg.color }"></span>
          <span class="legend-label">{{ seg.label }}</span>
          <span class="legend-val">{{ seg.count }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  orders: { type: Array, default: () => [] },
})

const CIRC = 2 * Math.PI * 48

const statusDist = computed(() => {
  const counts = { PENDING: 0, ACCEPTED: 0, COOKING: 0, COMPLETED: 0, CANCELLED: 0 }
  for (const o of props.orders) {
    const s = o.status === 'READY' ? 'COMPLETED' : o.status
    if (s in counts) counts[s]++
  }
  return counts
})

const total = computed(() => Object.values(statusDist.value).reduce((a, b) => a + b, 0))

const segments = computed(() => {
  const t = total.value
  if (!t) return []
  const items = [
    { label: '대기', key: 'PENDING', color: 'var(--accent-brass)' },
    { label: '접수', key: 'ACCEPTED', color: 'rgba(212,134,60,0.6)' },
    { label: '조리중', key: 'COOKING', color: 'rgba(212,134,60,0.35)' },
    { label: '완료', key: 'COMPLETED', color: 'var(--text-muted)' },
    { label: '취소', key: 'CANCELLED', color: 'var(--danger)' },
  ]
  let accumulated = 0
  return items
    .map(it => {
      const count = statusDist.value[it.key] || 0
      const pct = count / t
      const len = pct * CIRC
      const gap = count > 0 ? 3 : 0
      const dash = `${Math.max(len - gap, 0)} ${CIRC - Math.max(len - gap, 0)}`
      const offset = -accumulated * CIRC + CIRC * 0.25
      accumulated += pct
      return { ...it, count, dash, offset }
    })
    .filter(s => s.count > 0)
})
</script>

<style scoped>
.panel { background: var(--bg-surface); border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 22px; display: flex; flex-direction: column; }
.panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; }
.panel-header h3 { font-size: 14px; font-weight: 700; color: var(--text-primary); }
.panel-empty { text-align: center; padding: 32px; color: var(--text-muted); font-size: 13px; }
.donut-wrap { display: flex; flex-direction: column; align-items: center; gap: 18px; flex: 1; }
.donut-svg { width: 140px; height: 140px; }
.donut-seg { animation: donut-draw 0.8s cubic-bezier(0.4,0,0.2,1) both; transform-origin: center; }
@keyframes donut-draw { from { stroke-dasharray: 0 301.6; opacity: 0; } to { opacity: 1; } }
.donut-total { font-size: 22px; font-weight: 700; fill: var(--text-primary); font-family: inherit; }
.donut-total-label { font-size: 10px; font-weight: 500; fill: var(--text-muted); font-family: inherit; text-transform: uppercase; letter-spacing: 1px; }
.donut-legend { display: flex; flex-wrap: wrap; gap: 6px 14px; justify-content: center; }
.legend-item { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.legend-dot { width: 8px; height: 8px; border-radius: 2px; flex-shrink: 0; }
.legend-label { color: var(--text-secondary); }
.legend-val { font-weight: 700; color: var(--text-primary); font-variant-numeric: tabular-nums; }
</style>
