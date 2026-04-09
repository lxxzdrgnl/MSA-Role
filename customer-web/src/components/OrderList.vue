<template>
  <div>
    <div v-if="loading" class="order-list">
      <div v-for="i in 3" :key="i" class="skeleton" style="height:80px;border-radius:var(--radius-lg)"></div>
    </div>
    <div v-else-if="orders.length === 0" class="empty"><span class="empty-glyph">∅</span><span>주문 내역이 없어요</span></div>
    <div v-else class="order-list">
      <div v-for="(o, i) in orders" :key="o.id" class="oc fade-up" :style="`animation-delay:${i*0.04}s`" @click="$emit('select', o.id)">
        <div class="oc-top">
          <span class="oc-id mono">#{{ String(o.id).padStart(4,'0') }}</span>
          <span class="oc-st" :class="`st-${(o.status||'').toLowerCase()}`">{{ SL[o.status]||o.status }}</span>
        </div>
        <div class="oc-date">{{ fmtDate(o.createdAt) }}</div>
        <div v-if="o.items?.length" class="oc-items">{{ o.items.slice(0,3).map(i=>i.menuName||i.name).join(' · ') }}<span v-if="o.items.length>3" class="oc-more"> 외 {{ o.items.length-3 }}개</span></div>
        <div class="oc-bot"><span class="oc-price mono">{{ fmtPrice(o.totalPrice) }}원</span><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="9,18 15,12 9,6"/></svg></div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import { useFormatting } from '../composables/useFormatting'
import { STATUS_LABELS } from '../constants'
defineEmits(['select'])
const orders = ref([]), loading = ref(true)
const SL = STATUS_LABELS
const { formatPrice: fmtPrice, formatDate: fmtDate } = useFormatting()
onMounted(async () => { try { const r = await api.get('/orders',{params:{page:0,size:20}}); orders.value = Array.isArray(r.data)?r.data:(r.data.content||[]) } catch {} finally { loading.value=false } })
</script>
<style scoped>
.order-list { display:flex; flex-direction:column; gap:10px; }
.oc { background:var(--bg-card); border:1px solid var(--border); border-radius:var(--radius-lg); padding:16px 18px; cursor:pointer; transition:var(--transition); }
.oc:hover { border-color:var(--border-hover); box-shadow:var(--shadow-glow); transform:translateY(-2px); }
.oc-top { display:flex; justify-content:space-between; margin-bottom:6px; }
.oc-id { font-size:12px; color:var(--text-muted); font-weight:600; }
.oc-st { font-size:10px; font-weight:600; padding:2px 10px; border-radius:99px; letter-spacing:0.02em; }
.st-pending{background:rgba(255,255,255,.06);color:var(--text-secondary)}.st-accepted{background:rgba(212,134,60,.1);color:var(--accent-soft)}.st-cooking{background:rgba(212,134,60,.15);color:var(--accent-soft)}.st-ready{background:rgba(212,134,60,.1);color:var(--accent)}.st-completed{background:rgba(255,255,255,.04);color:var(--text-muted)}.st-cancelled{background:rgba(255,255,255,.04);color:var(--text-muted);text-decoration:line-through}
.oc-date { font-size:11px; color:var(--text-muted); margin-bottom:8px; }
.oc-items { font-size:12px; color:var(--text-secondary); margin-bottom:10px; }
.oc-more { color:var(--text-muted); font-size:10px; }
.oc-bot { display:flex; justify-content:space-between; align-items:center; padding-top:10px; border-top:1px solid var(--border); }
.oc-price { font-size:15px; font-weight:700; color:var(--accent-soft); }
.oc-bot svg { color:var(--text-muted); }
.empty { display:flex; flex-direction:column; align-items:center; padding:60px 0; gap:10px; color:var(--text-muted); }
.empty-glyph { font-size:40px; opacity:.3; }
</style>
