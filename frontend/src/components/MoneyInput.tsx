import { formatCents, parseCents } from '../format'

interface Props {
  cents: number | null
  onChange: (cents: number | null) => void
  required?: boolean
}

/**
 * Money field formatted as BRL while the user types: digits fill from the right
 * (2 → R$ 0,02, 25 → R$ 0,25, 2550 → R$ 25,50). The value is kept in whole cents, never as a
 * floating-point amount. An empty field stays empty, so `required` keeps working.
 */
export function MoneyInput({ cents, onChange, required }: Props) {
  return (
    <input
      type="text"
      inputMode="numeric"
      autoComplete="off"
      placeholder="R$ 0,00"
      value={cents === null ? '' : formatCents(cents)}
      onChange={(event) => onChange(parseCents(event.target.value))}
      required={required}
    />
  )
}
