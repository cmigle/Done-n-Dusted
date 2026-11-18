import { api } from './client'
import type {
  CharacterSheet,
  Difficulty,
  Friendship,
  FriendshipStatus,
  Guild,
  GuildRole,
  LogEntry,
  LogStatus,
  Me,
  Member,
  Paged,
  Quest,
  QuestType,
  Stat,
  TokenResponse,
  User,
  UserRole,
} from './types'

// ---------- Auth ----------
export const auth = {
  login: (username: string, password: string) =>
    api<TokenResponse>('/auth/login', { method: 'POST', body: { username, password }, auth: false }),
  register: (username: string, password: string) =>
    api<User>('/auth/register', { method: 'POST', body: { username, password }, auth: false }),
  logout: (refreshToken: string) =>
    api<void>('/auth/logout', { method: 'POST', body: { refreshToken }, auth: false }),
  me: () => api<Me>('/auth/me'),
}

// ---------- Guilds ----------
export interface GuildInput {
  name: string
  description?: string
}
export const guilds = {
  list: (query: { name?: string; page?: number; size?: number } = {}) =>
    api<Paged<Guild>>('/guilds', { query: { sort: 'name,asc', size: 50, ...query } }),
  get: (id: number) => api<Guild>(`/guilds/${id}`),
  create: (input: GuildInput) => api<Guild>('/guilds', { method: 'POST', body: input }),
  update: (id: number, input: Partial<GuildInput>) =>
    api<Guild>(`/guilds/${id}`, { method: 'PATCH', body: input }),
  remove: (id: number) => api<void>(`/guilds/${id}`, { method: 'DELETE' }),
}

// ---------- Members ----------
export const members = {
  list: (guildId: number) =>
    api<Paged<Member>>(`/guilds/${guildId}/members`, { query: { size: 100, sort: 'joinedAt,asc' } }),
  add: (guildId: number, username: string, role: GuildRole) =>
    api<Member>(`/guilds/${guildId}/members`, { method: 'POST', body: { username, role } }),
  setRole: (guildId: number, userId: number, role: GuildRole) =>
    api<Member>(`/guilds/${guildId}/members/${userId}`, { method: 'PATCH', body: { role } }),
  remove: (guildId: number, userId: number) =>
    api<void>(`/guilds/${guildId}/members/${userId}`, { method: 'DELETE' }),
  sheet: (guildId: number, userId: number) =>
    api<CharacterSheet>(`/guilds/${guildId}/members/${userId}/sheet`),
}

// ---------- Quests ----------
export interface QuestInput {
  title: string
  description?: string
  stat: Stat
  type: QuestType
  difficulty: Difficulty
  targetCount?: number | null
  active?: boolean
}
export interface QuestFilters {
  stat?: Stat | ''
  type?: QuestType | ''
  difficulty?: Difficulty | ''
  active?: boolean
  page?: number
  size?: number
}
export const quests = {
  list: (guildId: number, filters: QuestFilters = {}) =>
    api<Paged<Quest>>(`/guilds/${guildId}/quests`, {
      query: { size: 12, sort: 'title,asc', ...filters, active: filters.active ? true : undefined },
    }),
  get: (guildId: number, questId: number) => api<Quest>(`/guilds/${guildId}/quests/${questId}`),
  create: (guildId: number, input: QuestInput) =>
    api<Quest>(`/guilds/${guildId}/quests`, { method: 'POST', body: input }),
  update: (guildId: number, questId: number, input: Partial<QuestInput>) =>
    api<Quest>(`/guilds/${guildId}/quests/${questId}`, { method: 'PATCH', body: input }),
  remove: (guildId: number, questId: number) =>
    api<void>(`/guilds/${guildId}/quests/${questId}`, { method: 'DELETE' }),
}

// ---------- Log entries ----------
export interface LogInput {
  note?: string
  evidenceUrl?: string
  occurredAt: string
}
export interface ReviewInput {
  status: LogStatus
  xpAwarded?: number
  inspirationAwarded?: number
  reviewerNote?: string
}
export const logs = {
  list: (guildId: number, questId: number, query: { status?: LogStatus | ''; page?: number; size?: number } = {}) =>
    api<Paged<LogEntry>>(`/guilds/${guildId}/quests/${questId}/logs`, {
      query: { size: 20, sort: 'occurredAt,desc', ...query },
    }),
  create: (guildId: number, questId: number, input: LogInput) =>
    api<LogEntry>(`/guilds/${guildId}/quests/${questId}/logs`, { method: 'POST', body: input }),
  update: (guildId: number, questId: number, logId: number, input: Partial<LogInput> | ReviewInput) =>
    api<LogEntry>(`/guilds/${guildId}/quests/${questId}/logs/${logId}`, { method: 'PATCH', body: input }),
  remove: (guildId: number, questId: number, logId: number) =>
    api<void>(`/guilds/${guildId}/quests/${questId}/logs/${logId}`, { method: 'DELETE' }),
}

// ---------- Friends ----------
export const friends = {
  list: (status?: FriendshipStatus) =>
    api<Paged<Friendship>>('/friends', { query: { status, size: 100 } }),
  request: (username: string) => api<Friendship>('/friends', { method: 'POST', body: { username } }),
  respond: (id: number, status: 'ACCEPTED' | 'BLOCKED') =>
    api<Friendship>(`/friends/${id}`, { method: 'PATCH', body: { status } }),
  remove: (id: number) => api<void>(`/friends/${id}`, { method: 'DELETE' }),
  sheet: (id: number) => api<CharacterSheet>(`/friends/${id}/sheet`),
}

// ---------- Admin ----------
export const users = {
  list: (query: { username?: string; page?: number } = {}) =>
    api<Paged<User>>('/users', { query: { size: 20, sort: 'id,asc', ...query } }),
  update: (id: number, input: { role?: UserRole; enabled?: boolean }) =>
    api<User>(`/users/${id}`, { method: 'PATCH', body: input }),
}
