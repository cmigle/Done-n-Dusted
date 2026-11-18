import { useEffect, useId, useRef, type CSSProperties, type ReactNode } from 'react'
import { Close } from '@nsmr/pixelart-react'
import { sprite } from '../lib/avatar'

/* ---------- Pikselinis avataras ---------- */
export function PixelAvatar({ name, size = 40 }: { name: string; size?: number }) {
  const s = sprite(name)
  return (
    <span className="avatar" style={{ width: size, height: size }} aria-hidden="true">
      <svg viewBox="0 0 8 8" shapeRendering="crispEdges" width={size - 6} height={size - 6}>
        {s.cells.map(([row, col, kind]) => (
          <rect key={`${row}-${col}-${kind}`} x={col} y={row} width="1" height="1" fill={kind === 1 ? s.body : s.trim} />
        ))}
      </svg>
    </span>
  )
}

/* ---------- RPG langas ---------- */
export function Window({
  title,
  icon,
  actions,
  children,
  className = '',
  headingLevel = 2,
}: {
  title?: ReactNode
  icon?: ReactNode
  actions?: ReactNode
  children: ReactNode
  className?: string
  headingLevel?: 2 | 3
}) {
  const Heading = headingLevel === 2 ? 'h2' : 'h3'
  return (
    <section className={`window ${className}`}>
      {title && (
        <div className="row row--between">
          <Heading className="window__title">
            {icon}
            {title}
          </Heading>
          {actions}
        </div>
      )}
      {children}
    </section>
  )
}

/* ---------- Modalinis langas (natyvus <dialog>: fokusas, Esc ir backdrop - narsykles) ---------- */
export function Modal({
  open,
  title,
  onClose,
  children,
}: {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
}) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      className="modal"
      aria-labelledby={titleId}
      onClose={onClose}
      onClick={(e) => {
        if (e.target === ref.current) onClose()
      }}
    >
      {open && (
        <div className="window">
          <div className="modal__head">
            <h2 id={titleId}>{title}</h2>
            <button type="button" className="btn btn--ghost btn--icon" onClick={onClose} aria-label="Uždaryti">
              <Close size={20} aria-hidden="true" />
            </button>
          </div>
          <div className="modal__body">{children}</div>
        </div>
      )}
    </dialog>
  )
}

/* ---------- Juosta (XP, charakteristika) ---------- */
export function Bar({
  value,
  max,
  color = 'var(--gold)',
  label,
  animate = false,
}: {
  value: number
  max: number
  color?: string
  label: string
  animate?: boolean
}) {
  const percent = max > 0 ? Math.max(0, Math.min(100, (value / max) * 100)) : 0
  return (
    <div
      className={`bar ${animate ? 'bar--animate' : ''}`}
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={max}
      aria-valuenow={value}
      style={{ '--value': `${percent}%`, '--fill': color } as CSSProperties}
    >
      <div className="bar__fill" />
    </div>
  )
}

/* ---------- Formos laukas ---------- */
export function Field({
  label,
  hint,
  error,
  children,
}: {
  label: string
  hint?: string
  error?: string
  children: (props: { id: string; 'aria-invalid'?: boolean; 'aria-describedby'?: string }) => ReactNode
}) {
  const id = useId()
  const describedBy = [hint && `${id}-hint`, error && `${id}-error`].filter(Boolean).join(' ') || undefined
  return (
    <div className="field">
      <label className="field__label" htmlFor={id}>
        {label}
      </label>
      {children({ id, 'aria-invalid': error ? true : undefined, 'aria-describedby': describedBy })}
      {hint && (
        <span className="field__hint" id={`${id}-hint`}>
          {hint}
        </span>
      )}
      {error && (
        <span className="field__error" id={`${id}-error`} role="alert">
          {error}
        </span>
      )}
    </div>
  )
}

/* ---------- Busenos ---------- */
export function Spinner({ label = 'Kraunama' }: { label?: string }) {
  return (
    <span className="spinner" role="status" aria-label={label}>
      <span />
      <span />
      <span />
    </span>
  )
}

export function Loading() {
  return (
    <div className="empty">
      <Spinner />
    </div>
  )
}

export function Empty({ icon, children }: { icon?: ReactNode; children: ReactNode }) {
  return (
    <div className="empty">
      {icon}
      <div>{children}</div>
    </div>
  )
}

export function ErrorNote({ error }: { error: unknown }) {
  const message = error instanceof Error ? error.message : 'Kažkas nepavyko'
  return (
    <p className="field__error" role="alert">
      {message}
    </p>
  )
}
