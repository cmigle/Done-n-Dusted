import { BookOpen, Heart, HumanRun, Lightbulb, MoodHappy, Zap } from '@nsmr/pixelart-react'
import type { Stat } from '../api/types'

const ICONS = { STR: Zap, DEX: HumanRun, CON: Heart, INT: BookOpen, WIS: Lightbulb, CHA: MoodHappy }

export function StatIcon({ stat, size = 24 }: { stat: Stat; size?: number }) {
  const Icon = ICONS[stat]
  return <Icon size={size} aria-hidden="true" />
}
