// Generuoja pikselinius SVG (logo, favicon, hero) i public/. Paleisti: node scripts/pixel-art.mjs
// Kiekvienas pikselis - <rect>, shape-rendering="crispEdges": vektorinis, todel ryskus bet kokiu dydziu.
import { writeFileSync } from 'node:fs'

const C = {
  '#': '#1a1c2c', // midnight
  N: '#29366f', // navy
  B: '#3b5dc9', // royal
  S: '#566c86', // slate
  s: '#333c57', // shadow
  T: '#94b0c2', // steel
  F: '#f4f4f4', // frost
  Y: '#ffcd75', // gold
  O: '#ef7d57', // ember
  R: '#b13e53', // ruby
  G: '#38b764', // leaf
  L: '#a7f070', // lime
  E: '#257179', // teal
  A: '#73eff7', // aqua
  P: '#5d275d', // plum
}

function svg(width, height, rects, title) {
  // Gretimus tos pacios spalvos pikselius eiluteje sujungiam - maziau elementu.
  const body = rects
    .map(([x, y, w, h, c]) => `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${c}"/>`)
    .join('')
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" shape-rendering="crispEdges" role="img" aria-label="${title}"><title>${title}</title>${body}</svg>\n`
}

function fromAscii(rows) {
  const rects = []
  rows.forEach((row, y) => {
    let x = 0
    while (x < row.length) {
      const ch = row[x]
      if (ch === '.' || !C[ch]) {
        x++
        continue
      }
      let w = 1
      while (row[x + w] === ch) w++
      rects.push([x, y, w, 1, C[ch]])
      x += w
    }
  })
  return rects
}

// ---------- Logotipas: skydas su varnele ----------
const logo = [
  '................',
  '...##########...',
  '..#OOOOOOOOOO#..',
  '..#OYYYYYYYYO#..',
  '..#OYYYYYYYNO#..',
  '..#OYYYYYYNNO#..',
  '..#OYNYYYNNYO#..',
  '..#OYNNYNNYYO#..',
  '..#OYYNNNYYYO#..',
  '...#OYYNYYYO#...',
  '...#OYYYYYYO#...',
  '....#OYYYYO#....',
  '.....#OYYO#.....',
  '......#OO#......',
  '.......##.......',
  '................',
]
writeFileSync('public/logo.svg', svg(16, 16, fromAscii(logo), 'Done n Dusted logotipas'))
writeFileSync('public/favicon.svg', svg(16, 16, fromAscii(logo), 'Done n Dusted'))

// ---------- Hero: pilis naktyje, lauzas ir veliava ----------
const W = 96
const H = 48
const r = []
const rect = (x, y, w, h, c) => r.push([x, y, w, h, C[c]])

// Dangus juostomis (be gradientu)
rect(0, 0, W, 14, '#')
rect(0, 14, W, 10, 's')
rect(0, 24, W, 8, 'N')
// Zvaigzdes
for (const [x, y] of [[6, 3], [19, 7], [31, 2], [44, 9], [58, 4], [71, 11], [83, 3], [90, 8], [12, 12], [52, 15]]) {
  rect(x, y, 1, 1, x % 3 === 0 ? 'Y' : 'F')
}
// Menulis
rect(76, 4, 6, 1, 'F')
rect(75, 5, 8, 4, 'F')
rect(76, 9, 6, 1, 'F')
rect(79, 5, 3, 3, 'A')
// Kalnai laiptais
const mountain = (x0, peak, base, color) => {
  for (let i = 0; peak + i < base; i++) rect(x0 - i, peak + i, 2 * i + 1, 1, color)
}
mountain(14, 16, 34, 'S')
mountain(34, 20, 34, 's')
mountain(88, 18, 34, 'S')
// Pilis
rect(38, 22, 30, 14, 'T')
for (let x = 38; x < 68; x += 4) rect(x, 20, 2, 2, 'T') // dantys
rect(34, 16, 8, 20, 'T') // kaire bokstas
rect(64, 16, 8, 20, 'T') // desine bokstas
for (let x = 34; x < 42; x += 3) rect(x, 14, 2, 2, 'T')
for (let x = 64; x < 72; x += 3) rect(x, 14, 2, 2, 'T')
rect(36, 20, 2, 3, 'Y') // apsviesti langai
rect(68, 20, 2, 3, 'Y')
rect(46, 26, 2, 3, 'Y')
rect(58, 26, 2, 3, 'Y')
rect(50, 28, 6, 8, '#') // vartai
rect(51, 27, 4, 1, '#')
// Veliava
rect(67, 6, 1, 8, 'F')
rect(68, 6, 6, 4, 'R')
rect(69, 7, 2, 2, 'Y')
// Zeme
rect(0, 34, W, 3, 'G')
rect(0, 37, W, 11, 'E')
for (let x = 0; x < W; x += 6) rect(x + 2, 33, 1, 1, 'L')
// Takas
for (let i = 0; i < 11; i++) rect(49 - i, 37 + i, 8 + 2 * i, 1, 'P')
// Lauzas
rect(20, 40, 8, 2, 'P')
rect(22, 38, 4, 2, 'O')
rect(23, 36, 2, 2, 'Y')
rect(23, 35, 1, 1, 'R')

writeFileSync('public/hero.svg', svg(W, H, r, 'A castle at night with a campfire by the road'))
console.log('public/logo.svg, public/favicon.svg, public/hero.svg')
