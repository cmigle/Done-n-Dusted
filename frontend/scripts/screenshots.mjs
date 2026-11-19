// Screenshots of the running app (headless Edge).
// Usage: node scripts/screenshots.mjs [baseUrl] [outDir]
import { chromium } from '@playwright/test'
import { mkdirSync } from 'node:fs'

const base = process.argv[2] ?? 'http://localhost:5173'
const out = process.argv[3] ?? 'screenshots'
mkdirSync(out, { recursive: true })

const browser = await chromium.launch({ channel: process.env.E2E_CHANNEL ?? 'msedge' })

async function session(name, viewport, steps, colorScheme = 'light') {
  const context = await browser.newContext({ viewport, deviceScaleFactor: 1, locale: 'en-GB', colorScheme })
  const page = await context.newPage()
  page.on('pageerror', (e) => console.log(`[${name}] page error: ${e.message}`))
  page.on('console', (m) => m.type() === 'error' && console.log(`[${name}] console: ${m.text()}`))
  await steps(page)
  await context.close()
}

async function shot(page, file, fullPage = true) {
  await page.waitForTimeout(1200) // let the bar animation finish
  await page.screenshot({ path: `${out}/${file}.png`, fullPage })
  console.log(`${out}/${file}.png`)
}

async function login(page, username) {
  await page.goto(`${base}/login`)
  await page.getByLabel('Username').fill(username)
  await page.getByLabel('Password', { exact: true }).fill('Demo123!')
  await page.getByRole('button', { name: 'Log in' }).click()
  await page.waitForURL('**/tavern')
}

for (const [suffix, viewport] of [
  ['desktop', { width: 1280, height: 800 }],
  ['mobile', { width: 390, height: 844 }],
]) {
  await session(suffix, viewport, async (page) => {
    await page.goto(base)
    await shot(page, `01-landing-${suffix}`)
    await login(page, 'rokas')
    await shot(page, `02-tavern-${suffix}`)
    await page.goto(`${base}/guilds/2`)
    await page.getByRole('heading', { name: 'Iron Vow' }).waitFor()
    await shot(page, `03-guild-${suffix}`)
    await page.goto(`${base}/guilds/2/quests/4`)
    await page.getByRole('heading', { name: 'Run 50 kilometers' }).waitFor()
    await shot(page, `04-quest-${suffix}`)
    await page.goto(`${base}/guilds/2/members/4`)
    await shot(page, `05-sheet-${suffix}`)
    if (suffix === 'mobile') {
      await page.getByRole('button', { name: 'Open menu' }).click()
      await shot(page, `06-menu-${suffix}`, false)
      return
    }
    await page.goto(`${base}/guilds/2?tab=review`)
    await page.getByRole('button', { name: 'Review' }).first().click()
    await page.getByRole('dialog').waitFor()
    await shot(page, `06-review-modal-${suffix}`, false)
  })
}

await session('ieva', { width: 1280, height: 800 }, async (page) => {
  await login(page, 'ieva')
  await page.goto(`${base}/friends`)
  await page.getByRole('heading', { name: 'Friends', level: 1 }).waitFor()
  await shot(page, '07-friends-desktop')
})

await session('admin', { width: 1280, height: 800 }, async (page) => {
  await login(page, 'admin')
  await page.goto(`${base}/admin`)
  await page.getByRole('table').waitFor()
  await shot(page, '08-admin-desktop')
})

// Night theme (the app follows the system colour scheme until the visitor picks one)
for (const [suffix, viewport] of [
  ['desktop', { width: 1280, height: 800 }],
  ['mobile', { width: 390, height: 844 }],
]) {
  await session(`${suffix}-dark`, viewport, async (page) => {
    await page.goto(base)
    await shot(page, `01-landing-${suffix}-dark`)
    if (suffix === 'mobile') return
    await login(page, 'rokas')
    await page.goto(`${base}/guilds/2/members/4`)
    await shot(page, `05-sheet-${suffix}-dark`)
  }, 'dark')
}

await browser.close()
