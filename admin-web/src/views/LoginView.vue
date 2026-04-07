<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <span class="login-icon">🍽</span>
        <h1>관리자 로그인</h1>
        <p>Restaurant Admin Dashboard</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label class="form-label">아이디 (이메일)</label>
          <input
            v-model="email"
            type="text"
            class="form-input"
            placeholder="admin"
            autocomplete="username"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label">비밀번호</label>
          <input
            v-model="password"
            type="password"
            class="form-input"
            placeholder="••••••••"
            autocomplete="current-password"
            required
          />
        </div>

        <div v-if="errorMsg" class="alert-box alert-error">{{ errorMsg }}</div>

        <button type="submit" class="btn btn-primary login-submit" :disabled="loading">
          <span v-if="loading">로그인 중...</span>
          <span v-else>로그인</span>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authLogin } from '../api.js'

const router = useRouter()
const email = ref('')
const password = ref('')
const errorMsg = ref('')
const loading = ref(false)

async function handleLogin() {
  errorMsg.value = ''
  loading.value = true
  try {
    const res = await authLogin(email.value, password.value)
    const { accessToken, refreshToken, user } = res.data
    const role = user?.role

    if (role !== 'ADMIN') {
      errorMsg.value = '관리자 권한이 없습니다.'
      return
    }

    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken || '')
    localStorage.setItem('role', role)

    router.push('/orders')
  } catch (err) {
    if (err.response?.status === 401) {
      errorMsg.value = '아이디 또는 비밀번호가 올바르지 않습니다.'
    } else {
      errorMsg.value = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1d23 0%, #2d3240 100%);
}

.login-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  width: 100%;
  max-width: 380px;
  box-shadow: 0 20px 60px rgba(0,0,0,.3);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-icon {
  font-size: 40px;
  display: block;
  margin-bottom: 12px;
}

.login-header h1 {
  font-size: 22px;
  font-weight: 700;
  color: #1a1d23;
  margin-bottom: 4px;
}

.login-header p {
  font-size: 13px;
  color: #9ca3af;
}

.login-form .form-group {
  margin-bottom: 16px;
}

.login-submit {
  width: 100%;
  justify-content: center;
  padding: 12px;
  font-size: 15px;
  margin-top: 8px;
}
</style>
