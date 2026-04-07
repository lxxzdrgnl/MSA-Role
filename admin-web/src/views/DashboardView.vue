<template>
  <div class="page">
    <h2 class="page-title">대시보드</h2>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">오늘 매출</div>
        <div class="stat-value">{{ revenue.totalRevenue?.toLocaleString() ?? '-' }}원</div>
        <div class="stat-sub">주문 {{ revenue.orderCount ?? '-' }}건</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">진행 중 주문</div>
        <div class="stat-value">{{ activeCount ?? '-' }}</div>
        <div class="stat-sub">건</div>
      </div>
      <div class="stat-card" :class="`congestion-${congestion.level?.toLowerCase()}`">
        <div class="stat-label">혼잡도</div>
        <div class="stat-value">{{ CONGESTION_LABELS[congestion.level] ?? '-' }}</div>
        <div class="stat-sub">{{ congestion.message }}</div>
      </div>
    </div>

    <div class="section">
      <h3>베스트셀러 TOP 10</h3>
      <div v-if="bestSellers.length === 0" class="empty">데이터 없음</div>
      <table v-else class="table">
        <thead>
          <tr><th>#</th><th>메뉴명</th><th>판매 수</th></tr>
        </thead>
        <tbody>
          <tr v-for="(item, i) in bestSellers" :key="item.menuId">
            <td>{{ i + 1 }}</td>
            <td>{{ item.menuName }}</td>
            <td>{{ item.soldCount }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import api from '../api'

const revenue = ref({})
const activeCount = ref(null)
const congestion = ref({})
const bestSellers = ref([])

const CONGESTION_LABELS = { LOW: '여유', MEDIUM: '보통', HIGH: '혼잡' }

async function loadStats() {
  const today = new Date().toISOString().slice(0, 10)
  const [rev, active, cong, best] = await Promise.allSettled([
    api.get(`/orders/stats/revenue?period=daily&from=${today}&to=${today}`),
    api.get('/orders/active'),
    api.get('/operations/congestion'),
    api.get('/orders/stats/best-sellers?period=weekly&limit=10'),
  ])
  if (rev.status === 'fulfilled') {
    const revData = rev.value.data
    if (Array.isArray(revData) && revData.length > 0) {
      revenue.value = revData[0]
    }
  }
  if (active.status === 'fulfilled') {
    const activeData = active.value.data
    activeCount.value = Array.isArray(activeData) ? activeData.length : 0
  }
  if (cong.status === 'fulfilled') congestion.value = cong.value.data
  if (best.status === 'fulfilled') bestSellers.value = best.value.data
}

let timer = null
onMounted(() => {
  loadStats()
  timer = setInterval(loadStats, 30000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.page { padding: 24px; }
.page-title { font-size: 22px; font-weight: 700; margin-bottom: 24px; }

.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 32px; }
.stat-card { background: #fff; border-radius: 12px; padding: 20px 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.07); }
.stat-label { font-size: 13px; color: #888; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a1a; }
.stat-sub { font-size: 12px; color: #aaa; margin-top: 4px; }
.congestion-low .stat-value { color: #2e7d32; }
.congestion-medium .stat-value { color: #e65100; }
.congestion-high .stat-value { color: #c62828; }

.section h3 { font-size: 16px; font-weight: 700; margin-bottom: 12px; }
.table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.table th, .table td { padding: 10px 16px; text-align: left; font-size: 14px; border-bottom: 1px solid #f0f0f0; }
.table th { background: #f8f8f8; font-weight: 600; color: #555; }
.empty { color: #aaa; padding: 24px; text-align: center; }
</style>
