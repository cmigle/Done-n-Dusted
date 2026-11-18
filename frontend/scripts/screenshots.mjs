// Ekrano nuotraukos is veikiancios programos (nematoma Edge narsykle).
// Naudojimas: node scripts/screenshots.mjs [baseUrl] [outDir]
import { chromium } from '@playwright/test'
import { mkdirSync } from 'node:fs'

const base = process.argv[2] ?? 'http://localhost:5173'
const out = process.argv[3] ?? 'screenshots'
mkdirSync(out, { recursive: true })

const browser = await chromium.launch({ channel: 'msedge' })

async function session(name, viewport, steps) {
  const context = await browser.newContext({ viewport, deviceScaleFactor: 1, locale: 'lt-LT' })
  const page = await context.newPage()
  page.on('pageerror', (e) => console.log(`[${name}] page error: ${e.message}`))
  page.on('console', (m) => m.type() === 'error' && console.log(`[${name}] console: ${m.text()}`))
  await steps(page)
  await context.close()
}

async function shot(page, file) {
  await page.waitForTimeout(1200) // juostu animacija
  await page.screenshot({ path: `${out}/${file}.png`, fullPage: true })
  console.log(`${out}/${file}.png`)
}

async function login(page, username) {
  await page.goto(`${base}/prisijungti`)
  await page.getByLabel('Vardas').fill(username)
  await page.getByLabel('Slaptažodis', { exact: true }).fill('Demo123!')
  await page.getByRole('button', { name: 'Prisijungti' }).click()
  await page.waitForURL('**/taverna')
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
    await page.goto(`${base}/grupes/2`)
    await page.getByRole('heading', { name: 'Iron Vow' }).waitFor()
    await shot(page, `03-guild-${suffix}`)
    await page.goto(`${base}/grupes/2/uzduotys/4`)
    await page.getByRole('heading', { name: 'Run 50 kilometers' }).waitFor()
    await shot(page, `04-quest-${suffix}`)
    await page.goto(`${base}/grupes/2/nariai/4`)
    await shot(page, `05-sheet-${suffix}`)
    if (suffix === 'mobile') {
      await page.getByRole('button', { name: 'Atidaryti meniu' }).click()
      await shot(page, `06-menu-${suffix}`)
    }
  })
}

await browser.close()
