<template>
  <div class="lobby-wrapper">
    <div class="card lobby-card">
      <div class="header">
        <h1>🎲 Yam</h1>
        <div class="user-info">
          <span>👤 {{ auth.pseudo }}</span>
          <button class="btn-danger btn-sm" @click="handleLogout">Déconnexion</button>
        </div>
      </div>

      <div class="content">
        <h2>Salle d'attente</h2>

        <div v-if="!waiting" class="ready-section">
          <p class="description">
            Prêt à jouer ? Cliquez sur le bouton pour rejoindre la file d'attente.<br />
            Dès qu'un adversaire est disponible, la partie démarre automatiquement !
          </p>
          <button class="btn-success btn-large" @click="handleReady" :disabled="loading">
            {{ loading ? 'Recherche…' : '🎯 Je suis prêt !' }}
          </button>
        </div>

        <div v-else class="waiting-section">
          <div class="spinner">⏳</div>
          <p class="waiting-text">En attente d'un adversaire…</p>
          <p class="countdown">Expiration dans <strong>{{ expiresIn }}s</strong></p>
          <button class="btn-danger" @click="handleCancel">Annuler</button>
        </div>

        <p v-if="error" class="error-msg">{{ error }}</p>
      </div>

      <div class="footer">
        <router-link to="/halloffame" class="hof-link">🏆 Hall of Fame</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import apiClient from '../api/client.js'

const router = useRouter()
const auth = useAuthStore()

const waiting = ref(false)
const loading = ref(false)
const error = ref('')
const expiresIn = ref(60)

let pollInterval = null

async function handleReady() {
  error.value = ''
  loading.value = true
  try {
    const { data } = await apiClient.post('/lobby/ready')
    loading.value = false

    if (data.matched) {
      router.push({ name: 'game', params: { id: data.gameId } })
      return
    }

    waiting.value = true
    expiresIn.value = data.expiresInSec ?? 60
    startPolling()
  } catch (e) {
    loading.value = false
    error.value = e.response?.data?.message || 'Erreur lors de la mise en attente'
  }
}

function startPolling() {
  pollInterval = setInterval(async () => {
    try {
      const { data } = await apiClient.post('/lobby/ready')
      if (data.matched) {
        clearInterval(pollInterval)
        router.push({ name: 'game', params: { id: data.gameId } })
        return
      }
      expiresIn.value = data.expiresInSec ?? 0
      if (expiresIn.value <= 0) {
        waiting.value = false
        clearInterval(pollInterval)
      }
    } catch {
      waiting.value = false
      clearInterval(pollInterval)
    }
  }, 2500)
}

async function handleCancel() {
  clearInterval(pollInterval)
  waiting.value = false
  try {
    await apiClient.delete('/lobby/ready')
  } catch { /* ignore */ }
}

function handleLogout() {
  handleCancel()
  auth.logout()
  router.push({ name: 'login' })
}

onUnmounted(() => clearInterval(pollInterval))
</script>

<style scoped>
.lobby-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
}

.lobby-card {
  width: 100%;
  max-width: 520px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.header h1 {
  font-size: 2rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #aaa;
  font-size: 0.95rem;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 0.8rem;
}

.content {
  text-align: center;
}

h2 {
  font-size: 1.6rem;
  margin-bottom: 20px;
  color: #e94560;
}

.description {
  color: #aaa;
  line-height: 1.6;
  margin-bottom: 28px;
}

.btn-large {
  padding: 16px 40px;
  font-size: 1.2rem;
  border-radius: 12px;
}

.waiting-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.spinner {
  font-size: 3rem;
  animation: spin 2s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.waiting-text {
  font-size: 1.2rem;
  font-weight: 600;
}

.countdown {
  color: #aaa;
}

.footer {
  margin-top: 32px;
  text-align: center;
  border-top: 1px solid #2a2a4a;
  padding-top: 20px;
}

.hof-link {
  color: #f39c12;
  text-decoration: none;
  font-weight: 600;
  font-size: 1rem;
}

.hof-link:hover {
  text-decoration: underline;
}
</style>
