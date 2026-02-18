import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import LoginView from '../views/LoginView.vue'
import LobbyView from '../views/LobbyView.vue'
import GameView from '../views/GameView.vue'
import HallOfFameView from '../views/HallOfFameView.vue'

const routes = [
  { path: '/', name: 'login', component: LoginView },
  { path: '/lobby', name: 'lobby', component: LobbyView, meta: { requiresAuth: true } },
  { path: '/game/:id', name: 'game', component: GameView, meta: { requiresAuth: true } },
  { path: '/halloffame', name: 'halloffame', component: HallOfFameView, meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.token) {
    return { name: 'login' }
  }
  if (to.name === 'login' && auth.token) {
    return { name: 'lobby' }
  }
})

export default router
