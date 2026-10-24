import { describe, expect, it, vi } from 'vitest'
import { ApiError, api, request, setUnauthorizedHandler } from './client'
import { getToken, setToken } from '../auth/tokenStorage'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

describe('api client', () => {
  it('sends the stored token as a bearer token', async () => {
    setToken('abc')
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, []))
    vi.stubGlobal('fetch', fetchMock)

    await api.listGoals()

    const [, init] = fetchMock.mock.calls[0]
    expect(init.headers.Authorization).toBe('Bearer abc')
  })

  it('turns a problem detail into an ApiError with the backend message', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      jsonResponse(404, { title: 'Não encontrado', detail: 'Objetivo não encontrado.' })))

    const error = await api.getGoal('x').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(404)
    expect((error as ApiError).message).toBe('Objetivo não encontrado.')
  })

  it('falls back to a Portuguese message when the body is not a problem detail', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('oops', { status: 403 })))

    const error = (await request('/api/x').catch((e: unknown) => e)) as ApiError

    expect(error.message).toBe('Você não tem permissão para esta ação.')
  })

  it('ends the session when an authenticated request is rejected with 401', async () => {
    setToken('expired')
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(401, { title: 'Não autenticado' })))

    await expect(api.me()).rejects.toBeInstanceOf(ApiError)

    expect(getToken()).toBeNull()
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('does not treat a failed login as an expired session', async () => {
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      jsonResponse(401, { title: 'Não autenticado', detail: 'E-mail ou senha inválidos.' })))

    await expect(api.login('a@b.c', 'x')).rejects.toThrow('E-mail ou senha inválidos.')
    expect(onUnauthorized).not.toHaveBeenCalled()
  })
})
