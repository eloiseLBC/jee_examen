import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('yam_token') || null)
  const pseudo = ref(localStorage.getItem('yam_pseudo') || null)

  const isAuthenticated = computed(() => !!token.value)

  function login(newToken, playerPseudo) {
    token.value = newToken
    pseudo.value = playerPseudo
    localStorage.setItem('yam_token', newToken)
    localStorage.setItem('yam_pseudo', playerPseudo)
  }

  function logout() {
    token.value = null
    pseudo.value = null
    localStorage.removeItem('yam_token')
    localStorage.removeItem('yam_pseudo')
  }

  return { token, pseudo, isAuthenticated, login, logout }
})
