import type { ApiErrorBody, TokenResponse } from './types'

/** API adresas: vystant - Vite proxy (/api), debesyje - VITE_API_URL (pvz. https://...azurewebsites.net/api). */
export const API_BASE = (import.meta.env.VITE_API_URL ?? '/api').replace(/\/$/, '')

/** Swagger UI lives on the same server as the API. */
export const API_DOCS_URL = `${API_BASE.replace(/\/api$/, '')}/swagger-ui.html`

const REFRESH_KEY = 'dnd.refreshToken'

/**
 * The access token lives only in memory (an XSS payload cannot read it from localStorage); the refresh token
 * is kept in localStorage so a reload keeps you signed in. Ideally it would be an HttpOnly cookie (see LD2.md).
 */
let accessToken: string | null = null
let refreshing: Promise<boolean> | null = null
let sessionExpiredHandler: (() => void) | null = null

export class ApiError extends Error {
  readonly status: number
  readonly details: { field: string; message: string }[]

  constructor(status: number, message: string, details: { field: string; message: string }[] = []) {
    super(message)
    this.status = status
    this.details = details
  }

  /** Field error for a form, e.g. errorFor('name'). */
  errorFor(field: string): string | undefined {
    return this.details.find((d) => d.field === field)?.message
  }
}

export function onSessionExpired(handler: (() => void) | null) {
  sessionExpiredHandler = handler
}

export function hasStoredSession(): boolean {
  return readRefreshToken() !== null
}

export function storeTokens(tokens: TokenResponse) {
  accessToken = tokens.accessToken
  try {
    localStorage.setItem(REFRESH_KEY, tokens.refreshToken)
  } catch {
    /* private mode: the session lasts only until a reload */
  }
}

export function clearTokens() {
  accessToken = null
  try {
    localStorage.removeItem(REFRESH_KEY)
  } catch {
    /* nothing to clear */
  }
}

export function readRefreshToken(): string | null {
  try {
    return localStorage.getItem(REFRESH_KEY)
  } catch {
    return null
  }
}

/**
 * Gets a new token pair. Only one refresh runs at a time: the server rotates the refresh token and treats
 * reuse of an old one as theft (revoking every session), so parallel callers wait for the same promise.
 */
export function refreshSession(): Promise<boolean> {
  if (!refreshing) {
    refreshing = (async () => {
      const refreshToken = readRefreshToken()
      if (!refreshToken) return false
      try {
        const res = await fetch(`${API_BASE}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        })
        if (!res.ok) {
          clearTokens()
          return false
        }
        storeTokens((await res.json()) as TokenResponse)
        return true
      } catch {
        return false
      }
    })().finally(() => {
      refreshing = null
    })
  }
  return refreshing
}

type Query = Record<string, string | number | boolean | undefined | null>

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE'
  body?: unknown
  query?: Query
  /** false for public endpoints (login, registration). */
  auth?: boolean
}

export function buildUrl(path: string, query?: Query): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null && value !== '') params.append(key, String(value))
  }
  const qs = params.toString()
  return `${API_BASE}${path}${qs ? `?${qs}` : ''}`
}

async function toApiError(res: Response): Promise<ApiError> {
  try {
    const body = (await res.json()) as ApiErrorBody
    return new ApiError(res.status, body.message ?? res.statusText, body.details ?? [])
  } catch {
    return new ApiError(res.status, res.status >= 500 ? 'The server is temporarily unavailable. Try again in a minute.' : res.statusText)
  }
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, query, auth = true } = options
  const send = () => {
    const headers: Record<string, string> = { Accept: 'application/json' }
    if (body !== undefined) headers['Content-Type'] = 'application/json'
    if (auth && accessToken) headers.Authorization = `Bearer ${accessToken}`
    return fetch(buildUrl(path, query), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  }

  let res: Response
  try {
    res = await send()
  } catch {
    throw new ApiError(0, 'Could not reach the server. Check your connection.')
  }

  // Expired access token: refresh once and retry the request.
  if (res.status === 401 && auth && readRefreshToken()) {
    if (await refreshSession()) {
      res = await send()
    } else {
      sessionExpiredHandler?.()
    }
  }

  if (!res.ok) throw await toApiError(res)
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}
