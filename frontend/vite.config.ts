import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Same origin as the app, so the browser needs no CORS; the backend runs on 8080 locally
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
