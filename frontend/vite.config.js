import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 本机运行默认走 8080；容器运行可通过 VITE_API_TARGET 指向映射端口。
const apiTarget = process.env.VITE_API_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true
      }
    }
  }
})
