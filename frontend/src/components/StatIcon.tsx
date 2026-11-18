import type { CSSProperties } from 'react'
import { BookOpen, Heart, HumanRun, Lightbulb, MoodHappy, Zap } from '@nsmr/pixelart-react'
import type { Stat } from '../api/types'
import { STAT_INFO } from '../lib/game'

const ICONS = { STR: Zap, DEX: HumanRun, CON: Heart, INT: BookOpen, WIS: Lightbulb, CHA: MoodHappy }

export function StatIcon({ stat, size = 24 }: { stat: Stat; size?: number }) {
  const Icon = ICONS[stat]
  return <Icon size={size} aria-hidden="true" />
}

const TILE = { sm: ['tile tile--sm', 16], md: ['tile', 20], lg: ['tile tile--lg', 32] } as const

/** The stat icon in ink on a square of the stat's colour: the same colour marks the stat everywhere. */
export function StatTile({ stat, size = 'md' }: { stat: Stat; size?: keyof typeof TILE }) {
  const [className, iconSize] = TILE[size]
  return (
    <span className={className} style={{ '--tile': STAT_INFO[stat].color } as CSSProperties}>
      <StatIcon stat={stat} size={iconSize} />
    </span>
  )
}
