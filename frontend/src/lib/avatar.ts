/**
 * A procedurally generated pixel character: the name hashes to a seed, the seed to a symmetric 8x8 figure
 * and two colours. The same name always gives the same avatar, so no images need to be stored.
 */

const BODY = ['#b13e53', '#ef7d57', '#ffcd75', '#a7f070', '#38b764', '#41a6f6', '#73eff7', '#94b0c2']
const TRIM = ['#5d275d', '#257179', '#29366f', '#3b5dc9', '#333c57']

function hash(text: string): number {
  // FNV-1a
  let h = 0x811c9dc5
  for (let i = 0; i < text.length; i++) {
    h ^= text.charCodeAt(i)
    h = Math.imul(h, 0x01000193)
  }
  return h >>> 0
}

function random(seed: number) {
  // mulberry32
  let a = seed
  return () => {
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

export interface Sprite {
  body: string
  trim: string
  /** [row, column, 1 = body | 2 = trim] */
  cells: [number, number, 1 | 2][]
}

export function sprite(name: string): Sprite {
  const next = random(hash(name.toLowerCase()))
  const body = BODY[Math.floor(next() * BODY.length)]
  const trim = TRIM[Math.floor(next() * TRIM.length)]
  // One value per cell: a later one (e.g. the eyes) overwrites an earlier one.
  const grid = new Map<string, [number, number, 1 | 2]>()
  const put = (row: number, col: number, kind: 1 | 2) => grid.set(`${row}-${col}`, [row, col, kind])
  for (let row = 1; row < 7; row++) {
    for (let col = 1; col < 4; col++) {
      // Denser in the middle, so the figure looks like a character rather than noise.
      const chance = col === 3 ? 0.75 : 0.5
      const roll = next()
      if (roll < chance) {
        const kind: 1 | 2 = roll < chance * 0.2 ? 2 : 1
        put(row, col, kind)
        put(row, 7 - col, kind)
      }
    }
  }
  // Eyes, so there is always a "face".
  put(2, 2, 2)
  put(2, 5, 2)
  return { body, trim, cells: [...grid.values()] }
}
