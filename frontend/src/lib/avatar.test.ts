import { describe, expect, it } from 'vitest'
import { sprite } from './avatar'

describe('sprite', () => {
  it('the same name gives the same avatar (case-insensitive)', () => {
    expect(sprite('Rokas')).toEqual(sprite('rokas'))
  })

  it('different names give different avatars', () => {
    expect(sprite('rokas')).not.toEqual(sprite('ieva'))
  })

  it('draws every cell once', () => {
    for (const name of ['rokas', 'ieva', 'jonas', 'admin', 'Aurelija']) {
      const keys = sprite(name).cells.map(([row, col]) => `${row}-${col}`)
      expect(new Set(keys).size).toBe(keys.length)
    }
  })

  it('the figure is symmetric and fits the 8x8 grid', () => {
    const { cells } = sprite('jonas')
    const key = (r: number, c: number) => cells.some(([row, col]) => row === r && col === c)
    for (const [row, col] of cells) {
      expect(row).toBeGreaterThanOrEqual(0)
      expect(col).toBeLessThan(8)
      expect(key(row, 7 - col)).toBe(true)
    }
  })
})
