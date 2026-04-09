<template>
  <div class="panel panel-chart">
    <div class="panel-header">
      <h3>주간 매출 추이</h3>
      <span class="panel-badge">최근 7일</span>
    </div>
    <div v-if="data.length === 0" class="panel-empty">데이터 없음</div>
    <div v-else class="chart-area">
      <svg :viewBox="`0 0 ${W} ${H}`" preserveAspectRatio="none" class="revenue-svg">
        <defs>
          <linearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="var(--accent-brass)" stop-opacity="0.25"/>
            <stop offset="100%" stop-color="var(--accent-brass)" stop-opacity="0.02"/>
          </linearGradient>
        </defs>
        <line v-for="i in 4" :key="'g'+i"
          :x1="P" :x2="W - P"
          :y1="P + (i - 1) * ((H - P * 2) / 3)"
          :y2="P + (i - 1) * ((H - P * 2) / 3)"
          stroke="var(--border)" stroke-width="0.5"/>
        <path :d="areaPath" fill="url(#areaGrad)" class="area-shape"/>
        <polyline :points="linePoints" fill="none" stroke="var(--accent-brass)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="line-shape"/>
        <circle v-for="(pt, i) in pts" :key="'d'+i"
          :cx="pt.x" :cy="pt.y" r="3.5"
          fill="var(--bg-surface)" stroke="var(--accent-brass)" stroke-width="2"
          class="chart-dot"/>
      </svg>
      <div class="chart-x-labels">
        <span v-for="(d, i) in data" :key="'l'+i">{{ d.label }}</span>
      </div>
      <div class="chart-y-labels">
        <span>{{ fmtShort(maxVal) }}</span>
        <span>{{ fmtShort(Math.round(maxVal / 2)) }}</span>
        <span>0</span>
      </div>
      <div class="chart-values">
        <div v-for="(d, i) in data" :key="'v'+i" class="chart-hover-zone" :style="{ left: pts[i]?.xPct + '%' }">
          <div class="chart-tooltip">
            <span class="tt-date">{{ d.fullLabel }}</span>
            <span class="tt-val">{{ d.revenue.toLocaleString() }}원</span>
            <span class="tt-cnt">{{ d.count }}건</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatNumShort as fmtShort } from '../utils/formatters.js'

const props = defineProps({
  data: { type: Array, default: () => [] },
})

const W = 520, H = 180, P = 20

const maxVal = computed(() => Math.max(...props.data.map(d => d.revenue), 1) * 1.15)

const pts = computed(() => {
  const n = props.data.length
  if (!n) return []
  const usableW = W - P * 2, usableH = H - P * 2
  return props.data.map((d, i) => {
    const x = P + (i / Math.max(n - 1, 1)) * usableW
    const y = P + usableH - (d.revenue / maxVal.value) * usableH
    return { x, y, xPct: (x / W) * 100 }
  })
})

const linePoints = computed(() => pts.value.map(p => `${p.x},${p.y}`).join(' '))

const areaPath = computed(() => {
  const p = pts.value
  if (p.length < 2) return ''
  const bottom = H - P
  let d = `M${p[0].x},${bottom} L${p[0].x},${p[0].y}`
  for (let i = 1; i < p.length; i++) d += ` L${p[i].x},${p[i].y}`
  d += ` L${p[p.length - 1].x},${bottom} Z`
  return d
})
</script>

<style scoped>
.panel { background: var(--bg-surface); border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 22px; }
.panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; }
.panel-header h3 { font-size: 14px; font-weight: 700; color: var(--text-primary); }
.panel-badge { font-size: 11px; font-weight: 600; color: var(--accent-brass); background: var(--accent-brass-glow); padding: 3px 10px; border-radius: 20px; border: 1px solid var(--accent-brass-border); }
.panel-empty { text-align: center; padding: 32px; color: var(--text-muted); font-size: 13px; }
.panel-chart { position: relative; }
.chart-area { position: relative; padding-left: 36px; }
.revenue-svg { width: 100%; height: 180px; display: block; }
.area-shape { animation: area-reveal 1s cubic-bezier(0.4,0,0.2,1); }
@keyframes area-reveal { from { opacity: 0; } to { opacity: 1; } }
.line-shape { stroke-dasharray: 2000; stroke-dashoffset: 2000; animation: line-draw 1.2s cubic-bezier(0.4,0,0.2,1) forwards; }
@keyframes line-draw { to { stroke-dashoffset: 0; } }
.chart-dot { opacity: 0; animation: dot-pop 0.3s ease forwards; }
.chart-dot:nth-child(1) { animation-delay: 0.8s; }
.chart-dot:nth-child(2) { animation-delay: 0.9s; }
.chart-dot:nth-child(3) { animation-delay: 1.0s; }
.chart-dot:nth-child(4) { animation-delay: 1.1s; }
.chart-dot:nth-child(5) { animation-delay: 1.2s; }
.chart-dot:nth-child(6) { animation-delay: 1.3s; }
.chart-dot:nth-child(7) { animation-delay: 1.4s; }
@keyframes dot-pop { from { opacity: 0; r: 0; } to { opacity: 1; r: 3.5; } }
.chart-x-labels { display: flex; justify-content: space-between; padding: 8px 20px 0; font-size: 11px; color: var(--text-muted); font-weight: 500; }
.chart-y-labels { position: absolute; top: 0; left: 0; height: 180px; display: flex; flex-direction: column; justify-content: space-between; padding: 18px 0; font-size: 10px; color: var(--text-muted); text-align: right; width: 32px; }
.chart-values { position: absolute; top: 0; left: 36px; right: 0; height: 180px; pointer-events: none; }
.chart-hover-zone { position: absolute; top: 0; width: 0; height: 100%; pointer-events: all; }
.chart-hover-zone::before { content: ''; position: absolute; left: -20px; top: 0; width: 40px; height: 100%; }
.chart-tooltip { position: absolute; bottom: calc(100% + 8px); left: 50%; transform: translateX(-50%); background: var(--bg-elevated); border: 1px solid var(--border-strong); border-radius: 8px; padding: 8px 12px; display: flex; flex-direction: column; gap: 2px; white-space: nowrap; opacity: 0; pointer-events: none; transition: opacity 0.15s; box-shadow: 0 8px 24px rgba(0,0,0,0.3); z-index: 10; }
.chart-hover-zone:hover .chart-tooltip { opacity: 1; }
.tt-date { font-size: 11px; color: var(--text-muted); }
.tt-val { font-size: 14px; font-weight: 700; color: var(--accent-brass); }
.tt-cnt { font-size: 11px; color: var(--text-secondary); }
</style>
