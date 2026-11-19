import { useState, type FormEvent, type ReactNode } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/client'
import { logs } from '../api/endpoints'
import type { LogEntry, LogStatus, Quest } from '../api/types'
import { DIFFICULTY_INFO, formatDateTime, xp as formatXp } from '../lib/game'
import { useToast } from './toast'
import { Field, Modal } from './ui'

/** The game master's decision: radio (status), range (XP), number (inspiration) and textarea (note). */
export function ReviewModal({
  guildId,
  quest,
  entry,
  authorName,
  onClose,
}: {
  guildId: number
  quest: Quest
  entry: LogEntry
  authorName: string
  onClose: () => void
}) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const baseXp = DIFFICULTY_INFO[quest.difficulty].xp
  const [status, setStatus] = useState<LogStatus>('APPROVED')
  const [xp, setXp] = useState(baseXp)
  const [inspiration, setInspiration] = useState(0)
  const [note, setNote] = useState('')
  const [error, setError] = useState<string>()

  const review = useMutation({
    mutationFn: () =>
      logs.update(guildId, quest.id, entry.id, {
        status,
        reviewerNote: note || undefined,
        ...(status === 'APPROVED' ? { xpAwarded: xp, inspirationAwarded: inspiration } : {}),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['logs', guildId] })
      toast('success', status === 'APPROVED' ? `${authorName} earns ${formatXp(xp)}` : 'Entry rejected')
      onClose()
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : 'Could not save'),
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    setError(undefined)
    review.mutate()
  }

  return (
    <Modal open onClose={onClose} title="Review entry">
      <div className="review-summary">
        <p>
          <strong>{authorName}</strong> – “{quest.title}”, {formatDateTime(entry.occurredAt)}
        </p>
        {entry.note && <blockquote className="quote">{entry.note}</blockquote>}
        {entry.evidenceUrl && (
          <p>
            <a href={entry.evidenceUrl} target="_blank" rel="noreferrer">
              Open evidence
            </a>
          </p>
        )}
      </div>
      <form className="form" onSubmit={submit}>
        <fieldset className="choices">
          <legend>Decision</legend>
          <label className="choice">
            <input type="radio" name="status" checked={status === 'APPROVED'} onChange={() => setStatus('APPROVED')} />
            Approve
          </label>
          <label className="choice">
            <input type="radio" name="status" checked={status === 'REJECTED'} onChange={() => setStatus('REJECTED')} />
            Reject
          </label>
        </fieldset>
        {status === 'APPROVED' && (
          <div className="form__row form__row--2">
            <Field label={`Experience: ${formatXp(xp)}`} hint={`By difficulty – ${formatXp(baseXp)}`}>
              {(p) => (
                <input
                  {...p}
                  className="range"
                  type="range"
                  min={0}
                  max={200}
                  step={5}
                  value={xp}
                  onChange={(e) => setXp(Number(e.target.value))}
                />
              )}
            </Field>
            <Field label="Inspiration" hint="For an unusual or funny feat">
              {(p) => (
                <input
                  {...p}
                  className="input"
                  type="number"
                  min={0}
                  max={5}
                  value={inspiration}
                  onChange={(e) => setInspiration(Math.max(0, Number(e.target.value)))}
                />
              )}
            </Field>
          </div>
        )}
        <Field label="Note to the player" hint="Optional">
          {(p) => <textarea {...p} className="textarea" value={note} maxLength={500} onChange={(e) => setNote(e.target.value)} />}
        </Field>
        {error && <p className="field__error">{error}</p>}
        <div className="form__actions">
          <button type="button" className="btn btn--ghost" onClick={onClose}>
            Cancel
          </button>
          <button className={`btn ${status === 'REJECTED' ? 'btn--danger' : 'btn--success'}`} disabled={review.isPending}>
            {status === 'APPROVED' ? (
              <>
                Approve and award <span className="digits">{formatXp(xp)}</span>
              </>
            ) : (
              'Reject entry'
            )}
          </button>
        </div>
      </form>
    </Modal>
  )
}

export function ConfirmModal({
  title,
  children,
  confirmLabel,
  busy,
  onConfirm,
  onClose,
}: {
  title: string
  children: ReactNode
  confirmLabel: string
  busy?: boolean
  onConfirm: () => void
  onClose: () => void
}) {
  return (
    <Modal open onClose={onClose} title={title}>
      {children}
      <div className="form__actions">
        <button type="button" className="btn btn--ghost" onClick={onClose}>
          Cancel
        </button>
        <button type="button" className="btn btn--danger" disabled={busy} onClick={onConfirm}>
          {confirmLabel}
        </button>
      </div>
    </Modal>
  )
}
