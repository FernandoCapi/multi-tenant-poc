import type { GoalProgress } from '../api/types'
import { formatMoney, formatPercent } from '../format'

export function ProgressBar({ progress }: { progress: GoalProgress }) {
  const width = Math.min(progress.progressPercentage, 100)
  return (
    <div className="progress">
      <div
        className="progress-track"
        role="progressbar"
        aria-valuenow={progress.progressPercentage}
        aria-valuemin={0}
        aria-valuemax={100}
      >
        <div className={progress.targetReached ? 'progress-fill reached' : 'progress-fill'} style={{ width: `${width}%` }} />
      </div>
      <div className="progress-text">
        <span>
          {formatMoney(progress.accumulatedAmount)} de {formatMoney(progress.targetAmount)} ({formatPercent(progress.progressPercentage)})
        </span>
        <span>
          {progress.targetReached
            ? progress.surplusAmount > 0
              ? `Meta atingida, excedente de ${formatMoney(progress.surplusAmount)}`
              : 'Meta atingida'
            : `Faltam ${formatMoney(progress.remainingAmount)}`}
        </span>
      </div>
    </div>
  )
}
