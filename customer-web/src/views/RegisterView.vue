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
          <div class="nickname-row">
            <input v-model="form.nickname" type="text" class="input" placeholder="닉네임" required autocomplete="name" />
            <button type="button" class="nickname-refresh" @click="form.nickname = randomNickname()" title="랜덤 닉네임">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M1 4v6h6"/><path d="M23 20v-6h-6"/><path d="M20.49 9A9 9 0 005.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 013.51 15"/></svg>
            </button>
          </div>
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
import { useAuth } from '../composables/useAuth'

const { login } = useAuth()

const router = useRouter()

const adjectives = ['씩씩한','용감한','멋있는','귀여운','날쌘','똑똑한','든든한','활발한','느긋한','다정한','엉뚱한','수줍은','당당한','재빠른','힘센','졸린','배고픈','행복한','신나는','조용한','명랑한','유쾌한','진지한','겸손한','포근한','산뜻한','느릿한','덤벙대는','까칠한','소심한','호기심많은','장난꾸러기','수다쟁이','먹보','잠꾸러기','몽글몽글','반짝이는','따뜻한','시원한','새침한','덜렁대는','고집센','심심한','부지런한','게으른','투덜대는','기운찬','차분한','천진난만한','의젓한']
const animals = ['토끼','기린','고양이','강아지','다람쥐','수달','펭귄','코알라','판다','여우','사슴','호랑이','부엉이','고슴도치','햄스터','앵무새','거북이','돌고래','두루미','카피바라','알파카','치타','미어캣','레서판다','북극곰','해마','나무늘보','오리너구리','라쿤','플라밍고','카멜레온','해달','두더지','까마귀','참새','오리','올빼미','물개','고래','하마','코끼리','얼룩말','사자','독수리','비버','청설모','오소리','고라니','수리부엉이','날다람쥐','전북대생','당나귀']

function randomNickname() {
  const adj = adjectives[Math.floor(Math.random() * adjectives.length)]
  const animal = animals[Math.floor(Math.random() * animals.length)]
  return `${adj} ${animal}`
}

const form = ref({ nickname: randomNickname(), email: '', password: '' })
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
    login(accessToken, role)
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

.nickname-row { display: flex; gap: 8px; }
.nickname-row .input { flex: 1; }
.nickname-refresh { width: 40px; flex-shrink: 0; background: var(--bg-subtle); border: 1px solid var(--border); border-radius: var(--radius-sm); color: var(--text-secondary); display: flex; align-items: center; justify-content: center; cursor: pointer; transition: var(--transition); }
.nickname-refresh:hover { color: var(--accent); border-color: var(--accent); }

.auth-footer { text-align: center; margin-top: 20px; font-size: 13px; color: var(--text-muted); }
.link-accent { color: var(--accent); font-weight: 600; margin-left: 4px; }
.link-accent:hover { color: var(--accent-soft); }
</style>
