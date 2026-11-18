import { defineConfig } from '@playwright/test'

// E2E tests click through the app in a browser. They need the API (8080) and the frontend (5173) running.
// Locally: E2E_CHANNEL=msedge (the installed Edge, no download); CI uses Playwright Chromium.
const channel = process.env.E2E_CHANNEL

export default defineConfig({
  testDir: 'e2e',
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    locale: 'lt-LT',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    ...(channel ? { channel } : {}),
  },
})
