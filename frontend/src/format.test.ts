import { describe, expect, it } from 'vitest'
import {
  centsToAmount,
  formatDate,
  formatDateTime,
  formatMoney,
  formatPercent,
  parseCents,
  todayInSaoPaulo,
} from './format'

// Intl uses a non-breaking space between the symbol and the value
const normalize = (text: string) => text.replace(/ /g, ' ')

describe('format', () => {
  it('formats money as BRL', () => {
    expect(normalize(formatMoney(3450))).toBe('R$ 3.450,00')
    expect(normalize(formatMoney(0.5))).toBe('R$ 0,50')
  })

  it('reads typed money as whole cents and back as the API amount', () => {
    expect(parseCents('R$ 1.234,56')).toBe(123456)
    expect(parseCents('2550')).toBe(2550)
    expect(parseCents('R$ 0,00')).toBeNull()
    expect(parseCents('abc')).toBeNull()
    expect(parseCents('9'.repeat(20))).toBe(999999999999999)
    expect(centsToAmount(2550)).toBe(25.5)
    expect(JSON.stringify({ amount: centsToAmount(10) })).toBe('{"amount":0.1}')
  })

  it('formats percentages with up to two decimals', () => {
    expect(formatPercent(28.75)).toBe('28,75%')
    expect(formatPercent(100)).toBe('100%')
  })

  it('formats calendar dates without shifting the day', () => {
    expect(formatDate('2026-10-23')).toBe('23/10/2026')
  })

  it('computes today in São Paulo, not in UTC', () => {
    // 00:37 UTC on the 24th is still 21:37 on the 23rd in São Paulo
    expect(todayInSaoPaulo(new Date('2026-10-24T00:37:14Z'))).toBe('2026-10-23')
    expect(todayInSaoPaulo(new Date('2026-10-24T03:00:00Z'))).toBe('2026-10-24')
  })

  it('formats instants in São Paulo time', () => {
    expect(normalize(formatDateTime('2026-10-24T00:37:14Z'))).toBe('23/10/2026, 21:37')
  })
})
