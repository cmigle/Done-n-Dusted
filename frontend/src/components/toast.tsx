import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { Check, InfoBox, WarningBox } from '@nsmr/pixelart-react'

type Kind = 'success' | 'error' | 'info'
interface Toast {
  id: number
  kind: Kind
  text: string
}

const ToastContext = createContext<(kind: Kind, text: string) => void>(() => {})

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const push = useCallback((kind: Kind, text: string) => {
    const id = Date.now() + Math.random()
    setToasts((list) => [...list.slice(-2), { id, kind, text }])
    window.setTimeout(() => setToasts((list) => list.filter((t) => t.id !== id)), 4500)
  }, [])

  const value = useMemo(() => push, [push])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toasts" aria-live="polite" aria-atomic="false">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast--${t.kind}`} role={t.kind === 'error' ? 'alert' : 'status'}>
            <span className="tile tile--sm">
              {t.kind === 'success' && <Check size={16} aria-hidden="true" />}
              {t.kind === 'error' && <WarningBox size={16} aria-hidden="true" />}
              {t.kind === 'info' && <InfoBox size={16} aria-hidden="true" />}
            </span>
            <span>{t.text}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
