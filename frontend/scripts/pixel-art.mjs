// Generates the pixel-art logo and favicon into public/. Run: node scripts/pixel-art.mjs
// Every pixel is a <rect> with shape-rendering="crispEdges": vector, so it stays sharp at any size.
import { writeFileSync } from 'node:fs'

// Palette shared by both themes: ink outline, pink rim, XP-yellow shield
const C = {
  '#': '#4a3a5c', // ink
  O: '#ff8fab', // rim
  Y: '#ffcf5c', // shield
  N: '#4a3a5c', // check mark
}

function svg(width, height, rects, title) {
  const body = rects
    .map(([x, y, w, h, c]) => `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${c}"/>`)
    .join('')
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" shape-rendering="crispEdges" role="img" aria-label="${title}"><title>${title}</title>${body}</svg>\n`
}

function fromAscii(rows) {
  // Neighbouring pixels of the same colour in a row become one rect.
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

// ---------- Logo: a shield with a check mark ----------
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
writeFileSync('public/logo.svg', svg(16, 16, fromAscii(logo), 'Done n Dusted logo'))
writeFileSync('public/favicon.svg', svg(16, 16, fromAscii(logo), 'Done n Dusted'))
console.log('public/logo.svg, public/favicon.svg')
