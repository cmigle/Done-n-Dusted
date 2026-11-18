import type { ApiErrorBody, TokenResponse } from './types'

/** API adresas: vystant - Vite proxy (/api), debesyje - VITE_API_URL (pvz. https://...azurewebsites.net/api). */
export const API_BASE = (import.meta.env.VITE_API_URL ?? '/api').replace(/\/$/, '')

/** Swagger UI tame paciame serveryje kaip API. */
export const API_DOCS_URL = `${API_BASE.replace(/\/api$/, '')}/swagger-ui.html`

const REFRESH_KEY = 'dnd.refreshToken'

/**
 * Access zetonas laikomas tik atmintyje (XSS jo neperskaitys is localStorage), refresh - localStorage,
 * kad prisijungimas islikti perkrovus puslapi. Idealiu atveju refresh butu HttpOnly slapuke (LD2.md).
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

  /** Lauko klaida formai, pvz. errorFor('name'). */
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
    /* privatus rezimas - prisijungimas islieka tik iki perkrovimo */
  }
}

export function clearTokens() {
  accessToken = null
  try {
    localStorage.removeItem(REFRESH_KEY)
  } catch {
    /* nieko */
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
 * Nauja zetonu pora. Vienu metu vyksta tik vienas atnaujinimas: serveris rotuoja refresh zetona ir
 * pakartotini seno panaudojima laiko vagyste (atsaukia visas sesijas), todel lygiagretus kvietimai
 * laukia to paties pazado.
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
  /** false - viesas metodas (prisijungimas, registracija). */
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
    return new ApiError(res.status, res.status >= 500 ? 'Serveris laikinai nepasiekiamas – pabandyk po minutės' : res.statusText)
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
    throw new ApiError(0, 'Nepavyko susisiekti su serveriu – patikrink ryšį')
  }

  // Pasibaiges access zetonas: viena karta atnaujinam ir kartojam uzklausa.
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
