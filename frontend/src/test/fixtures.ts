import type { ContributionPage, GoalDetail, MeResponse } from '../api/types'

export function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

export const carlos: MeResponse = {
  id: 'carlos', email: 'carlos@silva.demo', displayName: 'Carlos', family: { id: 'silva', name: 'Família Silva' },
}

export function goal(overrides: Partial<GoalDetail> = {}): GoalDetail {
  return {
    id: 'trip',
    title: 'Viagem de férias',
    description: null,
    status: 'ACTIVE',
    targetAmount: 1000,
    currency: 'BRL',
    dueDate: null,
    createdBy: 'carlos',
    createdAt: '2026-10-01T12:00:00Z',
    completedAt: null,
    archivedAt: null,
    myRole: 'OWNER',
    iContribute: true,
    members: [
      { userId: 'carlos', displayName: 'Carlos', role: 'OWNER', contributes: true },
      { userId: 'pedro', displayName: 'Pedro', role: 'MEMBER', contributes: true },
      { userId: 'ana', displayName: 'Ana', role: 'MEMBER', contributes: false },
    ],
    progress: {
      targetAmount: 1000, accumulatedAmount: 150, remainingAmount: 850, surplusAmount: 0,
      progressPercentage: 15, targetReached: false,
    },
    contributionsByMember: [
      { userId: 'carlos', displayName: 'Carlos', amount: 100, currentMember: true },
      { userId: 'pedro', displayName: 'Pedro', amount: 50, currentMember: true },
    ],
    ...overrides,
  }
}

export function contributionsPage(): ContributionPage {
  return {
    content: [
      {
        id: 'c-carlos', goalId: 'trip', contributor: { id: 'carlos', displayName: 'Carlos' },
        registeredBy: { id: 'carlos', displayName: 'Carlos' }, amount: 100, contributedOn: '2026-10-20',
        note: null, createdAt: '2026-10-20T12:00:00Z', reversed: false, reversal: null,
      },
      {
        id: 'c-pedro', goalId: 'trip', contributor: { id: 'pedro', displayName: 'Pedro' },
        registeredBy: { id: 'pedro', displayName: 'Pedro' }, amount: 50, contributedOn: '2026-10-19',
        note: 'Mesada', createdAt: '2026-10-19T12:00:00Z', reversed: false, reversal: null,
      },
      {
        id: 'c-reversed', goalId: 'trip', contributor: { id: 'pedro', displayName: 'Pedro' },
        registeredBy: { id: 'pedro', displayName: 'Pedro' }, amount: 30, contributedOn: '2026-10-18',
        note: null, createdAt: '2026-10-18T12:00:00Z', reversed: true,
        reversal: { reversedBy: { id: 'pedro', displayName: 'Pedro' }, reason: 'Valor errado', reversedAt: '2026-10-18T13:00:00Z' },
      },
    ],
    page: 0,
    size: 10,
    totalElements: 3,
    totalPages: 1,
  }
}
