// WCAG 2.x contrast ratios for every text/background pair used by the UI, in both themes.
// Usage: npm run contrast [-- --md]   (reads the token values from src/styles/tokens.css)
import { readFileSync } from 'node:fs'

const css = readFileSync(new URL('../src/styles/tokens.css', import.meta.url), 'utf8')

function block(selector) {
  const start = css.indexOf(selector)
  if (start < 0) throw new Error(`no block ${selector}`)
  const open = css.indexOf('{', start)
  let depth = 0
  for (let i = open; i < css.length; i++) {
    if (css[i] === '{') depth++
    if (css[i] === '}' && --depth === 0) return css.slice(open + 1, i)
  }
}
function tokens(text) {
  const map = {}
  for (const [, name, value] of text.matchAll(/--([\w-]+):\s*(#[0-9a-fA-F]{6})\s*;/g)) map[name] = value
  return map
}
const themes = {
  light: tokens(block(':root,')),
  dark: tokens(block(":root[data-theme='dark']")),
}

// The "system is dark" block must repeat [data-theme='dark'] exactly.
const system = tokens(block(":root:not([data-theme='light'])"))
for (const [name, value] of Object.entries(themes.dark)) {
  if (system[name] !== value) throw new Error(`--${name}: ${value} in [data-theme='dark'] but ${system[name]} in the prefers-color-scheme block`)
}

const lin = (c) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)
function luminance(hex) {
  const [r, g, b] = [1, 3, 5].map((i) => lin(parseInt(hex.slice(i, i + 2), 16) / 255))
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}
function ratio(a, b) {
  const [x, y] = [luminance(a), luminance(b)].sort((p, q) => q - p)
  return (x + 0.05) / (y + 0.05)
}

// [foreground, background, where it is used, minimum]
const STATS = ['str', 'dex', 'con', 'int', 'wis', 'cha']
const pairs = [
  ['text', 'bg', 'body text on the page', 4.5],
  ['text', 'surface', 'text in cards, forms, modals', 4.5],
  ['text', 'surface-2', 'footer, quotes, counters, tables', 4.5],
  ['text-muted', 'bg', 'hints and meta on the page', 4.5],
  ['text-muted', 'surface', 'hints and meta in cards', 4.5],
  ['text-muted', 'surface-2', 'meta in the footer and counters', 4.5],
  ['link', 'bg', 'links on the page', 4.5],
  ['link', 'surface', 'links in cards', 4.5],
  ['link-2', 'surface-2', 'footer links', 4.5],
  ['danger-ink', 'surface', 'form errors', 4.5],
  ['danger-ink', 'bg', 'errors on the page, 404', 4.5],
  ['on-primary', 'primary', 'main CTA button', 4.5],
  ['on-primary', 'xp', 'XP chips, level badge', 4.5],
  ['on-primary', 'success', 'Approve / Accept buttons', 4.5],
  ['on-danger', 'danger-btn', 'Delete / Reject buttons', 4.5],
  ['text', 'surface-2', 'secondary buttons', 4.5],
  ...STATS.map((s) => ['on-primary', `stat-${s}`, `${s.toUpperCase()} icon tile`, 3]),
  ['focus', 'bg', 'focus outline on the page (non-text)', 3],
  ['focus', 'surface', 'focus outline in cards (non-text)', 3],
  ['control', 'surface', 'form control borders in cards (non-text)', 3],
  ['control', 'bg', 'form control borders on the page (non-text)', 3],
]

const md = process.argv.includes('--md')
if (md) {
  console.log('| Pair | Used for | Light | Dark | Min |')
  console.log('| --- | --- | --- | --- | --- |')
}
let failed = 0
for (const [fg, bg, where, min] of pairs) {
  const cells = Object.entries(themes).map(([name, t]) => {
    if (!t[fg] || !t[bg]) throw new Error(`${name}: missing --${fg} or --${bg}`)
    const r = ratio(t[fg], t[bg])
    if (r < min) failed++
    return { r, ok: r >= min, fg: t[fg], bg: t[bg] }
  })
  if (md) {
    const cell = (c) => `${c.r.toFixed(2)} ${c.ok ? '✓' : '✗'}`
    console.log(`| \`--${fg}\` on \`--${bg}\` | ${where} | ${cell(cells[0])} | ${cell(cells[1])} | ${min} |`)
  } else {
    const cell = (c) => `${c.fg} on ${c.bg} ${c.r.toFixed(2).padStart(5)} ${c.ok ? 'ok ' : 'FAIL'}`
    console.log(`${`--${fg} / --${bg}`.padEnd(30)} ${cell(cells[0])} | ${cell(cells[1])}`)
  }
}
console.log(failed ? `\n${failed} pair(s) below the minimum` : '\nall pairs pass')
process.exitCode = failed ? 1 : 0
