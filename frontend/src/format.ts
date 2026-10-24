import type { GoalRole, GoalStatus } from './api/types'

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const percent = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 })
const dateFormat = new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC' })
const dateTimeFormat = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short',
  timeStyle: 'short',
  timeZone: 'America/Sao_Paulo',
})

/** Money comes already calculated by the backend; the client only formats it. */
export function formatMoney(amount: number): string {
  return currency.format(amount)
}

export function formatPercent(value: number): string {
  return `${percent.format(value)}%`
}

/** Calendar dates (yyyy-mm-dd) have no time zone: format them as-is. */
export function formatDate(isoDate: string): string {
  return dateFormat.format(new Date(`${isoDate}T00:00:00Z`))
}

export function formatDateTime(isoInstant: string): string {
  return dateTimeFormat.format(new Date(isoInstant))
}

export const STATUS_LABELS: Record<GoalStatus, string> = {
  ACTIVE: 'Ativo',
  COMPLETED: 'Concluído',
  ARCHIVED: 'Arquivado',
}

export const ROLE_LABELS: Record<GoalRole, string> = {
  OWNER: 'Dono',
  MEMBER: 'Membro',
}
