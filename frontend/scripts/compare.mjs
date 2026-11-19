// Before/after comparison: landing, rules modal, menu, guild and sheet at 360 / 768 / 1280 px in both themes.
// Usage: node scripts/compare.mjs <outDir> [baseUrl] [light|dark|both]
import { chromium } from '@playwright/test'
import { mkdirSync } from 'node:fs'

const out = process.argv[2] ?? 'compare'
const base = process.argv[3] ?? 'http://localhost:5173'
const themes = (process.argv[4] ?? 'both') === 'both' ? ['light', 'dark'] : [process.argv[4]]
mkdirSync(out, { recursive: true })
const browser = await chromium.launch({ channel: process.env.E2E_CHANNEL ?? 'msedge' })

for (const theme of themes) {
  for (const width of [360, 768, 1280]) {
    // The theme follows the system colour scheme until the visitor picks one with the toggle.
    const context = await browser.newContext({ viewport: { width, height: 800 }, locale: 'en-GB', colorScheme: theme })
    const page = await context.newPage()
    page.on('pageerror', (e) => console.log(`[${theme} ${width}] page error: ${e.message}`))
    const shot = (name, fullPage = true) => page.screenshot({ path: `${out}/${theme}-${name}-${width}.png`, fullPage })
    await page.goto(base)
    await page.waitForTimeout(1000)
    await shot('landing')
    await page.getByRole('button', { name: /How does XP work/ }).click()
    await page.waitForTimeout(400)
    await shot('rules', false)
    await page.keyboard.press('Escape')
    if (width < 768) {
      await page.getByRole('button', { name: 'Open menu' }).click()
      await page.waitForTimeout(400)
      await shot('menu', false)
    }
    await page.goto(`${base}/login`)
    await page.getByLabel('Username').fill('rokas')
    await page.getByLabel('Password', { exact: true }).fill('Demo123!')
    await page.getByRole('button', { name: 'Log in' }).click()
    await page.waitForURL('**/tavern')
    await page.goto(`${base}/guilds/2`)
    await page.getByRole('heading', { name: 'Iron Vow' }).waitFor()
    await page.waitForTimeout(500)
    await shot('guild')
    await page.goto(`${base}/guilds/2/members/4`)
    await page.waitForTimeout(1300)
    await shot('sheet')
    await context.close()
    console.log(`${theme} ${width} done`)
  }
}
await browser.close()
