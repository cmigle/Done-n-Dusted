import { defineConfig } from '@playwright/test'

// E2E testai praspaudzia programa narsykleje. Reikia veikianciu API (8080) ir frontend (5173).
// Lokaliai: E2E_CHANNEL=msedge (idiegta Edge, nieko nesiunciant); CI - Playwright Chromium.
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
