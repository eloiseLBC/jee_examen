<template>
  <div class="game-page">
    <!-- Chargement initial -->
    <div v-if="loading" class="center-screen">
      <p>Chargement de la partie…</p>
    </div>

    <!-- Erreur -->
    <div v-else-if="fatalError" class="center-screen">
      <p class="error-msg">{{ fatalError }}</p>
      <router-link to="/lobby">← Retour au lobby</router-link>
    </div>

    <template v-else>
      <!-- Bannière de fin de partie -->
      <div v-if="isFinished" class="game-over-banner" :class="didIWin ? 'win' : 'lose'">
        <span v-if="didIWin">🎉 Vous avez gagné !</span>
        <span v-else>😞 Vous avez perdu…</span>
        <router-link to="/lobby" class="back-btn btn-secondary">Retour au lobby</router-link>
        <router-link to="/halloffame" class="back-btn btn-secondary">🏆 Hall of Fame</router-link>
      </div>

      <!-- Header -->
      <div class="game-header card">
        <div class="game-info">
          <span class="game-id">Partie #{{ gameId }}</span>
          <span class="status-badge" :class="game.status?.toLowerCase()">{{ statusLabel }}</span>
        </div>
        <div class="turn-info" v-if="!isFinished">
          <span>Tour de <strong>{{ currentPlayerPseudo }}</strong></span>
          <CountdownTimer v-if="game.turnDeadlineAt" :deadlineAt="game.turnDeadlineAt" />
        </div>
      </div>

      <!-- Zone de jeu : dés + actions -->
      <div v-if="isMyTurn && !isFinished" class="game-actions card">
        <h2>🎲 Vos dés</h2>
        <p class="rolls-info">
          Lancer {{ game.rollCount ?? 0 }}/3 —
          <span v-if="rollsLeft > 0">{{ rollsLeft }} lancer(s) restant(s)</span>
          <span v-else class="must-score">Vous devez scorer !</span>
        </p>

        <DiceDisplay
          :dice="localDice"
          :locked="localLocked"
          :can-interact="canLock"
          :rolling="rolling"
          @toggle-lock="toggleLock"
        />

        <p v-if="canLock" class="lock-hint">
          💡 Cliquez sur un dé pour le verrouiller/déverrouiller, puis relancez.
        </p>

        <div class="action-buttons">
          <button
            v-if="rollsLeft > 0 && game.rollCount === 0"
            class="btn-primary"
            @click="handleRoll"
            :disabled="actionLoading"
          >
            🎲 Lancer les dés
          </button>
          <button
            v-if="rollsLeft > 0 && game.rollCount > 0"
            class="btn-primary"
            @click="handleLockAndRoll"
            :disabled="actionLoading"
          >
            🎲 Relancer
          </button>
          <p v-if="actionError" class="error-msg">{{ actionError }}</p>
        </div>

        <p v-if="rollsLeft === 0 || game.rollCount > 0" class="score-hint">
          👆 Cliquez sur une case de votre feuille de score pour enregistrer votre score.
        </p>
      </div>

      <!-- Zone des dés en attente (tour de l'adversaire) -->
      <div v-else-if="!isFinished" class="game-waiting card">
        <p class="waiting-text">⏳ En attente du tour de <strong>{{ currentPlayerPseudo }}</strong>…</p>
        <DiceDisplay
          :dice="game.dice ?? [0,0,0,0,0]"
          :locked="game.locked ?? [false,false,false,false,false]"
          :can-interact="false"
        />
      </div>

      <!-- Feuilles de score -->
      <div class="score-section">
        <ScoreSheet
          v-for="sheet in game.scores"
          :key="sheet.playerId"
          :sheet="sheet"
          :is-active="sheet.playerId === game.currentPlayerId"
          :is-me="sheet.pseudo === auth.pseudo"
          :can-score="isMyTurn && !isFinished && (game.rollCount ?? 0) > 0"
          :possible-scores="sheet.pseudo === auth.pseudo ? possibleScores : {}"
          @score="handleScore"
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import apiClient from '../api/client.js'
import DiceDisplay from '../components/DiceDisplay.vue'
import ScoreSheet from '../components/ScoreSheet.vue'
import CountdownTimer from '../components/CountdownTimer.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const gameId = route.params.id
const game = ref({})
const loading = ref(true)
const fatalError = ref('')
const actionLoading = ref(false)
const actionError = ref('')
const rolling = ref(false)
const possibleScores = ref({})

// Dés et locks locaux (mis à jour après chaque roll)
const localDice = ref([0, 0, 0, 0, 0])
const localLocked = ref([false, false, false, false, false])

let pollInterval = null

// ── Computed ────────────────────────────────────────────────────────────────

const isFinished = computed(() => game.value.status === 'TERMINE' || game.value.status === 'ABANDON')

const isMyTurn = computed(() => {
  const mySheet = game.value.scores?.find(s => s.pseudo === auth.pseudo)
  return mySheet && game.value.currentPlayerId === mySheet.playerId
})

const rollsLeft = computed(() => Math.max(0, 3 - (game.value.rollCount ?? 0)))
const canLock = computed(() => isMyTurn.value && (game.value.rollCount ?? 0) > 0 && rollsLeft.value > 0)

const currentPlayerPseudo = computed(() => {
  const sheet = game.value.scores?.find(s => s.playerId === game.value.currentPlayerId)
  return sheet?.pseudo ?? '…'
})

const didIWin = computed(() => {
  if (!game.value.winnerId) return false
  const mySheet = game.value.scores?.find(s => s.pseudo === auth.pseudo)
  return mySheet?.playerId === game.value.winnerId
})

const statusLabel = computed(() => {
  const map = { EN_COURS: 'En cours', TERMINE: 'Terminée', ABANDON: 'Abandonnée' }
  return map[game.value.status] ?? game.value.status
})

// ── Lifecycle ────────────────────────────────────────────────────────────────

onMounted(async () => {
  await fetchGame()
  loading.value = false
  if (!isFinished.value) {
    pollInterval = setInterval(fetchGame, 3000)
  }
})

onUnmounted(() => clearInterval(pollInterval))

watch(isFinished, (finished) => {
  if (finished) clearInterval(pollInterval)
})

// ── Methods ──────────────────────────────────────────────────────────────────

async function fetchGame() {
  try {
    const { data } = await apiClient.get(`/games/${gameId}`)
    game.value = data
    // Sync dés locaux depuis l'état du serveur si ce n'est pas notre tour actif
    if (!isMyTurn.value || (game.value.rollCount ?? 0) === 0) {
      localDice.value = data.dice ? [...data.dice] : [0, 0, 0, 0, 0]
      localLocked.value = data.locked ? [...data.locked] : [false, false, false, false, false]
    }
  } catch (e) {
    if (loading.value) {
      fatalError.value = e.response?.data?.message || 'Impossible de charger la partie'
    }
  }
}

function toggleLock(index) {
  const next = [...localLocked.value]
  next[index] = !next[index]
  localLocked.value = next
}

async function handleRoll() {
  actionError.value = ''
  actionLoading.value = true
  rolling.value = true
  try {
    const { data } = await apiClient.post(`/games/${gameId}/roll`)
    applyRollResponse(data)
  } catch (e) {
    actionError.value = e.response?.data?.message || 'Erreur lors du lancer'
  } finally {
    actionLoading.value = false
    setTimeout(() => { rolling.value = false }, 350)
  }
}

async function handleLockAndRoll() {
  actionError.value = ''
  actionLoading.value = true
  rolling.value = true
  const lockedIndexes = localLocked.value
    .map((v, i) => v ? i : null)
    .filter(i => i !== null)
  try {
    const { data } = await apiClient.post(`/games/${gameId}/lock`, { lockedIndexes })
    applyRollResponse(data)
  } catch (e) {
    actionError.value = e.response?.data?.message || 'Erreur lors du lancer'
  } finally {
    actionLoading.value = false
    setTimeout(() => { rolling.value = false }, 350)
  }
}

async function handleScore(category) {
  actionError.value = ''
  actionLoading.value = true
  try {
    const { data } = await apiClient.post(`/games/${gameId}/score`, { category })
    game.value = data
    localDice.value = data.dice ? [...data.dice] : [0, 0, 0, 0, 0]
    localLocked.value = [false, false, false, false, false]
    possibleScores.value = {}
  } catch (e) {
    actionError.value = e.response?.data?.message || 'Erreur lors de l\'enregistrement du score'
  } finally {
    actionLoading.value = false
  }
}

function applyRollResponse(data) {
  // RollResponse contient dice, locked, rollCount, rollsLeft, turnDeadlineAt, possibleScores, scores
  localDice.value = [...data.dice]
  localLocked.value = [...data.locked]
  possibleScores.value = data.possibleScores ?? {}
  // Mise à jour partielle de game
  game.value = {
    ...game.value,
    dice: data.dice,
    locked: data.locked,
    rollCount: data.rollCount,
    turnDeadlineAt: data.turnDeadlineAt,
    scores: data.scores,
  }
}
</script>

<style scoped>
.game-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.center-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  gap: 16px;
  color: #aaa;
  font-size: 1.1rem;
}

/* Bannière de fin */
.game-over-banner {
  padding: 20px 28px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 1.3rem;
  font-weight: 700;
  flex-wrap: wrap;
}

.game-over-banner.win {
  background: linear-gradient(135deg, #1e8449, #27ae60);
  color: white;
}

.game-over-banner.lose {
  background: linear-gradient(135deg, #922b21, #e74c3c);
  color: white;
}

.back-btn {
  text-decoration: none;
  font-size: 0.9rem;
  padding: 8px 16px;
}

/* Header */
.game-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px 24px;
}

.game-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.game-id {
  font-weight: 700;
  font-size: 1rem;
  color: #aaa;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
}

.status-badge.en_cours { background: #1a6b3a; color: #7dcea0; }
.status-badge.termine { background: #1a3a6b; color: #7daeea; }
.status-badge.abandon { background: #6b1a1a; color: #ea7d7d; }

.turn-info {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 0.95rem;
}

/* Zone de jeu */
.game-actions, .game-waiting {
  padding: 28px;
  text-align: center;
}

.game-actions h2 {
  font-size: 1.4rem;
  margin-bottom: 8px;
}

.rolls-info {
  color: #aaa;
  margin-bottom: 20px;
}

.must-score {
  color: #e94560;
  font-weight: 700;
}

.lock-hint {
  color: #888;
  font-size: 0.85rem;
  margin-top: 12px;
  margin-bottom: 4px;
}

.score-hint {
  margin-top: 16px;
  color: #f39c12;
  font-size: 0.9rem;
  font-weight: 600;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.action-buttons button {
  padding: 14px 36px;
  font-size: 1.1rem;
}

.waiting-text {
  color: #aaa;
  font-size: 1.1rem;
  margin-bottom: 20px;
}

/* Feuilles de score */
.score-section {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  justify-content: center;
}

.score-section > * {
  flex: 1;
  min-width: 260px;
  max-width: 480px;
}
</style>
