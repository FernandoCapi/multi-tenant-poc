import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AppRoutes } from '../App'
import { AuthProvider } from '../auth/AuthProvider'
import { setToken } from '../auth/tokenStorage'
import { carlos, goal, jsonResponse } from '../test/fixtures'

describe('NewGoalPage', () => {
  it('creates a goal with the chosen members and opens it', async () => {
    setToken('token')
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      if (url === '/api/me') {
        return Promise.resolve(jsonResponse(200, carlos))
      }
      if (url === '/api/family/members') {
        return Promise.resolve(jsonResponse(200, [
          { id: 'ana', displayName: 'Ana' }, { id: 'carlos', displayName: 'Carlos' }, { id: 'pedro', displayName: 'Pedro' },
        ]))
      }
      if (url === '/api/goals' && init?.method === 'POST') {
        return Promise.resolve(jsonResponse(201, goal({ id: 'new-goal', title: 'Presente' })))
      }
      if (url.startsWith('/api/goals/new-goal/contributions')) {
        return Promise.resolve(jsonResponse(200, { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }))
      }
      if (url === '/api/goals/new-goal') {
        return Promise.resolve(jsonResponse(200, goal({ id: 'new-goal', title: 'Presente' })))
      }
      return Promise.resolve(jsonResponse(404, {}))
    })
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/goals/new']}>
          <AppRoutes />
        </MemoryRouter>
      </AuthProvider>,
    )

    await user.type(await screen.findByLabelText('Título'), 'Presente')
    await user.type(screen.getByLabelText('Valor-alvo'), '30000')
    expect(screen.getByLabelText('Valor-alvo')).toHaveValue('R$ 300,00')
    // The creator is not offered: they own the goal anyway
    expect(screen.queryByRole('checkbox', { name: 'Carlos' })).not.toBeInTheDocument()
    await user.click(await screen.findByRole('checkbox', { name: 'Ana' }))
    await user.click(screen.getAllByRole('checkbox', { name: 'contribui' })[0])
    await user.click(screen.getByRole('button', { name: 'Criar objetivo' }))

    expect(await screen.findByRole('heading', { name: 'Presente' })).toBeInTheDocument()
    const post = fetchMock.mock.calls.find(([url, init]) => url === '/api/goals' && init?.method === 'POST')
    expect(JSON.parse(post?.[1]?.body as string)).toEqual({
      title: 'Presente',
      targetAmount: 300,
      members: [{ userId: 'ana', role: 'MEMBER', contributes: false }],
    })
  })
})
