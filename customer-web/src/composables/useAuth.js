export function useAuth() {
  const getToken = () => localStorage.getItem('token')
  const getRole = () => localStorage.getItem('userRole')
  const isAuthenticated = () => !!getToken()

  function login(accessToken, role) {
    localStorage.setItem('token', accessToken)
    localStorage.setItem('userRole', role)
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('userRole')
  }

  return { getToken, getRole, isAuthenticated, login, logout }
}
