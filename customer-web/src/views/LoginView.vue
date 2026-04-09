<template>
  <div class="login-scene">
    <div class="glow glow-top"></div>
    <div class="glow glow-bottom"></div>

    <div class="login-stage">
      <div class="brand-block">
        <p class="brand-label">Rheon Kitchen</p>
        <h1 class="brand-heading display">Welcome</h1>
      </div>

      <div class="login-card">
        <div v-if="adminBlocked" class="alert-error">
          관리자 계정은 고객 앱을 이용할 수 없습니다.
        </div>

        <form @submit.prevent="handleLogin" class="login-form">
          <div class="field">
            <label class="field-label">이메일</label>
            <div class="field-input-wrap">
              <svg class="field-icon" width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="1.5" y="3" width="13" height="10" rx="2" stroke="currentColor" stroke-width="1.2"/><path d="M2 4l6 4.5L14 4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/></svg>
              <input v-model="form.email" type="email" class="field-input" placeholder="you@email.com" required autocomplete="email" />
            </div>
          </div>

          <div class="field">
            <label class="field-label">비밀번호</label>
            <div class="field-input-wrap">
              <svg class="field-icon" width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="3" y="7" width="10" height="7" rx="2" stroke="currentColor" stroke-width="1.2"/><path d="M5.5 7V5a2.5 2.5 0 0 1 5 0v2" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/><circle cx="8" cy="10.5" r="1" fill="currentColor"/></svg>
              <input v-model="form.password" type="password" class="field-input" placeholder="비밀번호 입력" required autocomplete="current-password" />
            </div>
          </div>

          <div v-if="errorMsg" class="alert-error">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <circle cx="7" cy="7" r="6" stroke="currentColor" stroke-width="1.3"/>
              <path d="M7 4V7.5M7 9.5V10" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>
            </svg>
            {{ errorMsg }}
          </div>

          <button type="submit" class="submit-btn" :disabled="loading">
            <span v-if="loading" class="spinner"></span>
            <span>{{ loading ? '로그인 중' : '로그인' }}</span>
            <svg v-if="!loading" width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M3 8h10m-4-4 4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
        </form>

        <div class="login-footer">
          <span>처음이신가요?</span>
          <router-link to="/register" class="link-register">회원가입 →</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '../api'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const route = useRoute()
const { login } = useAuth()
const form = ref({ email: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')
const adminBlocked = ref(false)

onMounted(() => { if (route.query.adminBlocked) adminBlocked.value = true })

async function handleLogin() {
  errorMsg.value = ''
  loading.value = true
  try {
    const res = await api.post('/auth/login', form.value)
    const { accessToken, user } = res.data
    const role = user?.role
    if (role === 'ADMIN') {
      errorMsg.value = '관리자 계정은 고객 앱을 이용할 수 없습니다.'
      return
    }
    login(accessToken, role)
    router.push('/')
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '이메일 또는 비밀번호가 올바르지 않습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-scene {
  min-height: 100vh;
  display: flex; align-items: center; justify-content: center;
  padding: 24px; position: relative; overflow: hidden;
  background: radial-gradient(ellipse at 50% 100%, rgba(212,134,60,0.06) 0%, transparent 60%);
}

.glow { position: fixed; border-radius: 50%; pointer-events: none; filter: blur(100px); }
.glow-top { width: 500px; height: 400px; background: radial-gradient(circle, rgba(212,134,60,0.12) 0%, transparent 70%); top: -200px; left: 50%; transform: translateX(-50%); }
.glow-bottom { width: 600px; height: 300px; background: radial-gradient(circle, rgba(212,134,60,0.08) 0%, transparent 70%); bottom: -150px; left: 50%; transform: translateX(-50%); }

.login-stage {
  position: relative; z-index: 1; width: 100%; max-width: 380px;
  display: flex; flex-direction: column; align-items: center;
  animation: stageIn 0.6s cubic-bezier(0.4, 0, 0.2, 1) both;
}
@keyframes stageIn { from { opacity: 0; transform: translateY(20px); } }

.brand-block { text-align: center; margin-bottom: 36px; }
.brand-label { font-size: 11px; font-weight: 600; color: var(--text-muted); letter-spacing: 0.18em; text-transform: uppercase; margin-bottom: 6px; }
.brand-heading { font-size: 24px; font-weight: 700; color: var(--text-primary); letter-spacing: -0.5px; margin: 0; }

.login-card {
  width: 100%;
  background: rgba(26, 25, 24, 0.7);
  backdrop-filter: blur(32px); -webkit-backdrop-filter: blur(32px);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 32px 28px;
  box-shadow: 0 1px 0 0 rgba(255,255,255,0.04) inset, 0 20px 60px rgba(0,0,0,0.4);
}

.login-form { display: flex; flex-direction: column; gap: 18px; }
.field-label { display: block; font-size: 12px; font-weight: 600; color: var(--text-secondary); margin-bottom: 7px; letter-spacing: 0.3px; text-transform: uppercase; }
.field-input-wrap { position: relative; }
.field-icon { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: var(--text-muted); pointer-events: none; transition: color 0.2s; }
.field-input {
  width: 100%; padding: 12px 14px 12px 40px;
  background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-md);
  color: var(--text-primary); font-family: inherit; font-size: 14px; outline: none; transition: all 0.2s;
}
.field-input::placeholder { color: var(--text-muted); }
.field-input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-glow); }
.field-input-wrap:focus-within .field-icon { color: var(--accent); }

.submit-btn {
  width: 100%; padding: 14px; background: var(--accent); color: var(--text-inverse);
  border: none; border-radius: var(--radius-md); font-size: 14px; font-weight: 700;
  font-family: inherit; cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  transition: all 0.2s; box-shadow: 0 2px 16px rgba(212,134,60,0.25); margin-top: 4px; letter-spacing: -0.2px;
}
.submit-btn:hover:not(:disabled) { box-shadow: 0 4px 24px rgba(212,134,60,0.35); transform: translateY(-1px); }
.submit-btn:active:not(:disabled) { transform: translateY(0); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }

.spinner { width: 16px; height: 16px; border: 2px solid rgba(17,17,16,0.2); border-top-color: var(--text-inverse); border-radius: 50%; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.alert-error {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-radius: var(--radius-md);
  background: rgba(220,38,38,0.1); color: #fca5a5; font-size: 13px;
  border: 1px solid rgba(220,38,38,0.2);
  animation: shake 0.4s cubic-bezier(0.36, 0.07, 0.19, 0.97);
}
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}

.login-footer {
  text-align: center; margin-top: 24px; padding-top: 20px;
  border-top: 1px solid var(--border);
  font-size: 13px; color: var(--text-muted);
}
.link-register { color: var(--accent); font-weight: 600; margin-left: 6px; transition: color 0.15s; }
.link-register:hover { color: var(--accent-soft); }
</style>
