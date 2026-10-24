import type { ReactNode } from 'react'
import type { Contribution, ContributionPage } from '../api/types'
import { formatDate, formatDateTime, formatMoney } from '../format'

interface Props {
  page: ContributionPage
  onPageChange: (page: number) => void
  /** Extra cell per row, e.g. the reverse action; the backend still decides who may reverse. */
  renderAction?: (contribution: Contribution) => ReactNode
}

export function ContributionsTable({ page, onPageChange, renderAction }: Props) {
  if (page.totalElements === 0) {
    return <p className="muted">Nenhuma contribuição registrada.</p>
  }
  return (
    <>
      <table className="table">
        <thead>
          <tr>
            <th>Data</th>
            <th>Contribuinte</th>
            <th>Valor</th>
            <th>Registrado por</th>
            <th>Observação</th>
            {renderAction && <th aria-label="Ações" />}
          </tr>
        </thead>
        <tbody>
          {page.content.map((contribution) => (
            <tr key={contribution.id} className={contribution.reversed ? 'reversed' : undefined}>
              <td>{formatDate(contribution.contributedOn)}</td>
              <td>{contribution.contributor.displayName}</td>
              <td className="amount">{formatMoney(contribution.amount)}</td>
              <td>{contribution.registeredBy.displayName}</td>
              <td>
                {contribution.note}
                {contribution.reversal && (
                  <div className="reversal-note">
                    Estornada por {contribution.reversal.reversedBy.displayName} em{' '}
                    {formatDateTime(contribution.reversal.reversedAt)}: {contribution.reversal.reason}
                  </div>
                )}
              </td>
              {renderAction && <td>{renderAction(contribution)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
      {page.totalPages > 1 && (
        <div className="pager">
          <button type="button" className="button secondary" disabled={page.page === 0} onClick={() => onPageChange(page.page - 1)}>
            Anterior
          </button>
          <span>
            Página {page.page + 1} de {page.totalPages} · {page.totalElements} contribuições
          </span>
          <button type="button" className="button secondary" disabled={page.page + 1 >= page.totalPages} onClick={() => onPageChange(page.page + 1)}>
            Próxima
          </button>
        </div>
      )}
    </>
  )
}
