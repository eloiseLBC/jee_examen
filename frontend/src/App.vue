<template>
  <router-view />
</template>

<style>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: #1a1a2e;
  color: #eee;
  min-height: 100vh;
}

button {
  cursor: pointer;
  border: none;
  border-radius: 8px;
  padding: 10px 20px;
  font-size: 1rem;
  font-weight: 600;
  transition: opacity 0.2s, transform 0.1s;
}

button:hover:not(:disabled) {
  opacity: 0.85;
  transform: translateY(-1px);
}

button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

input {
  border: 2px solid #444;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 1rem;
  background: #16213e;
  color: #eee;
  width: 100%;
  outline: none;
  transition: border-color 0.2s;
}

input:focus {
  border-color: #e94560;
}

.card {
  background: #16213e;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.btn-primary {
  background: #e94560;
  color: white;
}

.btn-secondary {
  background: #0f3460;
  color: white;
}

.btn-success {
  background: #27ae60;
  color: white;
}

.btn-danger {
  background: #c0392b;
  color: white;
}

.error-msg {
  color: #e74c3c;
  font-size: 0.9rem;
  margin-top: 8px;
}
</style>
