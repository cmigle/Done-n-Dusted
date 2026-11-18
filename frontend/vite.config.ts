/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Vystant /api nukreipiamas i lokalia API (8080), todel CORS nereikia.
// Debesyje adresas nurodomas VITE_API_URL kintamuoju.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/swagger-ui': 'http://localhost:8080',
      '/v3/api-docs': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    include: ['src/**/*.test.{ts,tsx}'],
    // Ikonu paketo ESM importai be pletiniu - Vitest ji transformuoja pats, o ne palieka Node.
    server: { deps: { inline: ['@nsmr/pixelart-react'] } },
  },
})
