import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError, api } from '../api/client'
import type { FamilyMember, GoalRole, NewMember } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { MoneyInput } from '../components/MoneyInput'
import { centsToAmount } from '../format'

interface MemberChoice {
  selected: boolean
  role: GoalRole
  contributes: boolean
}

/** The creator becomes the owner (INV-06); other family members are optional (D-01). */
export function NewGoalPage() {
  const { me } = useAuth()
  const navigate = useNavigate()
  const [family, setFamily] = useState<FamilyMember[]>([])
  const [choices, setChoices] = useState<Record<string, MemberChoice>>({})
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [targetCents, setTargetCents] = useState<number | null>(null)
  const [dueDate, setDueDate] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let active = true
    api.familyMembers()
      .then((members) => active && setFamily(members))
      .catch(() => active && setError('Não foi possível carregar os membros da família.'))
    return () => {
      active = false
    }
  }, [])

  const others = family.filter((member) => member.id !== me?.id)
  const choiceOf = (id: string): MemberChoice => choices[id] ?? { selected: false, role: 'MEMBER', contributes: true }
  const updateChoice = (id: string, change: Partial<MemberChoice>) =>
    setChoices((current) => ({ ...current, [id]: { ...choiceOf(id), ...change } }))

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (targetCents === null) {
      return
    }
    setError(null)
    setSubmitting(true)
    const members: NewMember[] = others
      .filter((member) => choiceOf(member.id).selected)
      .map((member) => ({ userId: member.id, role: choiceOf(member.id).role, contributes: choiceOf(member.id).contributes }))
    try {
      const created = await api.createGoal({
        title: title.trim(),
        description: description.trim() || undefined,
        targetAmount: centsToAmount(targetCents),
        dueDate: dueDate || undefined,
        members,
      })
      navigate(`/goals/${created.id}`)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Não foi possível criar o objetivo.')
      setSubmitting(false)
    }
  }

  return (
    <section className="card">
      <Link to="/">← Objetivos</Link>
      <h1>Novo objetivo</h1>
      <form onSubmit={handleSubmit} className="form" aria-label="Novo objetivo">
        <label>
          Título
          <input type="text" maxLength={120} value={title} onChange={(e) => setTitle(e.target.value)} required />
        </label>
        <label>
          Descrição
          <textarea maxLength={1000} value={description} onChange={(e) => setDescription(e.target.value)} />
        </label>
        <label>
          Valor-alvo
          <MoneyInput cents={targetCents} onChange={setTargetCents} required />
        </label>
        <label>
          Data-limite (opcional)
          <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
        </label>

        <fieldset className="members-fieldset">
          <legend>Quem participa além de você</legend>
          <p className="muted">Sem ninguém selecionado, o objetivo é só seu.</p>
          {others.map((member) => {
            const choice = choiceOf(member.id)
            return (
              <div key={member.id} className="member-choice">
                <label className="checkbox">
                  <input
                    type="checkbox"
                    checked={choice.selected}
                    onChange={(e) => updateChoice(member.id, { selected: e.target.checked })}
                  />
                  {member.displayName}
                </label>
                {choice.selected && (
                  <>
                    <select
                      aria-label={`Papel de ${member.displayName}`}
                      value={choice.role}
                      onChange={(e) => updateChoice(member.id, { role: e.target.value as GoalRole })}
                    >
                      <option value="MEMBER">Membro</option>
                      <option value="OWNER">Dono</option>
                    </select>
                    <label className="checkbox">
                      <input
                        type="checkbox"
                        checked={choice.contributes}
                        onChange={(e) => updateChoice(member.id, { contributes: e.target.checked })}
                      />
                      contribui
                    </label>
                  </>
                )}
              </div>
            )
          })}
        </fieldset>

        {error && <p role="alert" className="error">{error}</p>}
        <button type="submit" className="button" disabled={submitting}>
          Criar objetivo
        </button>
      </form>
    </section>
  )
}
