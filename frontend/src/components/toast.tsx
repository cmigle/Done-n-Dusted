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
            {t.kind === 'success' && <Check size={20} aria-hidden="true" color="var(--leaf)" />}
            {t.kind === 'error' && <WarningBox size={20} aria-hidden="true" color="var(--ruby)" />}
            {t.kind === 'info' && <InfoBox size={20} aria-hidden="true" color="var(--sky)" />}
            <span>{t.text}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
