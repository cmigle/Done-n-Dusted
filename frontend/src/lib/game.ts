import type { Difficulty, GuildRole, LogStatus, QuestType, Stat, UserRole } from '../api/types'

export const STATS: Stat[] = ['STR', 'DEX', 'CON', 'INT', 'WIS', 'CHA']

export const STAT_INFO: Record<Stat, { name: string; abbr: string; hint: string; color: string }> = {
  STR: { name: 'Strength', abbr: 'STR', hint: 'sport, physical work', color: 'var(--stat-str)' },
  DEX: { name: 'Dexterity', abbr: 'DEX', hint: 'movement, crafts', color: 'var(--stat-dex)' },
  CON: { name: 'Constitution', abbr: 'CON', hint: 'habits, health', color: 'var(--stat-con)' },
  INT: { name: 'Intelligence', abbr: 'INT', hint: 'study, work', color: 'var(--stat-int)' },
  WIS: { name: 'Wisdom', abbr: 'WIS', hint: 'reading, reflection', color: 'var(--stat-wis)' },
  CHA: { name: 'Charisma', abbr: 'CHA', hint: 'people, socialising', color: 'var(--stat-cha)' },
}

export const DIFFICULTIES: Difficulty[] = ['TRIVIAL', 'EASY', 'MEDIUM', 'HARD', 'LEGENDARY']

export const DIFFICULTY_INFO: Record<Difficulty, { name: string; xp: number }> = {
  TRIVIAL: { name: 'Trivial', xp: 10 },
  EASY: { name: 'Easy', xp: 20 },
  MEDIUM: { name: 'Medium', xp: 30 },
  HARD: { name: 'Hard', xp: 50 },
  LEGENDARY: { name: 'Legendary', xp: 80 },
}

export const QUEST_TYPES: QuestType[] = ['MILESTONE', 'HABIT', 'GRIND', 'SIDE_QUEST']

export const QUEST_TYPE_INFO: Record<QuestType, { name: string; hint: string }> = {
  MILESTONE: { name: 'Milestone', hint: 'a one-off goal' },
  HABIT: { name: 'Habit', hint: 'repeats regularly' },
  GRIND: { name: 'Grind', hint: 'counts up to a target' },
  SIDE_QUEST: { name: 'Side quest', hint: 'assigned by the game master only' },
}

export const STATUS_INFO: Record<LogStatus, { name: string; badge: string }> = {
  PENDING: { name: 'Awaiting GM', badge: 'badge--info' },
  APPROVED: { name: 'Approved', badge: 'badge--success' },
  REJECTED: { name: 'Rejected', badge: 'badge--danger' },
}

export const GUILD_ROLE_INFO: Record<GuildRole, { name: string }> = {
  PLAYER: { name: 'Player' },
  DUNGEON_MASTER: { name: 'Game master' },
}

export const USER_ROLE_INFO: Record<UserRole, string> = {
  USER: 'User',
  ADMIN: 'Administrator',
}

/** Experience points with a non-breaking space between number and unit: "50 XP". */
export const xp = (value: number) => `${value} XP`

/** "1 entry", "3 entries". */
export const plural = (n: number, one: string, many: string) => (n === 1 ? one : many)

const dateFormat = new Intl.DateTimeFormat('en-GB', { year: 'numeric', month: 'short', day: 'numeric' })
const dateTimeFormat = new Intl.DateTimeFormat('en-GB', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export const formatDate = (iso?: string) => (iso ? dateFormat.format(new Date(iso)) : '')
export const formatDateTime = (iso?: string) => (iso ? dateTimeFormat.format(new Date(iso)) : '')

/** Value for an <input type="datetime-local"> in the browser's local time. */
export function toLocalInputValue(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** "2026-11-18T10:30" (local time) -> ISO UTC string the API accepts. */
export const localInputToIso = (value: string) => new Date(value).toISOString()
