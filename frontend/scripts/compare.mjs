// Prieš/po palyginimui: landing, grupė ir kortelė 360 / 768 / 1280 px pločiu.
// Naudojimas: node scripts/compare.mjs <outDir> [baseUrl]
import { chromium } from '@playwright/test'
import { mkdirSync } from 'node:fs'

const out = process.argv[2] ?? 'compare'
const base = process.argv[3] ?? 'http://localhost:5173'
mkdirSync(out, { recursive: true })
const browser = await chromium.launch({ channel: process.env.E2E_CHANNEL ?? 'msedge' })

for (const width of [360, 768, 1280]) {
  const context = await browser.newContext({ viewport: { width, height: 800 }, locale: 'lt-LT' })
  const page = await context.newPage()
  await page.goto(base)
  await page.waitForTimeout(1000)
  await page.screenshot({ path: `${out}/landing-${width}.png`, fullPage: true })
  await page.getByRole('button', { name: /Kaip skaičiuojama patirtis/ }).click()
  await page.waitForTimeout(400)
  await page.screenshot({ path: `${out}/rules-${width}.png` })
  await page.keyboard.press('Escape')
  if (width < 768) {
    await page.getByRole('button', { name: 'Atidaryti meniu' }).click()
    await page.waitForTimeout(400)
    await page.screenshot({ path: `${out}/menu-${width}.png` })
  }
  await page.goto(`${base}/prisijungti`)
  await page.getByLabel('Vardas').fill('rokas')
  await page.getByLabel('Slaptažodis', { exact: true }).fill('Demo123!')
  await page.getByRole('button', { name: 'Prisijungti' }).click()
  await page.waitForURL('**/taverna')
  await page.goto(`${base}/grupes/2`)
  await page.getByRole('heading', { name: 'Iron Vow' }).waitFor()
  await page.waitForTimeout(500)
  await page.screenshot({ path: `${out}/guild-${width}.png`, fullPage: true })
  await page.goto(`${base}/grupes/2/nariai/4`)
  await page.waitForTimeout(1300)
  await page.screenshot({ path: `${out}/sheet-${width}.png`, fullPage: true })
  await context.close()
  console.log(`${width} done`)
}
await browser.close()
