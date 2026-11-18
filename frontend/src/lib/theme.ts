import { useCallback, useEffect, useState } from 'react'

export type Theme = 'light' | 'dark'

const KEY = 'dnd.theme'
const THEME_COLOR: Record<Theme, string> = { light: '#fff4e4', dark: '#2b2340' }
const systemQuery = () => window.matchMedia('(prefers-color-scheme: dark)')

function saved(): Theme | null {
  try {
    const value = localStorage.getItem(KEY)
    return value === 'light' || value === 'dark' ? value : null
  } catch {
    return null // storage blocked (private mode, strict settings): just follow the system
  }
}

function current(): Theme {
  return saved() ?? (systemQuery().matches ? 'dark' : 'light')
}

function paint(theme: Theme) {
  document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')?.setAttribute('content', THEME_COLOR[theme])
}

/**
 * The colour theme. Until the visitor picks one it follows the system setting (and its changes);
 * a pick is stored in localStorage and set as <html data-theme>, which index.html applies before first paint.
 */
export function useTheme() {
  const [theme, setTheme] = useState<Theme>(current)

  useEffect(() => {
    const query = systemQuery()
    const follow = () => {
      if (saved()) return
      const next = query.matches ? 'dark' : 'light'
      setTheme(next)
      paint(next)
    }
    query.addEventListener('change', follow)
    return () => query.removeEventListener('change', follow)
  }, [])

  const toggle = useCallback(() => {
    const next: Theme = current() === 'dark' ? 'light' : 'dark'
    const root = document.documentElement
    // A short colour cross-fade, only for this switch (the CSS skips it with reduced motion).
    root.classList.add('theme-switching')
    window.setTimeout(() => root.classList.remove('theme-switching'), 250)
    root.dataset.theme = next
    try {
      localStorage.setItem(KEY, next)
    } catch {
      /* not saved: the choice lasts until a reload */
    }
    paint(next)
    setTheme(next)
  }, [])

  return { theme, toggle }
}
