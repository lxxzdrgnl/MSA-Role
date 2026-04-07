<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo">🍽 레스토랑</div>
      <h2 class="auth-title">회원가입</h2>

      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>이름</label>
          <input
            v-model="form.name"
            type="text"
            placeholder="홍길동"
            required
            autocomplete="name"
          />
        </div>
        <div class="form-group">
          <label>이메일</label>
          <input
            v-model="form.email"
            type="email"
            placeholder="example@email.com"
            required
            autocomplete="email"
          />
        </div>
        <div class="form-group">
          <label>비밀번호</label>
          <input
            v-model="form.password"
            type="password"
            placeholder="8자 이상"
            required
            minlength="8"
            autocomplete="new-password"
          />
        </div>
        <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '처리 중...' : '회원가입' }}
        </button>
      </form>

      <p class="auth-footer">
        이미 계정이 있으신가요?
        <router-link to="/login">로그인</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()

const form = ref({ name: '', email: '', password: '' })
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
  } catch (err) {
    errorMsg.value = err.response?.data?.message || '회원가입 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff5f2 0%, #ffeee8 100%);
  padding: 24px;
}

.auth-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.10);
}

.auth-logo {
  font-size: 28px;
  text-align: center;
  margin-bottom: 8px;
}

.auth-title {
  font-size: 22px;
  font-weight: 700;
  text-align: center;
  color: #222;
  margin-bottom: 28px;
}

.auth-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: #888;
}

.auth-footer a {
  color: #e05c2a;
  text-decoration: none;
  font-weight: 600;
}

.auth-footer a:hover {
  text-decoration: underline;
}
</style>
