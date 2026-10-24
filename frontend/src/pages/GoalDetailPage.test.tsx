import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AppRoutes } from '../App'
import type { GoalDetail } from '../api/types'
import { AuthProvider } from '../auth/AuthProvider'
import { setToken } from '../auth/tokenStorage'
import { carlos, contributionsPage, goal, jsonResponse } from '../test/fixtures'

type Handler = (url: string, init?: RequestInit) => Response | undefined

/** Logged-in session whose API answers come from the given goal, plus an optional override. */
function mockApi(detail: GoalDetail | null, extra?: Handler) {
  setToken('token')
  const fetchMock = vi.fn((url: string, init?: RequestInit) => {
    const custom = extra?.(url, init)
    if (custom) {
      return Promise.resolve(custom)
    }
    if (url === '/api/me') {
      return Promise.resolve(jsonResponse(200, carlos))
    }
    if (url.startsWith('/api/goals/trip/contributions')) {
      return Promise.resolve(jsonResponse(200, contributionsPage()))
    }
    if (url === '/api/goals/trip') {
      return Promise.resolve(detail
        ? jsonResponse(200, detail)
        : jsonResponse(404, { title: 'Não encontrado', detail: 'Objetivo não encontrado.' }))
    }
    return Promise.resolve(jsonResponse(404, { detail: 'Não encontrado.' }))
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function renderDetail() {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={['/goals/trip']}>
        <AppRoutes />
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('GoalDetailPage', () => {
  it('shows progress, totals per member and contributions with reversals flagged', async () => {
    mockApi(goal())
    renderDetail()

    expect(await screen.findByRole('heading', { name: 'Viagem de férias' })).toBeInTheDocument()
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '15')
    expect(screen.getByText(/Faltam/)).toHaveTextContent('850,00')
    const reversedRow = screen.getByText(/Estornada por Pedro/).closest('tr') as HTMLElement
    expect(reversedRow).toHaveClass('reversed')
    expect(within(reversedRow).getByText(/Valor errado/)).toBeInTheDocument()
  })

  it('shows "not found" for a goal the user cannot see', async () => {
    mockApi(null)
    renderDetail()

    expect(await screen.findByRole('alert')).toHaveTextContent('Objetivo não encontrado.')
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument()
  })
})
