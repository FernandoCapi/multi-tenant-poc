import { useState } from 'react'
import { ApiError, api } from '../api/client'
import type { GoalDetail } from '../api/types'

type Action = 'complete' | 'reopen' | 'archive' | 'unarchive'

const ACTIONS: Record<GoalDetail['status'], { action: Action; label: string }[]> = {
  ACTIVE: [{ action: 'complete', label: 'Concluir' }, { action: 'archive', label: 'Arquivar' }],
  COMPLETED: [{ action: 'reopen', label: 'Reabrir' }, { action: 'archive', label: 'Arquivar' }],
  ARCHIVED: [{ action: 'unarchive', label: 'Desarquivar' }],
}

/** Status transitions of the owner (D-14); hidden for members, and rejected by the backend anyway. */
export function GoalActions({ goal, onChanged }: { goal: GoalDetail; onChanged: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (goal.myRole !== 'OWNER') {
    return null
  }

  async function run(action: Action) {
    setError(null)
    setBusy(true)
    try {
      await api.changeGoalStatus(goal.id, action)
      onChanged()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Não foi possível alterar o objetivo.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <div className="button-row">
        {ACTIONS[goal.status].map(({ action, label }) => (
          <button key={action} type="button" className="button secondary" disabled={busy} onClick={() => run(action)}>
            {label}
          </button>
        ))}
      </div>
      {error && <p role="alert" className="error">{error}</p>}
    </div>
  )
}
