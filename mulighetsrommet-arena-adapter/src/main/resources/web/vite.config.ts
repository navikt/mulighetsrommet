import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '^/manager/*': {
        target: 'http://0.0.0.0:8084',
        changeOrigin: true,
      }
    }
  }
})
