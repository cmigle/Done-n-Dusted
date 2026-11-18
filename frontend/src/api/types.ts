// API DTO types, matching the backend responses (docs/openapi.json).

export type Stat = 'STR' | 'DEX' | 'CON' | 'INT' | 'WIS' | 'CHA'
export type QuestType = 'MILESTONE' | 'HABIT' | 'GRIND' | 'SIDE_QUEST'
export type Difficulty = 'TRIVIAL' | 'EASY' | 'MEDIUM' | 'HARD' | 'LEGENDARY'
export type LogStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type GuildRole = 'PLAYER' | 'DUNGEON_MASTER'
export type UserRole = 'USER' | 'ADMIN'
export type FriendshipStatus = 'PENDING' | 'ACCEPTED' | 'BLOCKED'

export interface Paged<T> {
  totalElements: number
  content: T[]
  page: { number: number; size: number; totalElements: number; totalPages: number }
  links: { self: string; first: string; prev?: string; next?: string; last: string }
}

export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  refreshToken: string
}

export interface Me {
  id: number
  username: string
  role: UserRole
  enabled: boolean
  createdAt: string
  memberships: { guildId: number; guildName: string; role: GuildRole }[]
}

export interface User {
  id: number
  username: string
  role: UserRole
  enabled: boolean
  createdAt: string
}

export interface Guild {
  id: number
  name: string
  description: string
  ownerId: number
  createdAt: string
}

export interface Quest {
  id: number
  guildId: number
  title: string
  description: string
  stat: Stat
  type: QuestType
  difficulty: Difficulty
  targetCount?: number
  active: boolean
  xpPerCompletion: number
  createdBy?: number
  createdAt: string
}

export interface LogEntry {
  id: number
  questId: number
  authorId: number
  note?: string
  evidenceUrl?: string
  status: LogStatus
  xpAwarded: number
  inspirationAwarded: number
  reviewerNote?: string
  reviewerId?: number
  occurredAt: string
  reviewedAt?: string
  createdAt: string
}

export interface Member {
  userId: number
  username: string
  role: GuildRole
  joinedAt: string
}

export interface StatLine {
  stat: Stat
  score: number
  xp: number
}

export interface CharacterSheet {
  userId: number
  username: string
  guildId?: number | null
  guildName?: string | null
  role?: GuildRole | null
  view: 'FULL' | 'PUBLIC'
  level: number
  totalXp: number
  xpIntoLevel: number
  xpForNextLevel: number
  inspiration: number
  streakDays: number
  stats: StatLine[]
  recentLogs?: {
    logId: number
    questId: number
    questTitle: string
    stat: Stat
    xpAwarded: number
    inspirationAwarded: number
    occurredAt: string
    reviewedAt?: string
  }[] | null
  pendingCount?: number | null
}

export interface Friendship {
  id: number
  userId: number
  username: string
  status: FriendshipStatus
  direction: 'OUTGOING' | 'INCOMING'
  createdAt: string
  respondedAt?: string
}

export interface ApiErrorBody {
  status: number
  error?: string
  message?: string
  path?: string
  details?: { field: string; message: string }[]
}
