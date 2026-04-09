<template>
  <aside class="chat-panel" :class="{ collapsed: !open }">
    <button class="chat-toggle" @click="open = !open">
      <svg v-if="open" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><polyline points="9,18 15,12 9,6"/></svg>
      <template v-else><span class="toggle-spark">✦</span><span class="toggle-label">AI</span></template>
    </button>

    <div v-show="open" class="chat-inner">
      <div class="chat-header">
        <div class="chat-badge">AI POWERED</div>
        <h2 class="display chat-title">Recommend</h2>
        <p class="chat-sub">원하는 분위기나 맛을 알려주세요</p>
      </div>

      <div class="chat-messages" ref="scrollEl">
        <div v-if="history.length === 0" class="chat-welcome">
          <p>무엇을 드시고 싶으세요?</p>
        </div>
        <div class="quick-chips">
          <button v-for="q in quickQueries" :key="q" class="quick-chip" @click="ask(q)">{{ q }}</button>
        </div>
        <div v-for="(msg, i) in history" :key="i" class="chat-msg" :class="msg.role">
          <div class="msg-bubble">
            <template v-if="msg.menus">
              <div class="msg-text">{{ msg.text }}</div>
              <div class="rec-cards-inline">
                <div v-for="m in msg.menus" :key="m.id" class="rec-inline" @click="$emit('open-menu', m)">
                  <img v-if="m.imageUrl" :src="m.imageUrl" class="rec-thumb" @error="e => e.target.style.display='none'" />
                  <div v-else class="rec-thumb-empty">{{ categoryEmoji(m.categoryName) }}</div>
                  <div class="rec-inline-info">
                    <span class="rec-inline-name">{{ m.name }}</span>
                    <span class="rec-inline-price mono">{{ formatPrice(m.price) }}원</span>
                  </div>
                  <button class="btn-cart btn-cart-sm" @click.stop="$emit('add-to-cart', m)">담기</button>
                </div>
              </div>
            </template>
            <template v-else>{{ msg.text }}</template>
          </div>
        </div>
        <div v-if="loading" class="chat-msg assistant">
          <div class="msg-bubble"><span class="spinner"></span> 추천 중...</div>
        </div>
      </div>

      <form class="chat-input-area" @submit.prevent="send">
        <input v-model="query" class="chat-field" placeholder="매운 거 추천해줘..." :disabled="loading" />
        <button type="submit" class="chat-send" :disabled="loading || !query.trim()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22,2 15,22 11,13 2,9"/></svg>
        </button>
      </form>
    </div>
  </aside>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import api from '../api'
import { useFormatting } from '../composables/useFormatting'
import { categoryEmoji, QUICK_CHAT_QUERIES } from '../constants'

const props = defineProps({ menus: Array })
defineEmits(['open-menu', 'add-to-cart'])

const { formatPrice } = useFormatting()
const quickQueries = QUICK_CHAT_QUERIES

const open = ref(true)
const query = ref('')
const loading = ref(false)
const history = ref([])
const scrollEl = ref(null)

function ask(q) { query.value = q; send() }

async function send() {
  if (!query.value.trim()) return
  const userMsg = query.value.trim()
  history.value.push({ role: 'user', text: userMsg })
  query.value = ''
  loading.value = true
  await nextTick()
  scroll()

  try {
    const r = await api.post('/recommendations/chat', { message: userMsg })
    const data = r.data
    const menuIds = data.menu_ids || []
    const reason = data.reason || ''

    if (menuIds.length > 0) {
      const matched = menuIds.map(id => props.menus.find(m => m.id === id)).filter(Boolean)
      if (matched.length > 0) {
        history.value.push({ role: 'assistant', text: reason || '이런 메뉴는 어떨까요?', menus: matched })
      } else {
        const fetched = []
        for (const id of menuIds) {
          try { fetched.push((await api.get(`/menus/${id}`)).data) } catch {}
        }
        history.value.push({ role: 'assistant', text: reason || (fetched.length ? '추천 메뉴입니다!' : '추천할 메뉴를 찾지 못했어요.'), menus: fetched.length ? fetched : undefined })
      }
    } else {
      history.value.push({ role: 'assistant', text: reason || '추천 결과가 없어요. 다른 키워드를 시도해보세요.' })
    }
  } catch (e) {
    history.value.push({ role: 'assistant', text: e.response?.data?.message || 'AI 추천 중 오류가 발생했습니다.' })
  } finally {
    loading.value = false
    await nextTick()
    scroll()
  }
}

function scroll() {
  if (scrollEl.value) scrollEl.value.scrollTop = scrollEl.value.scrollHeight
}
</script>

<style scoped>
.chat-panel { background: var(--bg-elevated); border-left: 1px solid var(--border); display: flex; flex-direction: column; position: sticky; top: 0; height: 100vh; overflow: hidden; transition: var(--transition); }
.chat-panel.collapsed { background: var(--bg-base); }
.chat-toggle { position: absolute; top: 16px; left: -16px; width: 32px; height: 32px; background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 99px; display: flex; align-items: center; justify-content: center; color: var(--text-secondary); z-index: 10; transition: var(--transition); box-shadow: 0 2px 8px rgba(0,0,0,0.3); cursor: pointer; }
.chat-toggle:hover { color: var(--accent); border-color: var(--border-hover); }
.collapsed .chat-toggle { position: static; width: 100%; height: auto; border-radius: 0; border: none; border-bottom: 1px solid var(--border); flex-direction: column; gap: 6px; padding: 16px 0; background: transparent; }
.toggle-spark { font-size: 18px; color: var(--accent); }
.toggle-label { font-size: 10px; font-weight: 700; letter-spacing: 0.12em; color: var(--text-muted); }
.chat-inner { display: flex; flex-direction: column; height: 100%; overflow: hidden; }
.chat-header { padding: 24px 20px 16px; border-bottom: 1px solid var(--border); }
.chat-badge { font-size: 9px; font-weight: 700; color: var(--accent); letter-spacing: 0.16em; margin-bottom: 4px; }
.chat-title { font-size: 28px; color: var(--text-primary); margin: 0; }
.chat-sub { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 10px; }
.chat-welcome { text-align: center; padding: 32px 0 16px; color: var(--text-secondary); font-size: 14px; }
.quick-chips { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; margin-top: 16px; }
.quick-chip { background: var(--bg-card); border: 1px solid var(--border); border-radius: 99px; padding: 6px 14px; font-size: 12px; color: var(--text-secondary); transition: var(--transition); font-family: inherit; cursor: pointer; }
.quick-chip:hover { border-color: var(--accent); color: var(--accent-soft); background: var(--accent-bg); }
.chat-msg { display: flex; }
.chat-msg.user { justify-content: flex-end; }
.chat-msg.assistant { justify-content: flex-start; }
.msg-bubble { max-width: 90%; padding: 10px 14px; border-radius: var(--radius-md); font-size: 13px; line-height: 1.5; display: flex; flex-direction: column; gap: 8px; }
.user .msg-bubble { background: var(--accent); color: #fff; border-bottom-right-radius: 4px; }
.assistant .msg-bubble { background: var(--bg-card); border: 1px solid var(--border); color: var(--text-primary); border-bottom-left-radius: 4px; }
.msg-text { margin-bottom: 4px; }
.rec-cards-inline { display: flex; flex-direction: column; gap: 6px; }
.rec-inline { display: flex; align-items: center; gap: 10px; background: var(--bg-subtle); border-radius: var(--radius-sm); padding: 8px 10px; cursor: pointer; transition: var(--transition); }
.rec-inline:hover { background: var(--bg-hover); }
.rec-thumb { width: 40px; height: 40px; border-radius: var(--radius-sm); object-fit: cover; flex-shrink: 0; }
.rec-thumb-empty { width: 40px; height: 40px; border-radius: var(--radius-sm); background: var(--bg-card); display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; opacity: 0.5; }
.rec-inline-info { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.rec-inline-name { font-size: 13px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rec-inline-price { font-size: 11px; color: var(--accent-soft); }
.btn-cart { background: var(--accent); color: #fff; border: none; border-radius: var(--radius-sm); padding: 7px 16px; font-size: 13px; font-weight: 600; font-family: inherit; transition: var(--transition); cursor: pointer; }
.btn-cart:hover:not(:disabled) { background: var(--accent-soft); }
.btn-cart-sm { padding: 4px 10px; font-size: 11px; }
.chat-input-area { padding: 12px 16px 16px; border-top: 1px solid var(--border); display: flex; gap: 8px; }
.chat-field { flex: 1; padding: 10px 14px; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-md); color: var(--text-primary); font-family: inherit; font-size: 13px; outline: none; transition: var(--transition); }
.chat-field::placeholder { color: var(--text-muted); }
.chat-field:focus { border-color: var(--accent); }
.chat-send { width: 40px; height: 40px; background: var(--accent); border: none; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; color: #fff; transition: var(--transition); flex-shrink: 0; cursor: pointer; }
.chat-send:hover:not(:disabled) { background: var(--accent-soft); }
.chat-send:disabled { opacity: 0.35; }
</style>
