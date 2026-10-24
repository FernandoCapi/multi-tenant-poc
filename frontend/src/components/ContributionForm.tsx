import { useState, type FormEvent } from 'react'
import { ApiError, api } from '../api/client'
import type { GoalDetail } from '../api/types'
import { todayInSaoPaulo } from '../format'

interface Props {
  goal: GoalDetail
  currentUserId: string
  onRegistered: () => void
}

/**
 * Participants register their own contributions; owners may also register for any participant
 * (D-03). The list only offers who may contribute; the backend validates it again.
 */
export function ContributionForm({ goal, currentUserId, onRegistered }: Props) {
  const isOwner = goal.myRole === 'OWNER'
  const contributors = goal.members.filter(
    (member) => member.contributes && (isOwner || member.userId === currentUserId),
  )
  const [contributorId, setContributorId] = useState(
    contributors.some((member) => member.userId === currentUserId) ? currentUserId : contributors[0]?.userId ?? '',
  )
  const [amount, setAmount] = useState('')
  const [contributedOn, setContributedOn] = useState('')
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (contributors.length === 0) {
    return null
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await api.registerContribution(goal.id, {
        amount: Number(amount),
        contributedOn: contributedOn || undefined,
        note: note.trim() || undefined,
        contributorId: contributorId === currentUserId ? undefined : contributorId,
      })
      setAmount('')
      setContributedOn('')
      setNote('')
      onRegistered()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Não foi possível registrar a contribuição.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="form inline-form" aria-label="Registrar contribuição">
      {isOwner && (
        <label>
          Contribuinte
          <select value={contributorId} onChange={(e) => setContributorId(e.target.value)}>
            {contributors.map((member) => (
              <option key={member.userId} value={member.userId}>
                {member.displayName}
              </option>
            ))}
          </select>
        </label>
      )}
      <label>
        Valor (R$)
        <input type="number" min="0.01" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required />
      </label>
      <label>
        Data
        {/* Future days are disabled in the picker; the backend still rejects them (D-18) */}
        <input type="date" max={todayInSaoPaulo()} value={contributedOn} onChange={(e) => setContributedOn(e.target.value)} />
      </label>
      <label>
        Observação
        <input type="text" maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} />
      </label>
      {error && <p role="alert" className="error">{error}</p>}
      <button type="submit" className="button" disabled={submitting}>
        Registrar contribuição
      </button>
    </form>
  )
}
