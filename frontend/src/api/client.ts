import { clearToken, getToken } from '../auth/tokenStorage'
import type {
  ContributionPage,
  Contribution,
  CreateGoalRequest,
  FamilyMember,
  GoalDetail,
  GoalStatus,
  GoalSummary,
  LoginResponse,
  MeResponse,
  RegisterContributionRequest,
} from './types'

/** An error answer of the API, carrying the Portuguese problem detail written by the backend. */
export class ApiError extends Error {
  readonly status: number
  readonly title: string

  constructor(status: number, title: string, detail: string) {
    super(detail)
    this.status = status
    this.title = title
  }
}

const FALLBACK_MESSAGES: Record<number, string> = {
  401: 'Faça login para continuar.',
  403: 'Você não tem permissão para esta ação.',
  404: 'Não encontrado.',
  409: 'Ação não permitida no estado atual.',
  422: 'Verifique os dados informados.',
}

let unauthorizedHandler: () => void = () => {}

/** Called when an authenticated request comes back 401 (expired or invalid token). */
export function setUnauthorizedHandler(handler: () => void): void {
  unauthorizedHandler = handler
}

export async function request<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 401 && token) {
    clearToken()
    unauthorizedHandler()
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  let title = 'Erro'
  let detail = FALLBACK_MESSAGES[response.status] ?? 'Não foi possível concluir a operação.'
  try {
    const problem = (await response.json()) as { title?: string; detail?: string }
    title = problem.title ?? title
    detail = problem.detail ?? detail
  } catch {
    // Body is not a problem detail; keep the fallback message
  }
  return new ApiError(response.status, title, detail)
}

export const api = {
  login: (email: string, password: string) =>
    request<LoginResponse>('/api/auth/login', 'POST', { email, password }),
  me: () => request<MeResponse>('/api/me'),

  listGoals: (status?: GoalStatus) =>
    request<GoalSummary[]>(status ? `/api/goals?status=${status}` : '/api/goals'),
  getGoal: (goalId: string) => request<GoalDetail>(`/api/goals/${goalId}`),
  createGoal: (goal: CreateGoalRequest) => request<GoalDetail>('/api/goals', 'POST', goal),
  changeGoalStatus: (goalId: string, action: 'complete' | 'reopen' | 'archive' | 'unarchive') =>
    request<GoalDetail>(`/api/goals/${goalId}/${action}`, 'POST'),

  listContributions: (goalId: string, page: number, size: number) =>
    request<ContributionPage>(`/api/goals/${goalId}/contributions?page=${page}&size=${size}`),
  registerContribution: (goalId: string, contribution: RegisterContributionRequest) =>
    request<Contribution>(`/api/goals/${goalId}/contributions`, 'POST', contribution),
  reverseContribution: (goalId: string, contributionId: string, reason: string) =>
    request<Contribution>(`/api/goals/${goalId}/contributions/${contributionId}/reversal`, 'POST', { reason }),

  familyMembers: () => request<FamilyMember[]>('/api/family/members'),
}
