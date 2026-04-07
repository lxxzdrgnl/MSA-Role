<template>
  <div class="auth-shell">
    <div class="blob blob-1"></div>
    <div class="blob blob-2"></div>

    <div class="auth-card fade-up">
      <div class="brand">
        <span class="brand-icon">🔥</span>
        <div>
          <p class="brand-sub">Seoul Kitchen</p>
          <h1 class="display brand-title">JOIN</h1>
        </div>
      </div>

      <form @submit.prevent="handleRegister" class="auth-form">
        <div class="input-group fade-up fade-up-1">
          <label class="input-label">닉네임</label>
          <input v-model="form.nickname" type="text" class="input" placeholder="홍길동" required autocomplete="name" />
        </div>
        <div class="input-group fade-up fade-up-2">
          <label class="input-label">이메일</label>
          <input v-model="form.email" type="email" class="input" placeholder="you@email.com" required autocomplete="email" />
        </div>
        <div class="input-group fade-up fade-up-3">
          <label class="input-label">비밀번호</label>
          <input v-model="form.password" type="password" class="input" placeholder="8자 이상" required minlength="8" autocomplete="new-password" />
        </div>

        <div v-if="errorMsg" class="alert-error">{{ errorMsg }}</div>

        <button type="submit" class="btn btn-primary submit-btn fade-up fade-up-4" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '처리 중...' : '회원가입' }}
        </button>
      </form>

      <p class="auth-footer">
        이미 계정이 있으신가요?
        <router-link to="/login" class="link-accent">로그인</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const form = ref({ nickname: '', email: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')

async function handleRegister() {
  errorMsg.value = ''
  loading.value = true
  try {
    const res = await api.post('/auth/register', form.value)
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
    errorMsg.value = e.response?.data?.message || '회원가입 중 오류가 발생했습니다.'
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
.blob { position: fixed; border-radius: 50%; filter: blur(80px); opacity: 0.15; pointer-events: none; }
.blob-1 { width: 350px; height: 350px; background: #f59e0b; top: -80px; right: -60px; }
.blob-2 { width: 300px; height: 300px; background: var(--accent); bottom: -60px; left: -60px; }

.auth-card {
  width: 100%; max-width: 380px;
  background: rgba(31, 29, 24, 0.85);
  backdrop-filter: blur(24px);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 36px 32px;
  position: relative; z-index: 1;
}

.brand { display: flex; align-items: center; gap: 14px; margin-bottom: 32px; }
.brand-icon { font-size: 36px; line-height: 1; filter: drop-shadow(0 0 16px rgba(249,163,22,0.6)); }
.brand-sub { font-size: 11px; font-weight: 500; color: var(--text-muted); letter-spacing: 0.12em; text-transform: uppercase; }
.brand-title { font-size: 42px; color: var(--text-primary); margin: 0; }

.auth-form { display: flex; flex-direction: column; gap: 14px; }

.submit-btn {
  width: 100%; padding: 13px; font-size: 15px; font-weight: 700;
  letter-spacing: 0.04em; margin-top: 4px;
  display: flex; align-items: center; justify-content: center; gap: 8px;
}

.spinner {
  width: 16px; height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.auth-footer { text-align: center; margin-top: 20px; font-size: 13px; color: var(--text-muted); }
.link-accent { color: var(--accent); font-weight: 600; margin-left: 4px; }
.link-accent:hover { color: var(--accent-soft); }
</style>
