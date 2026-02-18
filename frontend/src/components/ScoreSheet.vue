<template>
  <div class="score-sheet" :class="{ active: isActive }">
    <h3 class="player-name">
      {{ sheet.pseudo }}
      <span v-if="isActive" class="turn-badge">▶ Son tour</span>
      <span v-if="isMe" class="me-badge">Moi</span>
    </h3>

    <table>
      <thead>
        <tr>
          <th>Catégorie</th>
          <th>Score</th>
        </tr>
      </thead>
      <tbody>
        <!-- Section numérique -->
        <tr class="section-header">
          <td colspan="2">Section numérique</td>
        </tr>
        <tr
          v-for="cat in numericCategories"
          :key="cat.key"
          :class="{ clickable: canScore && sheet[cat.key] === null || canScore && sheet[cat.key] === undefined }"
          @click="canScore && sheet[cat.key] == null && $emit('score', cat.category)"
        >
          <td>{{ cat.label }}</td>
          <td class="score-cell">
            <span v-if="sheet[cat.key] != null" class="filled">{{ sheet[cat.key] }}</span>
            <span v-else-if="canScore && possibleScores[cat.category] !== undefined" class="possible">
              {{ possibleScores[cat.category] }}
            </span>
            <span v-else class="empty-cell">—</span>
          </td>
        </tr>

        <!-- Sous-totaux -->
        <tr class="subtotal">
          <td>Sous-total (1–6)</td>
          <td>{{ sheet.totalNumbers ?? 0 }}</td>
        </tr>
        <tr class="subtotal bonus">
          <td>Bonus (≥63 → +35)</td>
          <td>{{ sheet.totalNumbersBonus ?? 0 }}</td>
        </tr>

        <!-- Section combinaisons -->
        <tr class="section-header">
          <td colspan="2">Combinaisons</td>
        </tr>
        <tr
          v-for="cat in comboCategories"
          :key="cat.key"
          :class="{ clickable: canScore && sheet[cat.key] == null }"
          @click="canScore && sheet[cat.key] == null && $emit('score', cat.category)"
        >
          <td>{{ cat.label }}</td>
          <td class="score-cell">
            <span v-if="sheet[cat.key] != null" class="filled">{{ sheet[cat.key] }}</span>
            <span v-else-if="canScore && possibleScores[cat.category] !== undefined" class="possible">
              {{ possibleScores[cat.category] }}
            </span>
            <span v-else class="empty-cell">—</span>
          </td>
        </tr>

        <!-- Total général -->
        <tr class="total-row">
          <td>TOTAL</td>
          <td>{{ sheet.scoreTotal ?? 0 }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
const props = defineProps({
  sheet: { type: Object, required: true },
  isActive: { type: Boolean, default: false },
  isMe: { type: Boolean, default: false },
  canScore: { type: Boolean, default: false },
  possibleScores: { type: Object, default: () => ({}) },
})

defineEmits(['score'])

const numericCategories = [
  { key: 'score1', label: '1 (Aces)', category: 'ONE' },
  { key: 'score2', label: '2 (Twos)', category: 'TWO' },
  { key: 'score3', label: '3 (Threes)', category: 'THREE' },
  { key: 'score4', label: '4 (Fours)', category: 'FOUR' },
  { key: 'score5', label: '5 (Fives)', category: 'FIVE' },
  { key: 'score6', label: '6 (Sixes)', category: 'SIX' },
]

const comboCategories = [
  { key: 'scoreBrelan', label: 'Brelan', category: 'BRELAN' },
  { key: 'scoreCarre', label: 'Carré', category: 'CARRE' },
  { key: 'scoreFull', label: 'Full (25)', category: 'FULL' },
  { key: 'scorePetiteSuite', label: 'Petite Suite (30)', category: 'PETITE_SUITE' },
  { key: 'scoreGrandeSuite', label: 'Grande Suite (40)', category: 'GRANDE_SUITE' },
  { key: 'scoreYam', label: 'Yam ! (50)', category: 'YAM' },
  { key: 'scoreChance', label: 'Chance', category: 'CHANCE' },
]
</script>

<style scoped>
.score-sheet {
  background: #16213e;
  border-radius: 12px;
  padding: 16px;
  border: 2px solid #2a2a4a;
  transition: border-color 0.3s;
  min-width: 220px;
}

.score-sheet.active {
  border-color: #e94560;
}

.player-name {
  text-align: center;
  font-size: 1.1rem;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.turn-badge {
  background: #e94560;
  color: white;
  font-size: 0.7rem;
  padding: 2px 8px;
  border-radius: 20px;
  font-weight: 700;
}

.me-badge {
  background: #0f3460;
  color: #aaa;
  font-size: 0.7rem;
  padding: 2px 8px;
  border-radius: 20px;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

th {
  padding: 6px 8px;
  text-align: left;
  font-size: 0.75rem;
  color: #888;
  text-transform: uppercase;
  border-bottom: 1px solid #2a2a4a;
}

td {
  padding: 7px 8px;
  border-bottom: 1px solid #1a1a3e;
}

tr.clickable {
  cursor: pointer;
}

tr.clickable:hover {
  background: rgba(233, 69, 96, 0.12);
}

.section-header td {
  background: #0f3460;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #88aadd;
  padding: 5px 8px;
}

.score-cell {
  text-align: right;
  font-weight: 600;
}

.filled {
  color: #eee;
}

.possible {
  color: #f39c12;
  font-style: italic;
}

.empty-cell {
  color: #555;
}

.subtotal td {
  color: #aaa;
  font-size: 0.85rem;
}

.bonus td {
  color: #f39c12;
}

.total-row td {
  font-weight: 700;
  font-size: 1.05rem;
  color: #e94560;
  border-top: 2px solid #2a2a4a;
}
</style>
