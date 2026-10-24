import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AppRoutes } from '../App'
import { AuthProvider } from '../auth/AuthProvider'
import { getToken } from '../auth/tokenStorage'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function renderAt(path: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <AppRoutes />
      </MemoryRouter>
    </AuthProvider>,
  )
}

async function fillAndSubmit() {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('E-mail'), 'carlos@silva.demo')
  await user.type(screen.getByLabelText('Senha'), 'vesta-demo')
  await user.click(screen.getByRole('button', { name: 'Entrar' }))
}

describe('LoginPage', () => {
  it('shows the backend message when the credentials are wrong', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      jsonResponse(401, { title: 'Não autenticado', detail: 'E-mail ou senha inválidos.' })))
    renderAt('/login')

    await fillAndSubmit()

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos.')
    expect(getToken()).toBeNull()
  })

  it('stores the token and opens the goal list after a successful login', async () => {
    vi.stubGlobal('fetch', vi.fn((url: string) => {
      if (url === '/api/auth/login') {
        return Promise.resolve(jsonResponse(200, { accessToken: 'token-123', tokenType: 'Bearer', expiresIn: 3600 }))
      }
      if (url === '/api/me') {
        return Promise.resolve(jsonResponse(200, {
          id: 'u1', email: 'carlos@silva.demo', displayName: 'Carlos', family: { id: 'f1', name: 'Família Silva' },
        }))
      }
      return Promise.resolve(jsonResponse(200, []))
    }))
    renderAt('/login')

    await fillAndSubmit()

    expect(await screen.findByRole('heading', { name: 'Objetivos' })).toBeInTheDocument()
    expect(screen.getByText('Carlos · Família Silva')).toBeInTheDocument()
    expect(getToken()).toBe('token-123')
  })

  it('sends visitors without a session to the login page', async () => {
    renderAt('/')

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
  })
})
