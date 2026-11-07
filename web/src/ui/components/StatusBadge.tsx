/** A status's colors, a class of theme.css. */
export type StatusTone = 'not-started' | 'ordered' | 'in-progress' | 'extra' | 'finished'

/**
 * A record's status in a pill, with how much of it is left or done below, [currentAmount] in bold.
 * For the top right corner of a card.
 */
export function StatusBadge({ status, amount, currentAmount }: { status: string; amount: string; currentAmount: number }) {
  return (
    <div className="status-badge">
      <div className="status-pill">
        <span className="status-dot" />
        <span className="label-medium">{status}</span>
      </div>
      <span className="body-small">
        <BoldNumber text={amount} number={currentAmount} />
      </span>
    </div>
  )
}

/** [text] with the first [number] in it in bold. */
export function BoldNumber({ text, number }: { text: string; number: number }) {
  const current = String(number)
  const start = text.indexOf(current)
  if (start < 0) return <>{text}</>
  return (
    <>
      {text.slice(0, start)}
      <b>{current}</b>
      {text.slice(start + current.length)}
    </>
  )
}
