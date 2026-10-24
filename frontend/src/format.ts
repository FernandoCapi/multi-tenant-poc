import type { GoalRole, GoalStatus } from './api/types'

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const percent = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 })
const dateFormat = new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC' })
const dateTimeFormat = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short',
  timeStyle: 'short',
  timeZone: 'America/Sao_Paulo',
})
// en-CA formats as yyyy-mm-dd, the value format of <input type="date">
const isoDateInSaoPaulo = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' })

/** "Today" in the same time zone the backend uses to reject future contributions (D-18). */
export function todayInSaoPaulo(now: Date = new Date()): string {
  return isoDateInSaoPaulo.format(now)
}

/** Money comes already calculated by the backend; the client only formats it. */
export function formatMoney(amount: number): string {
  return currency.format(amount)
}

// Up to R$ 9.999.999.999.999,99: stays an exact integer number of cents in JavaScript
const MAX_CENT_DIGITS = 15

/**
 * Reads what the user typed in a money field as whole cents, cash-register style: only digits
 * count and they fill from the right ("2550" → 2550 cents = R$ 25,50). Empty or zero → null.
 */
export function parseCents(typed: string): number | null {
  const digits = typed.replace(/\D/g, '').replace(/^0+/, '').slice(0, MAX_CENT_DIGITS)
  return digits === '' ? null : Number(digits)
}

export function formatCents(cents: number): string {
  return currency.format(cents / 100)
}

/** Cents as the decimal amount the API expects (two places, e.g. 2550 → 25.5). */
export function centsToAmount(cents: number): number {
  return cents / 100
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
