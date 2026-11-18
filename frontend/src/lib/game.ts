import type { Difficulty, GuildRole, LogStatus, QuestType, Stat, UserRole } from '../api/types'

export const STATS: Stat[] = ['STR', 'DEX', 'CON', 'INT', 'WIS', 'CHA']

export const STAT_INFO: Record<Stat, { name: string; abbr: string; hint: string; color: string }> = {
  STR: { name: 'Jėga', abbr: 'JĖG', hint: 'sportas, fizinis darbas', color: 'var(--stat-str)' },
  DEX: { name: 'Vikrumas', abbr: 'VIK', hint: 'judėjimas, rankdarbiai', color: 'var(--stat-dex)' },
  CON: { name: 'Ištvermė', abbr: 'IŠT', hint: 'įpročiai, sveikata', color: 'var(--stat-con)' },
  INT: { name: 'Intelektas', abbr: 'INT', hint: 'mokslai, darbas', color: 'var(--stat-int)' },
  WIS: { name: 'Išmintis', abbr: 'IŠM', hint: 'skaitymas, refleksija', color: 'var(--stat-wis)' },
  CHA: { name: 'Charizma', abbr: 'CHA', hint: 'žmonės, bendravimas', color: 'var(--stat-cha)' },
}

export const DIFFICULTIES: Difficulty[] = ['TRIVIAL', 'EASY', 'MEDIUM', 'HARD', 'LEGENDARY']

export const DIFFICULTY_INFO: Record<Difficulty, { name: string; xp: number }> = {
  TRIVIAL: { name: 'Labai lengva', xp: 10 },
  EASY: { name: 'Lengva', xp: 20 },
  MEDIUM: { name: 'Vidutinė', xp: 30 },
  HARD: { name: 'Sunki', xp: 50 },
  LEGENDARY: { name: 'Legendinė', xp: 80 },
}

export const QUEST_TYPES: QuestType[] = ['MILESTONE', 'HABIT', 'GRIND', 'SIDE_QUEST']

export const QUEST_TYPE_INFO: Record<QuestType, { name: string; hint: string }> = {
  MILESTONE: { name: 'Etapas', hint: 'vienkartinis tikslas' },
  HABIT: { name: 'Įprotis', hint: 'kartojasi nuolat' },
  GRIND: { name: 'Kaupiamoji', hint: 'renkama iki tikslo skaičiaus' },
  SIDE_QUEST: { name: 'Šalutinė', hint: 'skiria tik žaidimo vedėjas' },
}

export const STATUS_INFO: Record<LogStatus, { name: string; badge: string }> = {
  PENDING: { name: 'Laukia vedėjo', badge: 'badge--info' },
  APPROVED: { name: 'Patvirtinta', badge: 'badge--success' },
  REJECTED: { name: 'Atmesta', badge: 'badge--danger' },
}

export const GUILD_ROLE_INFO: Record<GuildRole, { name: string; short: string }> = {
  PLAYER: { name: 'Žaidėjas', short: 'Žaidėjas' },
  DUNGEON_MASTER: { name: 'Žaidimo vedėjas', short: 'Žaidimo vedėjas' },
}

export const USER_ROLE_INFO: Record<UserRole, string> = {
  USER: 'Naudotojas',
  ADMIN: 'Administratorius',
}

/** Patirties taskai su nekertamu tarpu tarp skaiciaus ir vieneto: „50 PT“. */
export const pt = (value: number) => `${value} PT`

/** Lietuviska skaiciaus ir daiktavardzio derme: 1 ir 21 - pirma forma, 2–9 - antra, 0 ir 10–20 - trecia. */
export function plural(n: number, one: string, few: string, many: string): string {
  const lastTwo = Math.abs(n) % 100
  const last = lastTwo % 10
  if (last === 1 && lastTwo !== 11) return one
  if (last >= 2 && (lastTwo < 10 || lastTwo >= 20)) return few
  return many
}

const dateFormat = new Intl.DateTimeFormat('lt-LT', { year: 'numeric', month: 'short', day: 'numeric' })
const dateTimeFormat = new Intl.DateTimeFormat('lt-LT', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export const formatDate = (iso?: string) => (iso ? dateFormat.format(new Date(iso)) : '')
export const formatDateTime = (iso?: string) => (iso ? dateTimeFormat.format(new Date(iso)) : '')

/** Reiksme <input type="datetime-local"> laukui pagal vietini laika. */
export function toLocalInputValue(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** "2026-11-18T10:30" (vietinis laikas) -> ISO UTC, kuri priima API. */
export const localInputToIso = (value: string) => new Date(value).toISOString()
