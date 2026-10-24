import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { AppRoutes } from '../App'
import type { GoalDetail } from '../api/types'
import { AuthProvider } from '../auth/AuthProvider'
import { setToken } from '../auth/tokenStorage'
import { todayInSaoPaulo } from '../format'
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

  it('hides owner actions from a member and offers reversal only of their own contributions', async () => {
    mockApi(goal({ myRole: 'MEMBER' }))
    renderDetail()

    await screen.findByRole('heading', { name: 'Viagem de férias' })
    expect(screen.queryByRole('button', { name: 'Concluir' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Arquivar' })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Contribuinte')).not.toBeInTheDocument()
    expect(screen.getByRole('form', { name: 'Registrar contribuição' })).toBeInTheDocument()
    // The logged-in user (Carlos) may reverse only his own contribution
    expect(screen.getAllByRole('button', { name: 'Estornar' })).toHaveLength(1)
    const ownRow = screen.getByRole('button', { name: 'Estornar' }).closest('tr') as HTMLElement
    expect(ownRow).toHaveTextContent('100,00')
    expect(ownRow).not.toHaveTextContent('Pedro')
  })

  it('does not offer the contribution form to a member who does not contribute', async () => {
    mockApi(goal({ myRole: 'MEMBER', iContribute: false }))
    renderDetail()

    await screen.findByRole('heading', { name: 'Viagem de férias' })
    expect(screen.queryByRole('form', { name: 'Registrar contribuição' })).not.toBeInTheDocument()
  })

  it('offers neither registration nor reversal on a completed goal, only reopening', async () => {
    mockApi(goal({ status: 'COMPLETED' }))
    renderDetail()

    await screen.findByRole('heading', { name: 'Viagem de férias' })
    expect(screen.queryByRole('form', { name: 'Registrar contribuição' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Estornar' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reabrir' })).toBeInTheDocument()
  })

  it('lets the owner register a contribution on behalf of a participant', async () => {
    const fetchMock = mockApi(goal(), (url, init) =>
      url === '/api/goals/trip/contributions' && init?.method === 'POST' ? jsonResponse(201, {}) : undefined)
    const user = userEvent.setup()
    renderDetail()

    await screen.findByRole('heading', { name: 'Viagem de férias' })
    // Only people who contribute are offered: Ana does not contribute
    const contributor = screen.getByLabelText('Contribuinte')
    expect(within(contributor).queryByRole('option', { name: 'Ana' })).not.toBeInTheDocument()
    await user.selectOptions(contributor, 'pedro')
    await user.type(screen.getByLabelText('Valor (R$)'), '25.50')
    await user.click(screen.getByRole('button', { name: 'Registrar contribuição' }))

    const post = fetchMock.mock.calls.find(([url, init]) => url === '/api/goals/trip/contributions' && init?.method === 'POST')
    expect(JSON.parse(post?.[1]?.body as string)).toEqual({ amount: 25.5, contributorId: 'pedro' })
  })

  it('does not let a future date be picked for a contribution', async () => {
    mockApi(goal())
    renderDetail()

    await screen.findByRole('heading', { name: 'Viagem de férias' })
    expect(screen.getByLabelText('Data')).toHaveAttribute('max', todayInSaoPaulo())
  })

  it('asks for a reason before reversing', async () => {
    const fetchMock = mockApi(goal(), (url, init) =>
      url.endsWith('/reversal') && init?.method === 'POST' ? jsonResponse(200, {}) : undefined)
    const user = userEvent.setup()
    renderDetail()

    await screen.findByRole('heading', { name: 'Viagem de férias' })
    await user.click(screen.getAllByRole('button', { name: 'Estornar' })[0])
    const confirm = screen.getByRole('button', { name: 'Confirmar estorno' })
    expect(confirm).toBeDisabled()
    await user.type(screen.getByLabelText('Motivo do estorno'), 'Lançado em duplicidade')
    await user.click(confirm)

    const post = fetchMock.mock.calls.find(([url]) => url === '/api/goals/trip/contributions/c-carlos/reversal')
    expect(JSON.parse(post?.[1]?.body as string)).toEqual({ reason: 'Lançado em duplicidade' })
  })

  it('shows the backend message when an action is rejected', async () => {
    mockApi(goal(), (url) =>
      url === '/api/goals/trip/complete'
        ? jsonResponse(409, { title: 'Ação não permitida', detail: 'Só objetivos ativos podem ser concluídos.' })
        : undefined)
    const user = userEvent.setup()
    renderDetail()

    await user.click(await screen.findByRole('button', { name: 'Concluir' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Só objetivos ativos podem ser concluídos.')
  })

  it('shows "not found" for a goal the user cannot see', async () => {
    mockApi(null)
    renderDetail()

    expect(await screen.findByRole('alert')).toHaveTextContent('Objetivo não encontrado.')
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument()
  })
})
