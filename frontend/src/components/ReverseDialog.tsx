import { useState, type FormEvent } from 'react'
import { ApiError, api } from '../api/client'
import type { Contribution } from '../api/types'

interface Props {
  goalId: string
  contribution: Contribution
  onReversed: () => void
}

/** Reversal asks for a reason (D-02); the contribution itself is never edited or deleted. */
export function ReverseDialog({ goalId, contribution, onReversed }: Props) {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (!open) {
    return (
      <button type="button" className="button secondary small" onClick={() => setOpen(true)}>
        Estornar
      </button>
    )
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await api.reverseContribution(goalId, contribution.id, reason.trim())
      onReversed()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Não foi possível estornar a contribuição.')
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="reverse-form" aria-label="Estornar contribuição">
      <label>
        Motivo do estorno
        <input type="text" maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} required />
      </label>
      {error && <p role="alert" className="error">{error}</p>}
      <div className="button-row">
        <button type="submit" className="button danger small" disabled={submitting || reason.trim() === ''}>
          Confirmar estorno
        </button>
        <button type="button" className="button secondary small" onClick={() => setOpen(false)}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
