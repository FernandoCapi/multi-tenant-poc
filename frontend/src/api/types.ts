// Mirrors of the backend DTOs (backend/src/main/java/com/vesta/**/…Response.java)

export type GoalStatus = 'ACTIVE' | 'COMPLETED' | 'ARCHIVED'
export type GoalRole = 'OWNER' | 'MEMBER'

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface MeResponse {
  id: string
  email: string
  displayName: string
  family: { id: string; name: string }
}

export interface GoalProgress {
  targetAmount: number
  accumulatedAmount: number
  remainingAmount: number
  surplusAmount: number
  progressPercentage: number
  targetReached: boolean
}

export interface GoalSummary {
  id: string
  title: string
  status: GoalStatus
  targetAmount: number
  currency: string
  dueDate: string | null
  myRole: GoalRole
  iContribute: boolean
  progress: GoalProgress
}

export interface GoalMember {
  userId: string
  displayName: string
  role: GoalRole
  contributes: boolean
}

export interface ContributorTotal {
  userId: string
  displayName: string
  amount: number
  currentMember: boolean
}

export interface GoalDetail {
  id: string
  title: string
  description: string | null
  status: GoalStatus
  targetAmount: number
  currency: string
  dueDate: string | null
  createdBy: string
  createdAt: string
  completedAt: string | null
  archivedAt: string | null
  myRole: GoalRole
  iContribute: boolean
  members: GoalMember[]
  progress: GoalProgress
  contributionsByMember: ContributorTotal[]
}

export interface Person {
  id: string
  displayName: string
}

export interface Contribution {
  id: string
  goalId: string
  contributor: Person
  registeredBy: Person
  amount: number
  contributedOn: string
  note: string | null
  createdAt: string
  reversed: boolean
  reversal: { reversedBy: Person; reason: string; reversedAt: string } | null
}

export interface ContributionPage {
  content: Contribution[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface FamilyMember {
  id: string
  displayName: string
}

export interface NewMember {
  userId: string
  role: GoalRole
  contributes: boolean
}

export interface CreateGoalRequest {
  title: string
  description?: string
  targetAmount: number
  dueDate?: string
  members: NewMember[]
}

export interface RegisterContributionRequest {
  amount: number
  contributedOn?: string
  note?: string
  contributorId?: string
}
