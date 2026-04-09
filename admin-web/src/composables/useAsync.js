import { ref } from 'vue'

/**
 * Composable for async operations with loading/error state management.
 *
 * Usage:
 *   const { loading, error, run } = useAsync()
 *   await run(async () => { ... })          // auto sets loading + catches error
 *   await run(async () => { ... }, '커스텀 에러 메시지')
 */
export function useAsync() {
  const loading = ref(false)
  const error = ref('')

  async function run(fn, fallbackMsg = '요청에 실패했습니다.') {
    loading.value = true
    error.value = ''
    try {
      const result = await fn()
      return result
    } catch (e) {
      error.value = e.response?.data?.message || fallbackMsg
      throw e
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = ''
  }

  return { loading, error, run, clearError }
}
