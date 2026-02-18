<template>
  <div class="countdown" :class="urgency">
    ⏱ {{ displayTime }}
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  deadlineAt: { type: Number, required: true }, // timestamp ms
})

const remaining = ref(0)
let interval = null

function update() {
  remaining.value = Math.max(0, props.deadlineAt - Date.now())
}

onMounted(() => {
  update()
  interval = setInterval(update, 250)
})

onUnmounted(() => clearInterval(interval))

watch(() => props.deadlineAt, () => update())

const displayTime = computed(() => {
  const secs = Math.ceil(remaining.value / 1000)
  return `${secs}s`
})

const urgency = computed(() => {
  const secs = remaining.value / 1000
  if (secs <= 5) return 'urgent'
  if (secs <= 10) return 'warning'
  return 'normal'
})
</script>

<style scoped>
.countdown {
  display: inline-block;
  font-weight: 700;
  font-size: 1.1rem;
  padding: 6px 14px;
  border-radius: 20px;
  background: #0f3460;
  color: #eee;
  transition: background 0.3s, color 0.3s;
}

.countdown.warning {
  background: #e67e22;
  color: white;
}

.countdown.urgent {
  background: #e74c3c;
  color: white;
  animation: pulse 0.5s ease-in-out infinite alternate;
}

@keyframes pulse {
  from { transform: scale(1); }
  to { transform: scale(1.08); }
}
</style>
