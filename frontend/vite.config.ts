/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// In development /api is proxied to the local API (8080), so CORS is not needed.
// A separately hosted API can be set with VITE_API_URL.
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
    // The icon package uses extensionless ESM imports, so Vitest transforms it instead of leaving it to Node.
    server: { deps: { inline: ['@nsmr/pixelart-react'] } },
  },
})
