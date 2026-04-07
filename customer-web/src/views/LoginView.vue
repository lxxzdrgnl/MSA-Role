<template>
  <div class="auth-shell">
    <!-- Ambient background blobs -->
    <div class="blob blob-1"></div>
    <div class="blob blob-2"></div>

    <div class="auth-card fade-up">
      <!-- Logo / brand -->
      <div class="brand">
        <span class="brand-icon">🔥</span>
        <div>
          <p class="brand-sub">Seoul Kitchen</p>
          <h1 class="display brand-title">ORDER</h1>
        </div>
      </div>

      <div v-if="adminBlocked" class="alert-error" style="margin-bottom:16px">
        관리자 계정은 고객 앱을 이용할 수 없습니다.
      </div>

      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="input-group fade-up fade-up-1">
          <label class="input-label">이메일</label>
          <input v-model="form.email" type="email" class="input" placeholder="you@email.com" required autocomplete="email" />
        </div>
        <div class="input-group fade-up fade-up-2">
          <label class="input-label">비밀번호</label>
          <input v-model="form.password" type="password" class="input" placeholder="••••••••" required autocomplete="current-password" />
        </div>

        <div v-if="errorMsg" class="alert-error fade-up">{{ errorMsg }}</div>

        <button type="submit" class="btn btn-primary submit-btn fade-up fade-up-3" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '로그인 중...' : '로그인' }}
        </button>
      </form>

      <p class="auth-footer fade-up fade-up-4">
        처음이신가요?
        <router-link to="/register" class="link-accent">회원가입</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '../api'

const router = useRouter()
const route = useRoute()
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
    localStorage.setItem('token', accessToken)
    localStorage.setItem('userRole', role)
    router.push('/')
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '이메일 또는 비밀번호가 올바르지 않습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-shell {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  position: relative;
}

.blob {
  position: fixed;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.15;
  pointer-events: none;
}
.blob-1 {
  width: 350px; height: 350px;
  background: var(--accent);
  top: -80px; left: -80px;
}
.blob-2 {
  width: 300px; height: 300px;
  background: #f59e0b;
  bottom: -60px; right: -60px;
}

.auth-card {
  width: 100%;
  max-width: 380px;
  background: rgba(31, 29, 24, 0.85);
  backdrop-filter: blur(24px);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 36px 32px;
  position: relative;
  z-index: 1;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 32px;
}
.brand-icon {
  font-size: 36px;
  line-height: 1;
  filter: drop-shadow(0 0 16px rgba(232,92,30,0.6));
}
.brand-sub {
  font-size: 11px;
  font-weight: 500;
  color: var(--text-muted);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
.brand-title {
  font-size: 42px;
  color: var(--text-primary);
  margin: 0;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.submit-btn {
  width: 100%;
  padding: 13px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.04em;
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.spinner {
  width: 16px; height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}
@keyframes spin { to { transform: rotate(360deg); } }

.auth-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: var(--text-muted);
}
.link-accent {
  color: var(--accent);
  font-weight: 600;
  margin-left: 4px;
}
.link-accent:hover { color: var(--accent-soft); }
</style>
