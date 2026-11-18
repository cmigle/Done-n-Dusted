import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, api, buildUrl, clearTokens, readRefreshToken, storeTokens } from './client'

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

const tokens = (n: number) => ({ accessToken: `access-${n}`, tokenType: 'Bearer', expiresIn: 900, refreshToken: `refresh-${n}` })

describe('buildUrl', () => {
  it('praleidzia tuscius parametrus', () => {
    expect(buildUrl('/guilds', { name: '', size: 20, stat: undefined, active: null, sort: 'name,asc' })).toBe(
      '/api/guilds?size=20&sort=name%2Casc',
    )
  })
})

describe('api', () => {
  beforeEach(() => {
    clearTokens()
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('ApiError turi statusa ir lauku klaidas', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        json(400, { status: 400, message: 'Neteisingas uzklausos turinys', details: [{ field: 'name', message: 'per trumpas' }] }),
      ),
    )
    const error = (await api('/guilds', { method: 'POST', body: { name: 'x' } }).catch((e: unknown) => e)) as ApiError
    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.errorFor('name')).toBe('per trumpas')
  })

  it('pasibaigus access zetonui viena karta atnaujina ir pakartoja uzklausa', async () => {
    storeTokens(tokens(1))
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(json(401, { status: 401, message: 'Access zetonas pasibaiges' }))
      .mockResolvedValueOnce(json(200, tokens(2)))
      .mockResolvedValueOnce(json(200, { id: 1 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(api('/auth/me')).resolves.toEqual({ id: 1 })
    expect(fetchMock.mock.calls[1][0]).toBe('/api/auth/refresh')
    expect(fetchMock.mock.calls[2][1].headers.Authorization).toBe('Bearer access-2')
    expect(readRefreshToken()).toBe('refresh-2')
  })

  it('lygiagretus 401 sukelia tik viena refresh (rotacija + pakartotinio panaudojimo apsauga)', async () => {
    storeTokens(tokens(1))
    let refreshCalls = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init: RequestInit) => {
        if (url.endsWith('/auth/refresh')) {
          refreshCalls++
          return json(200, tokens(2))
        }
        const auth = (init.headers as Record<string, string>).Authorization
        return auth === 'Bearer access-2' ? json(200, { ok: true }) : json(401, { status: 401 })
      }),
    )

    await Promise.all([api('/a'), api('/b'), api('/c')])
    expect(refreshCalls).toBe(1)
  })

  it('nepavykus atnaujinti - isvalo sesija', async () => {
    storeTokens(tokens(1))
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValueOnce(json(401, { status: 401 }))
        .mockResolvedValueOnce(json(401, { status: 401, message: 'Refresh zetonas nebegalioja' })),
    )
    await expect(api('/auth/me')).rejects.toMatchObject({ status: 401 })
    expect(readRefreshToken()).toBeNull()
  })
})
