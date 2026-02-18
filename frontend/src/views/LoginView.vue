<template>
  <div class="auth-wrapper">
    <div class="card auth-card">
      <h1>🎲 Yam</h1>
      <p class="subtitle">Le Yahtzee en ligne</p>

      <div class="tabs">
        <button :class="{ active: mode === 'login' }" @click="mode = 'login'">Connexion</button>
        <button :class="{ active: mode === 'register' }" @click="mode = 'register'">Inscription</button>
      </div>

      <!-- LOGIN -->
      <form v-if="mode === 'login'" @submit.prevent="handleLogin">
        <div class="field">
          <label>Pseudo</label>
          <input v-model="pseudo" type="text" placeholder="alice" required />
        </div>
        <div class="field">
          <label>Mot de passe</label>
          <input v-model="password" type="password" placeholder="••••••••" required />
        </div>
        <p v-if="error" class="error-msg">{{ error }}</p>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? 'Connexion…' : 'Se connecter' }}
        </button>
      </form>

      <!-- REGISTER -->
      <form v-else @submit.prevent="handleRegister">
        <div class="field">
          <label>Pseudo</label>
          <input v-model="pseudo" type="text" placeholder="Choisissez un pseudo" required />
        </div>
        <div class="field">
          <label>Mot de passe</label>
          <input v-model="password" type="password" placeholder="••••••••" required />
        </div>
        <p v-if="error" class="error-msg">{{ error }}</p>
        <p v-if="success" class="success-msg">{{ success }}</p>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? 'Inscription…' : "S'inscrire" }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import apiClient from '../api/client.js'

const router = useRouter()
const auth = useAuthStore()

const mode = ref('login')
const pseudo = ref('')
const password = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    const { data } = await apiClient.post('/auth/login', {
      pseudo: pseudo.value,
      password: password.value,
    })
    auth.login(data.token, pseudo.value)
    router.push({ name: 'lobby' })
  } catch (e) {
    error.value = e.response?.data?.message || 'Identifiants incorrects'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  error.value = ''
  success.value = ''
  loading.value = true
  try {
    await apiClient.post('/auth/register', {
      pseudo: pseudo.value,
      password: password.value,
    })
    success.value = 'Compte créé ! Vous pouvez maintenant vous connecter.'
    mode.value = 'login'
  } catch (e) {
    error.value = e.response?.data?.message || 'Erreur lors de l\'inscription'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  text-align: center;
}

h1 {
  font-size: 2.5rem;
  margin-bottom: 4px;
}

.subtitle {
  color: #999;
  margin-bottom: 32px;
  font-size: 1rem;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 28px;
}

.tabs button {
  flex: 1;
  background: #0f3460;
  color: #aaa;
  padding: 10px;
}

.tabs button.active {
  background: #e94560;
  color: white;
}

.field {
  text-align: left;
  margin-bottom: 16px;
}

.field label {
  display: block;
  font-size: 0.85rem;
  color: #aaa;
  margin-bottom: 6px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

form button[type='submit'] {
  width: 100%;
  margin-top: 8px;
  padding: 14px;
  font-size: 1rem;
}

.success-msg {
  color: #27ae60;
  font-size: 0.9rem;
  margin-top: 8px;
}
</style>
