<template>
  <div class="hof-wrapper">
    <div class="card hof-card">
      <div class="header">
        <router-link to="/lobby" class="back-link">← Retour au lobby</router-link>
        <h1>🏆 Hall of Fame</h1>
        <div></div>
      </div>

      <div v-if="loading" class="center">Chargement…</div>
      <div v-else-if="error" class="error-msg center">{{ error }}</div>
      <div v-else>
        <table v-if="entries.length > 0">
          <thead>
            <tr>
              <th>#</th>
              <th>Joueur</th>
              <th>Score</th>
              <th>Partie</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(entry, index) in entries" :key="index" :class="{ podium: index < 3 }">
              <td class="rank">{{ rankEmoji(index) }}</td>
              <td class="pseudo">{{ entry.pseudo }}</td>
              <td class="score">{{ entry.score }}</td>
              <td class="game-id">#{{ entry.partieId }}</td>
            </tr>
          </tbody>
        </table>
        <p v-else class="center empty">Aucune partie terminée pour l'instant.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import apiClient from '../api/client.js'

const entries = ref([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const { data } = await apiClient.get('/halloffame?limit=10')
    entries.value = data.entries ?? []
  } catch (e) {
    error.value = 'Impossible de charger le classement'
  } finally {
    loading.value = false
  }
})

function rankEmoji(index) {
  if (index === 0) return '🥇'
  if (index === 1) return '🥈'
  if (index === 2) return '🥉'
  return index + 1
}
</script>

<style scoped>
.hof-wrapper {
  display: flex;
  justify-content: center;
  padding: 40px 20px;
  min-height: 100vh;
}

.hof-card {
  width: 100%;
  max-width: 600px;
  height: fit-content;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

h1 {
  font-size: 1.8rem;
}

.back-link {
  color: #aaa;
  text-decoration: none;
  font-size: 0.9rem;
}

.back-link:hover {
  color: #eee;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  text-align: left;
  padding: 10px 14px;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #888;
  border-bottom: 1px solid #2a2a4a;
}

td {
  padding: 14px;
  border-bottom: 1px solid #2a2a4a;
}

tr.podium td {
  background: rgba(233, 69, 96, 0.08);
}

.rank {
  font-size: 1.2rem;
  text-align: center;
  width: 50px;
}

.pseudo {
  font-weight: 600;
  font-size: 1.05rem;
}

.score {
  color: #f39c12;
  font-weight: 700;
  font-size: 1.1rem;
}

.game-id {
  color: #888;
  font-size: 0.85rem;
}

.center {
  text-align: center;
  padding: 32px;
}

.empty {
  color: #888;
}
</style>
