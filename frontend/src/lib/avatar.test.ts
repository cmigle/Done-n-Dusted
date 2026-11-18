import { describe, expect, it } from 'vitest'
import { sprite } from './avatar'

describe('sprite', () => {
  it('tas pats vardas - tas pats avataras (nepriklauso nuo raidziu dydzio)', () => {
    expect(sprite('Rokas')).toEqual(sprite('rokas'))
  })

  it('skirtingi vardai - skirtingi avatarai', () => {
    expect(sprite('rokas')).not.toEqual(sprite('ieva'))
  })

  it('kiekvienas langelis piesiamas viena karta', () => {
    for (const name of ['rokas', 'ieva', 'jonas', 'admin', 'Aurelija']) {
      const keys = sprite(name).cells.map(([row, col]) => `${row}-${col}`)
      expect(new Set(keys).size).toBe(keys.length)
    }
  })

  it('figura simetriska ir telpa 8x8 tinklelyje', () => {
    const { cells } = sprite('jonas')
    const key = (r: number, c: number) => cells.some(([row, col]) => row === r && col === c)
    for (const [row, col] of cells) {
      expect(row).toBeGreaterThanOrEqual(0)
      expect(col).toBeLessThan(8)
      expect(key(row, 7 - col)).toBe(true)
    }
  })
})
