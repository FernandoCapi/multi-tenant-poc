import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { ApiError, api } from '../api/client'
import type { GoalSummary } from '../api/types'
import { ProgressBar } from '../components/ProgressBar'
import { ROLE_LABELS, STATUS_LABELS, formatDate } from '../format'

type Tab = 'current' | 'archived'

/** Only the goals the user is linked to; archived ones on their own tab (D-15). */
export function GoalsPage() {
  const [tab, setTab] = useState<Tab>('current')
  const [goals, setGoals] = useState<GoalSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    api.listGoals(tab === 'archived' ? 'ARCHIVED' : undefined)
      .then((result) => active && setGoals(result))
      .catch((e: unknown) => active && setError(e instanceof ApiError ? e.message : 'Erro ao carregar os objetivos.'))
    return () => {
      active = false
    }
  }, [tab])

  function changeTab(next: Tab) {
    setGoals(null)
    setError(null)
    setTab(next)
  }

  return (
    <section>
      <div className="section-header">
        <h1>Objetivos</h1>
        <Link to="/goals/new" className="button">Novo objetivo</Link>
      </div>
      <div className="tabs" role="tablist">
        <button type="button" role="tab" aria-selected={tab === 'current'} onClick={() => changeTab('current')}>
          Ativos e concluídos
        </button>
        <button type="button" role="tab" aria-selected={tab === 'archived'} onClick={() => changeTab('archived')}>
          Arquivados
        </button>
      </div>

      {error && <p role="alert" className="error">{error}</p>}
      {!error && goals === null && <p>Carregando…</p>}
      {goals?.length === 0 && <p className="muted">Nenhum objetivo por aqui.</p>}

      <ul className="goal-list">
        {goals?.map((goal) => (
          <li key={goal.id} className="card">
            <div className="goal-heading">
              <Link to={`/goals/${goal.id}`} className="goal-title">{goal.title}</Link>
              <span className={`badge status-${goal.status.toLowerCase()}`}>{STATUS_LABELS[goal.status]}</span>
            </div>
            <p className="muted">
              {ROLE_LABELS[goal.myRole]}
              {!goal.iContribute && ' · não contribui'}
              {goal.dueDate && ` · até ${formatDate(goal.dueDate)}`}
            </p>
            <ProgressBar progress={goal.progress} />
          </li>
        ))}
      </ul>
    </section>
  )
}
