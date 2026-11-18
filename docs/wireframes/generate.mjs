// LD3 wireframe'ai (SVG). Paleisti: node docs/wireframes/generate.mjs
// Pilki staciakampiai be spalvu ir sriftu stiliaus - tik isdestymas, kaip ir dera wireframe'ui.
import { writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const dir = dirname(fileURLToPath(import.meta.url))
const INK = '#3a3f4b'
const LINE = '#9aa3b2'
const FILL = '#eef0f4'
const DARK = '#d5d9e1'

class Frame {
  constructor(w, h, title) {
    this.w = w
    this.h = h
    this.title = title
    this.parts = []
  }
  box(x, y, w, h, { fill = FILL, stroke = LINE, dash = false, r = 0 } = {}) {
    this.parts.push(
      `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${r}" fill="${fill}" stroke="${stroke}" stroke-width="2"${dash ? ' stroke-dasharray="8 6"' : ''}/>`,
    )
    return this
  }
  text(x, y, t, { size = 16, weight = 400, anchor = 'start', color = INK } = {}) {
    const safe = t.replace(/&/g, '&amp;').replace(/</g, '&lt;')
    this.parts.push(
      `<text x="${x}" y="${y}" font-family="Arial, sans-serif" font-size="${size}" font-weight="${weight}" fill="${color}" text-anchor="${anchor}">${safe}</text>`,
    )
    return this
  }
  lines(x, y, w, count, gap = 18) {
    for (let i = 0; i < count; i++) {
      const lw = i === count - 1 ? w * 0.6 : w
      this.parts.push(`<rect x="${x}" y="${y + i * gap}" width="${lw}" height="8" fill="${DARK}"/>`)
    }
    return this
  }
  image(x, y, w, h, label = 'Paveikslas') {
    this.box(x, y, w, h, { fill: '#fff' })
    this.parts.push(
      `<line x1="${x}" y1="${y}" x2="${x + w}" y2="${y + h}" stroke="${LINE}" stroke-width="2"/><line x1="${x + w}" y1="${y}" x2="${x}" y2="${y + h}" stroke="${LINE}" stroke-width="2"/>`,
    )
    return this.text(x + w / 2, y + h / 2 + 6, label, { anchor: 'middle', size: 14 })
  }
  button(x, y, w, label, primary = true) {
    this.box(x, y, w, 40, { fill: primary ? DARK : '#fff', stroke: INK, r: 4 })
    return this.text(x + w / 2, y + 26, label, { anchor: 'middle', size: 15, weight: 700 })
  }
  input(x, y, w, label, { h = 40, kind = '' } = {}) {
    this.text(x, y - 8, label, { size: 14, weight: 700 })
    this.box(x, y, w, h, { fill: '#fff' })
    if (kind) this.text(x + w - 12, y + 25, kind, { anchor: 'end', size: 13, color: LINE })
    return this
  }
  avatar(x, y, s) {
    this.box(x, y, s, s, { fill: DARK })
    return this.text(x + s / 2, y + s / 2 + 5, '8x8', { anchor: 'middle', size: 12 })
  }
  bar(x, y, w, fraction) {
    this.box(x, y, w, 16, { fill: '#fff' })
    return this.box(x, y, w * fraction, 16, { fill: DARK, stroke: 'none' })
  }
  note(x, y, t) {
    return this.text(x, y, t, { size: 13, color: '#b13e53' })
  }
  header(nav, user = false) {
    this.box(0, 0, this.w, 72, { fill: DARK, stroke: INK })
    this.avatar(24, 16, 40)
    this.text(76, 43, 'Done n Dusted', { size: 20, weight: 700 })
    let x = this.w - 24 - (user ? 60 : 0)
    for (const item of [...nav].reverse()) {
      const w = item.length * 9 + 48
      x -= w + 8
      this.box(x, 18, w, 36, { fill: '#fff', stroke: INK })
      this.text(x + w / 2, 42, item, { anchor: 'middle', size: 14 })
    }
    if (user) this.avatar(this.w - 72, 18, 36)
    return this
  }
  mobileHeader() {
    this.box(0, 0, this.w, 64, { fill: DARK, stroke: INK })
    this.avatar(16, 14, 36)
    this.text(62, 39, 'Done n Dusted', { size: 17, weight: 700 })
    this.box(this.w - 56, 12, 40, 40, { fill: '#fff', stroke: INK })
    for (let i = 0; i < 3; i++) this.box(this.w - 46, 22 + i * 9, 20, 3, { fill: INK, stroke: 'none' })
    return this
  }
  footer(cols = 3) {
    const y = this.h - 120
    this.box(0, y, this.w, 120, { fill: '#e3efe6', stroke: INK })
    const cw = (this.w - 48) / cols
    for (let i = 0; i < cols; i++) {
      this.text(24 + i * cw, y + 32, ['Apie', 'Projektas', 'Kursas'][i] ?? '', { size: 14, weight: 700 })
      this.lines(24 + i * cw, y + 48, Math.min(cw - 32, 220), 2, 16)
    }
    return this
  }
  save(file) {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${this.w} ${this.h + 40}" width="${this.w}" height="${this.h + 40}" role="img" aria-label="${this.title}"><title>${this.title}</title><rect width="100%" height="100%" fill="#fff"/><g transform="translate(0 40)">${this.parts.join('')}</g><text x="16" y="26" font-family="Arial, sans-serif" font-size="18" font-weight="700" fill="${INK}">${this.title}</text></svg>\n`
    writeFileSync(join(dir, file), svg)
    console.log(file)
  }
}

const W = 1280
const NAV = ['Taverna', 'Draugai', 'Administravimas', 'Atsijungti']

// 1. Landing
{
  const f = new Frame(W, 900, '1. Pradžia (svečias) – 1280 px')
  f.header(['Prisijungti', 'Registruotis'])
  f.box(104, 112, 420, 52, { fill: DARK }).text(116, 146, 'H1: Tavo veikėjas auga tarp sesijų', { size: 18, weight: 700 })
  f.lines(104, 190, 400, 4)
  f.button(104, 280, 180, 'Sukurti veikėją').button(300, 280, 180, 'Jau turiu paskyrą', false)
  f.box(700, 112, 476, 400).avatar(724, 136, 64).text(804, 160, 'Pavyzdinis veikėjas', { weight: 700 }).box(1096, 136, 56, 56, { fill: DARK }).text(1124, 170, 'Lv', { anchor: 'middle', weight: 700 })
  f.bar(724, 220, 428, 0.65)
  for (let i = 0; i < 6; i++) f.text(724, 270 + i * 30, ['Jėga', 'Vikrumas', 'Ištvermė', 'Intelektas', 'Išmintis', 'Charizma'][i], { size: 14 }).bar(900, 258 + i * 30, 252, [0.3, 0.1, 0.5, 0.2, 0.6, 0.1][i])
  f.box(724, 446, 428, 44, { fill: '#fff' }).text(740, 474, 'Žurnalo įrašo pavyzdys: +20 XP', { size: 14 })
  f.text(104, 476, 'H2: Kaip tai veikia', { size: 18, weight: 700 })
  f.button(916, 450, 260, 'Kaip skaičiuojama patirtis?', false)
  for (let i = 0; i < 3; i++) {
    const x = 104 + i * 366
    f.box(x, 500, 340, 200).text(x + 20, 536, `${i + 1}. žingsnis + ikona`, { weight: 700 }).lines(x + 20, 560, 290, 3)
  }
  f.note(104, 730, 'Mygtukas „Kaip skaičiuojama patirtis?“ atidaro modalinį langą su XP lentele')
  f.footer()
  f.save('01-landing.svg')
}

// 2. Taverna
{
  const f = new Frame(W, 760, '2. Taverna (prisijungus) – 1280 px')
  f.header(NAV, true)
  f.text(104, 132, 'H1: Taverna', { size: 26, weight: 700 }).lines(104, 150, 380, 1)
  f.box(104, 190, 344, 150).avatar(128, 214, 96).text(244, 240, 'vardas', { weight: 700 }).lines(244, 256, 170, 2)
  f.box(104, 364, 344, 110).text(124, 396, 'Pakvietimai (jei yra)', { weight: 700 }).button(124, 414, 140, 'Peržiūrėti')
  f.text(480, 214, 'H2: Tavo grupės', { size: 20, weight: 700 }).button(996, 186, 180, '+ Įkurti grupę')
  for (let i = 0; i < 4; i++) {
    const x = 480 + (i % 2) * 356
    const y = 236 + Math.floor(i / 2) * 170
    f.box(x, y, 340, 150).text(x + 20, y + 36, 'Grupės pavadinimas', { weight: 700 }).lines(x + 20, y + 54, 290, 2).box(x + 20, y + 104, 130, 26, { fill: DARK })
  }
  f.note(480, 600, 'Kortelė – nuoroda į grupę; ženklelis rodo tavo rolę (DM / žaidėjas)')
  f.footer()
  f.save('02-tavern.svg')
}

// 3. Grupe
{
  const f = new Frame(W, 900, '3. Grupė: užduotys – 1280 px')
  f.header(NAV, true)
  f.text(104, 110, 'Taverna / Grupė', { size: 13 })
  f.text(104, 150, 'H1: Grupės pavadinimas', { size: 26, weight: 700 }).lines(104, 168, 420, 1)
  f.box(104, 190, 130, 26, { fill: DARK }).text(250, 208, 'Savininkas: …', { size: 14 })
  f.button(906, 150, 130, 'Redaguoti', false).button(1046, 150, 130, 'Ištrinti', false)
  f.box(104, 240, 1072, 40, { fill: '#fff' }).text(124, 266, 'Užduotys   |   Nariai   |   Peržiūra (tik DM)', { size: 15, weight: 700 })
  f.box(104, 300, 1072, 96)
  f.input(124, 336, 300, 'Charakteristika', { kind: 'select' }).input(444, 336, 300, 'Tipas', { kind: 'select' }).input(764, 336, 260, 'Sunkumas', { kind: 'select' })
  f.box(1044, 346, 20, 20, { fill: '#fff' }).text(1072, 362, 'Tik aktyvios', { size: 14 })
  f.text(104, 432, 'Rasta užduočių: N', { size: 14 }).button(996, 410, 180, '+ Nauja užduotis')
  for (let i = 0; i < 6; i++) {
    const x = 104 + (i % 3) * 366
    const y = 470 + Math.floor(i / 3) * 150
    f.box(x, y, 340, 130).box(x + 16, y + 18, 28, 28, { fill: DARK }).text(x + 56, y + 38, 'Užduoties pavadinimas', { weight: 700 })
    f.box(x + 16, y + 84, 70, 24, { fill: DARK }).box(x + 96, y + 84, 60, 24, { fill: DARK }).lines(x + 168, y + 92, 140, 1)
  }
  f.note(104, 790, '„Nauja užduotis“ – modalinis langas: text, textarea, select, radio (sunkumas), number (tikslas), checkbox')
  f.footer()
  f.save('03-guild.svg')
}

// 4. Uzduotis
{
  const f = new Frame(W, 980, '4. Užduotis ir progreso įrašai – 1280 px')
  f.header(NAV, true)
  f.box(104, 110, 344, 380).box(128, 134, 48, 48, { fill: DARK }).text(128, 222, 'H1: Užduotis', { size: 22, weight: 700 }).lines(128, 240, 290, 3)
  for (let i = 0; i < 6; i++) f.text(128 + (i % 2) * 150, 330 + Math.floor(i / 2) * 44, 'Faktas', { size: 13 }).lines(128 + (i % 2) * 150, 340 + Math.floor(i / 2) * 44, 100, 1)
  f.button(128, 440, 130, 'Redaguoti', false).button(270, 440, 130, 'Ištrinti', false)
  f.box(104, 514, 344, 340).text(128, 548, 'Užregistruoti žygį', { weight: 700 })
  f.input(128, 586, 296, 'Ką padarei?', { h: 70, kind: 'textarea' }).input(128, 690, 296, 'Įrodymo nuoroda', { kind: 'url' })
  f.input(128, 766, 296, 'Kada?', { kind: 'datetime-local' }).button(284, 816, 140, 'Užregistruoti')
  f.box(480, 110, 696, 744).text(504, 146, 'Progreso įrašai', { size: 18, weight: 700 }).input(976, 136, 176, 'Būsena', { kind: 'select' })
  for (let i = 0; i < 4; i++) {
    const y = 200 + i * 150
    f.avatar(504, y, 44).text(562, y + 18, 'autorius', { weight: 700 }).box(650, y + 4, 90, 20, { fill: DARK }).lines(562, y + 34, 420, 2).button(1006, y, 100, 'Peržiūrėti').box(1116, y, 36, 40, { fill: '#fff' })
  }
  f.footer()
  f.save('04-quest.svg')
}

// 5. Veikejo kortele
{
  const f = new Frame(W, 900, '5. Veikėjo kortelė – 1280 px')
  f.header(NAV, true)
  f.box(260, 110, 760, 640)
  f.avatar(288, 138, 112).text(424, 168, 'vardas', { size: 22, weight: 700 }).lines(424, 184, 240, 1)
  f.box(424, 210, 60, 60, { fill: DARK }).text(454, 246, 'Lv', { anchor: 'middle', weight: 700 }).bar(500, 222, 490, 0.3)
  f.text(500, 262, 'XP iki kito lygio, iš viso XP', { size: 13 })
  for (let i = 0; i < 3; i++) f.box(288 + i * 200, 290, 180, 56).text(304 + i * 200, 324, ['įkvėpimas', 'serija', 'laukia DM'][i], { size: 14 })
  f.text(288, 386, 'Charakteristikos', { size: 18, weight: 700 })
  const stats = ['Jėga', 'Vikrumas', 'Ištvermė', 'Intelektas', 'Išmintis', 'Charizma']
  stats.forEach((s, i) => {
    const y = 404 + i * 40
    f.box(288, y, 24, 24, { fill: DARK }).text(324, y + 18, s).text(470, y + 18, '1x', { weight: 700 }).bar(500, y + 4, 490, [0.1, 0, 0.4, 0, 0.2, 0][i])
  })
  f.text(288, 676, 'Paskutiniai patvirtinti žygiai (tik pilname vaizde)', { size: 16, weight: 700 }).lines(288, 696, 700, 2)
  f.note(260, 780, 'Juostos užsipildo atsidarius kortelei (pikseliniai žingsniai); viešame vaizde įrašų nėra')
  f.footer()
  f.save('05-sheet.svg')
}

// 6. Perziuros modalinis langas
{
  const f = new Frame(W, 720, '6. DM peržiūros modalinis langas')
  f.box(0, 0, W, 720, { fill: '#c9ced8', stroke: 'none' })
  f.box(360, 60, 560, 600, { fill: '#fff', stroke: INK })
  f.text(384, 100, 'Peržiūrėti įrašą', { size: 20, weight: 700 }).box(872, 76, 32, 32, { fill: '#fff', stroke: INK }).text(888, 98, 'X', { anchor: 'middle' })
  f.lines(384, 126, 500, 2).box(384, 170, 512, 60, { fill: FILL })
  f.text(384, 262, 'Sprendimas', { size: 14, weight: 700 }).text(384, 290, '(•) Patvirtinti   ( ) Atmesti', { size: 15 })
  f.input(384, 340, 240, 'Patirtis: 50 XP', { kind: 'range' }).input(656, 340, 240, 'Įkvėpimas', { kind: 'number' })
  f.input(384, 430, 512, 'Pastaba žaidėjui', { h: 90, kind: 'textarea' })
  f.button(556, 590, 120, 'Atšaukti', false).button(686, 590, 210, 'Patvirtinti ir skirti XP')
  f.note(360, 700, 'Natyvus <dialog>: Esc ir paspaudimas šalia uždaro, fokusas lieka lange')
  f.save('06-review-modal.svg')
}

// 7. Telefonas
{
  const f = new Frame(1000, 860, '7. Telefonas (< 768 px): meniu ir grupė')
  const phone = (x, title) => {
    f.box(x, 0, 300, 800, { fill: '#fff', stroke: INK, r: 16 })
    f.text(x + 150, 830, title, { anchor: 'middle', size: 14, weight: 700 })
  }
  phone(0, 'Meniu uždarytas')
  phone(340, 'Hamburger meniu atidarytas')
  phone(680, 'Grupė – vienas stulpelis')
  for (const x of [0, 340, 680]) {
    f.box(x, 0, 300, 60, { fill: DARK, stroke: INK }).avatar(x + 14, 12, 36).text(x + 58, 36, 'Done n Dusted', { size: 15, weight: 700 })
    f.box(x + 248, 10, 40, 40, { fill: '#fff', stroke: INK })
  }
  f.text(16, 100, 'H1', { size: 20, weight: 700 }).lines(16, 116, 260, 3).image(16, 190, 268, 140).button(16, 350, 268, 'Sukurti veikėją')
  for (let i = 0; i < 3; i++) f.box(356, 60 + i * 52, 268, 44, { fill: '#fff', stroke: INK }).text(372, 88 + i * 52, ['Taverna', 'Draugai', 'Atsijungti'][i], { size: 14 })
  f.box(340, 216, 300, 2, { fill: INK, stroke: 'none' })
  f.text(696, 100, 'H1: Grupė', { size: 18, weight: 700 }).box(696, 120, 268, 200).input(712, 156, 236, 'Charakteristika', { kind: 'select' }).input(712, 236, 236, 'Tipas', { kind: 'select' })
  for (let i = 0; i < 3; i++) f.box(696, 344 + i * 120, 268, 104).text(712, 374 + i * 120, 'Užduotis', { weight: 700 }).lines(712, 390 + i * 120, 200, 2)
  f.save('07-mobile.svg')
}
