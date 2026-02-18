<template>
  <div class="dice-row">
    <div
      v-for="(value, index) in dice"
      :key="index"
      class="die"
      :class="{
        locked: locked[index],
        clickable: canInteract,
        'roll-anim': rolling,
      }"
      @click="canInteract && toggleLock(index)"
      :title="canInteract ? (locked[index] ? 'Déverrouiller' : 'Verrouiller') : ''"
    >
      <span class="die-face">{{ diceFace(value) }}</span>
      <span v-if="locked[index]" class="lock-badge">🔒</span>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  dice: { type: Array, required: true },       // int[5]
  locked: { type: Array, required: true },      // boolean[5]
  canInteract: { type: Boolean, default: false },
  rolling: { type: Boolean, default: false },
})

const emit = defineEmits(['toggle-lock'])

function toggleLock(index) {
  emit('toggle-lock', index)
}

const FACES = ['', '⚀', '⚁', '⚂', '⚃', '⚄', '⚅']

function diceFace(value) {
  return FACES[value] ?? '？'
}
</script>

<style scoped>
.dice-row {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.die {
  position: relative;
  width: 72px;
  height: 72px;
  background: #f5f5f5;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid transparent;
  transition: transform 0.15s, border-color 0.2s, background 0.2s;
  user-select: none;
}

.die.clickable {
  cursor: pointer;
}

.die.clickable:hover {
  transform: scale(1.08);
  border-color: #888;
}

.die.locked {
  background: #c8e6c9;
  border-color: #27ae60;
}

.die.locked.clickable:hover {
  border-color: #1e8449;
}

.die-face {
  font-size: 3rem;
  line-height: 1;
  color: #111;
}

.lock-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  font-size: 0.9rem;
}

.roll-anim {
  animation: shake 0.3s ease;
}

@keyframes shake {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(-8deg) scale(1.1); }
  75% { transform: rotate(8deg) scale(1.1); }
}
</style>
