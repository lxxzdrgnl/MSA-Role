<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="odm">
      <div class="odm-head">
        <div>
          <div class="chat-badge">ORDER</div>
          <h3 class="display odm-title">#{{ String(orderId).padStart(4,'0') }}</h3>
        </div>
        <button class="modal-x" @click="$emit('close')">✕</button>
      </div>

      <div v-if="loading" class="odm-loading"><span class="spinner"></span></div>

      <template v-else-if="order">
        <!-- Status -->
        <div class="sb" :class="`sb-${(order.status||'').toLowerCase()}`">
          <span class="sb-icon">{{ ST[order.status]?.icon||'?' }}</span>
          <div><div class="sb-label">{{ ST[order.status]?.label||order.status }}</div><div class="sb-desc">{{ ST[order.status]?.desc }}</div></div>
        </div>

        <!-- Info -->
        <div class="odm-info">
          <div class="oi"><span class="oi-k">주문 시각</span><span class="oi-v">{{ fmtDate(order.createdAt) }}</span></div>
          <div class="oi"><span class="oi-k">총 금액</span><span class="oi-v accent mono">{{ fmtPrice(order.totalPrice) }}원</span></div>
        </div>

        <!-- Items -->
        <div class="odm-items">
          <div v-for="it in order.items" :key="it.id" class="oi-row">
            <span class="oi-name">{{ it.menuName }}</span>
            <span class="oi-qty mono">×{{ it.quantity }}</span>
            <span class="oi-price mono">{{ fmtPrice(it.price*it.quantity) }}원</span>
          </div>
        </div>

        <!-- Review section -->
        <div v-if="order.status!=='CANCELLED'" class="rv-section">
          <!-- Existing review (read mode) -->
          <template v-if="existingReview && !editing">
            <div class="rv-title">내 리뷰</div>
            <div class="rv-existing">
              <div class="rv-ex-top">
                <span class="rv-ex-stars">{{ '★'.repeat(existingReview.rating) }}{{ '☆'.repeat(5 - existingReview.rating) }}</span>
                <span class="rv-ex-date">{{ fmtDate(existingReview.createdAt) }}</span>
              </div>
              <p class="rv-ex-content">{{ existingReview.content }}</p>
              <div class="rv-ex-actions">
                <button class="rv-act-btn" @click="startEdit">수정</button>
                <button class="rv-act-btn rv-act-del" @click="deleteRv">삭제</button>
              </div>
            </div>
          </template>

          <!-- Write / Edit mode -->
          <template v-else-if="!existingReview || editing">
            <div class="rv-title">{{ editing ? '리뷰 수정' : '리뷰 작성' }}</div>
            <div class="star-row"><button v-for="n in 5" :key="n" class="star" :class="{on:rvRating>=n}" @click="rvRating=n">★</button></div>
            <div class="kw-row">
              <button v-for="k in KW" :key="k" class="kw" :class="{sel:rvKws.includes(k)}" @click="togKw(k)">{{ k }}</button>
            </div>
            <div class="kw-custom">
              <div v-for="k in customKws" :key="k" class="kw sel">{{ k }} <span class="kw-x" @click="removeCustom(k)">×</span></div>
              <form class="kw-input-form" @submit.prevent="addCustomKw">
                <input v-model="customKw" class="kw-input" placeholder="직접 입력..." />
                <button type="submit" class="kw-add-btn">+</button>
              </form>
            </div>
            <button class="btn-draft" :disabled="draftL||rvRating===0" @click="genDraft"><span v-if="draftL" class="spinner"></span>{{ draftL?'AI 작성 중...':'✦ AI 리뷰 초안' }}</button>
            <textarea v-model="rvText" class="rv-ta" rows="3" placeholder="리뷰를 작성하세요..."></textarea>
            <div class="rv-btn-row">
              <button v-if="editing" class="rv-act-btn" @click="editing=false">취소</button>
              <button class="btn-rv-submit" :disabled="!rvText.trim()||rvSubmitting" @click="submitRv">{{ rvSubmitting ? '등록 중...' : editing ? '수정 완료' : '리뷰 등록' }}</button>
            </div>
          </template>
        </div>

        <!-- WS -->
        <div v-if="wsMsg" class="ws-bar"><span class="ws-dot"></span>{{ wsMsg }}</div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import api from '../api'

const props = defineProps({ orderId: Number })
const emit = defineEmits(['close'])

const order = ref(null), loading = ref(true), wsMsg = ref('')
let ws = null

const ST = {
  PENDING:{label:'접수 대기',icon:'⏳',desc:'주문이 접수되길 기다리고 있어요'},
  ACCEPTED:{label:'주문 수락',icon:'✓',desc:'주문이 접수되었습니다'},
  COOKING:{label:'조리 중',icon:'🔥',desc:'셰프가 조리하고 있어요'},
  READY:{label:'준비 완료',icon:'✨',desc:'음식이 준비되었습니다!'},
  COMPLETED:{label:'완료',icon:'✓',desc:'수령 완료'},
  CANCELLED:{label:'취소됨',icon:'✕',desc:'주문이 취소되었습니다'},
}
const fmtPrice = p => Number(p).toLocaleString('ko-KR')
const fmtDate = s => s ? new Date(s).toLocaleString('ko-KR',{month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'}) : ''

// Review
const existingReview = ref(null)
const existingReviews = ref([])
const editing = ref(false)
const rvRating = ref(0), rvText = ref(''), rvKws = ref([]), draftL = ref(false), rvSubmitting = ref(false)
const KW = ['맛있다','양이 많다','빠르다','국물이 좋다','매콤하다','신선하다','가성비 좋다']
const customKw = ref('')
const customKws = ref([])
function addCustomKw() {
  const v = customKw.value.trim()
  if (v && !rvKws.value.includes(v) && !customKws.value.includes(v)) { customKws.value.push(v); rvKws.value.push(v) }
  customKw.value = ''
}
function removeCustom(k) { customKws.value = customKws.value.filter(x=>x!==k); rvKws.value = rvKws.value.filter(x=>x!==k) }
function togKw(k) { const i=rvKws.value.indexOf(k); i>=0?rvKws.value.splice(i,1):rvKws.value.push(k) }

async function genDraft() {
  draftL.value = true
  try {
    const name = order.value.items?.map(i=>i.menuName).join(', ')||''
    const r = await api.post('/reviews/generate',{menuName:name,rating:rvRating.value,keywords:rvKws.value})
    rvText.value = r.data.draft||r.data.review||''
  } catch {} finally { draftL.value = false }
}

function startEdit() {
  rvRating.value = existingReview.value.rating
  rvText.value = existingReview.value.content
  editing.value = true
}

async function deleteRv() {
  if (!confirm('리뷰를 삭제하시겠습니까?')) return
  try {
    for (const rv of existingReviews.value) {
      await api.delete(`/reviews/${rv.id}`)
    }
    existingReview.value = null
    existingReviews.value = []
  } catch {}
}

async function submitRv() {
  if(!rvText.value.trim()) return
  rvSubmitting.value = true
  try {
    if (editing.value && existingReview.value) {
      // Delete all existing reviews for this order
      for (const rv of existingReviews.value) {
        try { await api.delete(`/reviews/${rv.id}`) } catch {}
      }
    }
    // Register review for each menu item in the order
    let lastRes = null
    for (const item of (order.value.items || [])) {
      lastRes = await api.post('/reviews', {
        orderId: order.value.id,
        menuId: item.menuId || item.id,
        menuName: item.menuName || item.name || '',
        rating: rvRating.value,
        content: rvText.value
      })
    }
    if (lastRes) existingReview.value = lastRes.data
    await loadExistingReview()
    editing.value = false
  } catch {} finally { rvSubmitting.value=false }
}

async function loadExistingReview() {
  try {
    const r = await api.get('/reviews/by-order', { params: { orderId: props.orderId } })
    const reviews = r.data.content || []
    existingReviews.value = reviews
    if (reviews.length > 0) existingReview.value = reviews[0]
  } catch {}
}

onMounted(async () => {
  try { order.value = (await api.get(`/orders/${props.orderId}`)).data } catch {} finally { loading.value = false }
  loadExistingReview()
  const proto = location.protocol==='https:'?'wss':'ws'
  ws = new WebSocket(`${proto}://${location.host}/ws/orders/${props.orderId}`)
  ws.onmessage = e => { try { const d=JSON.parse(e.data); if(d.status){order.value.status=d.status; wsMsg.value=ST[d.status]?.label||d.status; setTimeout(()=>{wsMsg.value=''},5000)} } catch{} }
})
onUnmounted(() => { if(ws) ws.close() })
</script>

<style scoped>
.modal-backdrop { position:fixed; inset:0; background:rgba(0,0,0,.6); backdrop-filter:blur(4px); display:flex; align-items:center; justify-content:center; z-index:300; }
.odm { background:var(--bg-elevated); border:1px solid var(--border); border-radius:var(--radius-xl); width:460px; max-height:85vh; overflow-y:auto; box-shadow:0 24px 64px rgba(0,0,0,.6); }
.odm-head { display:flex; justify-content:space-between; align-items:center; padding:24px 24px 16px; border-bottom:1px solid var(--border); }
.chat-badge { font-size:9px; font-weight:700; color:var(--accent); letter-spacing:.16em; margin-bottom:2px; }
.odm-title { font-size:30px; color:var(--text-primary); margin:0; }
.modal-x { width:32px; height:32px; border-radius:99px; background:var(--bg-subtle); border:1px solid var(--border); color:var(--text-secondary); display:flex; align-items:center; justify-content:center; font-size:14px; cursor:pointer; transition:var(--transition); }
.modal-x:hover { color:var(--text-primary); border-color:var(--border-hover); }
.odm-loading { display:flex; justify-content:center; padding:40px; }

/* Status */
.sb { display:flex; align-items:center; gap:14px; padding:16px 24px; margin:16px 20px 0; border-radius:var(--radius-md); border:1px solid var(--border); }
.sb-icon { font-size:22px; width:40px; height:40px; border-radius:var(--radius-sm); display:flex; align-items:center; justify-content:center; }
.sb-label { font-size:14px; font-weight:700; color:var(--text-primary); }
.sb-desc { font-size:11px; color:var(--text-secondary); }
.sb-pending{background:rgba(251,191,36,.05)}.sb-pending .sb-icon{background:rgba(251,191,36,.12)}
.sb-accepted{background:rgba(96,165,250,.05)}.sb-accepted .sb-icon{background:rgba(96,165,250,.12)}
.sb-cooking{background:rgba(251,146,60,.05)}.sb-cooking .sb-icon{background:rgba(251,146,60,.12)}
.sb-ready{background:rgba(74,222,128,.05)}.sb-ready .sb-icon{background:rgba(74,222,128,.12)}
.sb-completed{background:var(--bg-card)}.sb-completed .sb-icon{background:var(--bg-subtle)}
.sb-cancelled{background:rgba(248,113,113,.05)}.sb-cancelled .sb-icon{background:rgba(248,113,113,.12)}

/* Info */
.odm-info { display:grid; grid-template-columns:1fr 1fr; gap:10px; padding:16px 20px 0; }
.oi { background:var(--bg-card); border:1px solid var(--border); border-radius:var(--radius-sm); padding:12px 14px; }
.oi-k { display:block; font-size:10px; color:var(--text-muted); font-weight:600; letter-spacing:.08em; text-transform:uppercase; margin-bottom:4px; }
.oi-v { font-size:14px; font-weight:600; color:var(--text-primary); }
.oi-v.accent { color:var(--accent-soft); }

/* Items */
.odm-items { margin:16px 20px 0; background:var(--bg-card); border:1px solid var(--border); border-radius:var(--radius-md); overflow:hidden; }
.oi-row { display:flex; align-items:center; gap:10px; padding:10px 14px; border-bottom:1px solid var(--border); }
.oi-row:last-child { border-bottom:none; }
.oi-name { flex:1; font-size:13px; font-weight:500; color:var(--text-primary); }
.oi-qty { font-size:12px; color:var(--text-muted); }
.oi-price { font-size:13px; font-weight:600; color:var(--accent-soft); }

/* Review */
.rv-section { margin:16px 20px 20px; background:var(--bg-card); border:1px solid var(--border); border-radius:var(--radius-md); padding:16px; display:flex; flex-direction:column; gap:12px; }
.rv-title { font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:.08em; }
.star-row { display:flex; gap:2px; }
.star { font-size:24px; color:var(--bg-subtle); background:none; border:none; cursor:pointer; transition:var(--transition); padding:0; line-height:1; }
.star.on { color:#fbbf24; text-shadow:0 0 6px rgba(251,191,36,.35); }
.kw-row { display:flex; flex-wrap:wrap; gap:5px; }
.kw { background:var(--bg-subtle); border:1px solid var(--border); border-radius:99px; padding:4px 10px; font-size:11px; color:var(--text-secondary); font-family:inherit; cursor:pointer; transition:var(--transition); }
.kw.sel { background:var(--accent-bg); border-color:var(--accent); color:var(--accent-soft); }
.kw-custom { display:flex; flex-wrap:wrap; gap:5px; align-items:center; }
.kw-custom .kw { display:flex; align-items:center; gap:4px; }
.kw-x { cursor:pointer; font-size:12px; opacity:.6; }
.kw-x:hover { opacity:1; }
.kw-input-form { display:flex; gap:4px; align-items:center; }
.kw-input { width:100px; padding:4px 10px; background:var(--bg-subtle); border:1px dashed var(--border); border-radius:99px; font-size:11px; color:var(--text-primary); font-family:inherit; outline:none; transition:var(--transition); }
.kw-input:focus { border-color:var(--accent); width:130px; }
.kw-input::placeholder { color:var(--text-muted); }
.kw-add-btn { width:24px; height:24px; border-radius:99px; background:var(--accent); border:none; color:#fff; font-size:14px; display:flex; align-items:center; justify-content:center; cursor:pointer; flex-shrink:0; }
.btn-draft { background:var(--bg-elevated); border:1px dashed var(--accent); color:var(--accent-soft); padding:8px 14px; border-radius:var(--radius-sm); font-size:12px; font-weight:600; font-family:inherit; cursor:pointer; transition:var(--transition); display:flex; align-items:center; justify-content:center; gap:6px; }
.btn-draft:disabled { opacity:.4; cursor:not-allowed; }
.rv-ta { width:100%; padding:8px 12px; background:var(--bg-elevated); border:1px solid var(--border); border-radius:var(--radius-sm); color:var(--text-primary); font-family:inherit; font-size:12px; resize:vertical; min-height:60px; outline:none; }
.rv-ta:focus { border-color:var(--accent); }
.btn-rv-submit { background:var(--accent); color:#fff; border:none; border-radius:var(--radius-sm); padding:10px; font-size:13px; font-weight:700; font-family:inherit; cursor:pointer; transition:var(--transition); }
.btn-rv-submit:hover:not(:disabled) { background:var(--accent-soft); }
.btn-rv-submit:disabled { opacity:.4; cursor:not-allowed; }
.rv-existing { background:var(--bg-elevated); border:1px solid var(--border); border-radius:var(--radius-sm); padding:12px 14px; }
.rv-ex-top { display:flex; justify-content:space-between; align-items:center; margin-bottom:8px; }
.rv-ex-stars { font-size:14px; color:#fbbf24; letter-spacing:1px; }
.rv-ex-date { font-size:11px; color:var(--text-muted); }
.rv-ex-content { font-size:13px; color:var(--text-secondary); line-height:1.5; margin-bottom:10px; }
.rv-ex-actions { display:flex; gap:8px; }
.rv-act-btn { background:var(--bg-subtle); border:1px solid var(--border); border-radius:var(--radius-sm); padding:5px 12px; font-size:11px; color:var(--text-secondary); font-family:inherit; cursor:pointer; transition:var(--transition); }
.rv-act-btn:hover { border-color:var(--border-hover); color:var(--text-primary); }
.rv-act-del { color:#f87171; }
.rv-act-del:hover { border-color:#f87171; background:rgba(248,113,113,.08); }
.rv-btn-row { display:flex; gap:8px; justify-content:flex-end; }
.rv-btn-row .rv-act-btn { flex:0; }
.rv-btn-row .btn-rv-submit { flex:1; }

.rv-done { display:flex; align-items:center; gap:8px; color:#4ade80; font-weight:600; font-size:13px; }
.rv-done-ico { width:24px; height:24px; background:rgba(74,222,128,.12); border-radius:99px; display:flex; align-items:center; justify-content:center; font-size:12px; }

/* WS */
.ws-bar { margin:12px 20px 20px; padding:10px 14px; background:rgba(74,222,128,.08); border:1px solid rgba(74,222,128,.2); border-radius:var(--radius-sm); font-size:12px; color:#4ade80; display:flex; align-items:center; gap:8px; }
.ws-dot { width:6px; height:6px; background:#4ade80; border-radius:99px; animation:pulse 1.5s infinite; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.4} }
</style>
