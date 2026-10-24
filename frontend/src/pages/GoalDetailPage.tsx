import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { ApiError, api } from '../api/client'
import type { ContributionPage, GoalDetail } from '../api/types'
import { ContributionsTable } from '../components/ContributionsTable'
import { ProgressBar } from '../components/ProgressBar'
import { ROLE_LABELS, STATUS_LABELS, formatDate, formatMoney } from '../format'

export const CONTRIBUTIONS_PAGE_SIZE = 10

export function GoalDetailPage() {
  const { goalId = '' } = useParams()
  const [goal, setGoal] = useState<GoalDetail | null>(null)
  const [contributions, setContributions] = useState<ContributionPage | null>(null)
  const [page, setPage] = useState(0)
  const [error, setError] = useState<string | null>(null)
  useEffect(() => {
    let active = true
    Promise.all([api.getGoal(goalId), api.listContributions(goalId, page, CONTRIBUTIONS_PAGE_SIZE)])
      .then(([loadedGoal, loadedContributions]) => {
        if (active) {
          setGoal(loadedGoal)
          setContributions(loadedContributions)
          setError(null)
        }
      })
      .catch((e: unknown) => {
        // 404 for a goal the user cannot see, whatever the reason (D-09)
        if (active) {
          setError(e instanceof ApiError ? e.message : 'Erro ao carregar o objetivo.')
        }
      })
    return () => {
      active = false
    }
  }, [goalId, page])

  if (error && !goal) {
    return (
      <section className="card">
        <p role="alert" className="error">{error}</p>
        <Link to="/">Voltar para os objetivos</Link>
      </section>
    )
  }
  if (!goal || !contributions) {
    return <p>Carregando…</p>
  }

  return (
    <section className="detail">
      <Link to="/">← Objetivos</Link>
      <div className="goal-heading">
        <h1>{goal.title}</h1>
        <span className={`badge status-${goal.status.toLowerCase()}`}>{STATUS_LABELS[goal.status]}</span>
      </div>
      <p className="muted">
        Seu papel: {ROLE_LABELS[goal.myRole]}
        {!goal.iContribute && ' · você não contribui neste objetivo'}
        {goal.dueDate && ` · data-limite ${formatDate(goal.dueDate)}`}
      </p>
      {goal.description && <p>{goal.description}</p>}
      {error && <p role="alert" className="error">{error}</p>}

      <div className="card">
        <h2>Progresso</h2>
        <ProgressBar progress={goal.progress} />
      </div>

      <div className="columns">
        <div className="card">
          <h2>Total por membro</h2>
          {goal.contributionsByMember.length === 0 ? (
            <p className="muted">Ainda sem contribuições.</p>
          ) : (
            <ul className="plain-list">
              {goal.contributionsByMember.map((total) => (
                <li key={total.userId}>
                  <span>
                    {total.displayName}
                    {!total.currentMember && <span className="muted"> (saiu do objetivo)</span>}
                  </span>
                  <strong>{formatMoney(total.amount)}</strong>
                </li>
              ))}
            </ul>
          )}
        </div>
        <div className="card">
          <h2>Membros</h2>
          <ul className="plain-list">
            {goal.members.map((member) => (
              <li key={member.userId}>
                <span>{member.displayName}</span>
                <span className="muted">
                  {ROLE_LABELS[member.role]}
                  {!member.contributes && ' · não contribui'}
                </span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      <div className="card">
        <h2>Contribuições</h2>
        <ContributionsTable page={contributions} onPageChange={setPage} />
      </div>
    </section>
  )
}
