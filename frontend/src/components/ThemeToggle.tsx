import { Moon, Sun } from '@nsmr/pixelart-react'
import { useTheme } from '../lib/theme'

/** Sun in the night theme, moon in the day theme: the icon shows where the button takes you. */
export function ThemeToggle() {
  const { theme, toggle } = useTheme()
  const dark = theme === 'dark'
  return (
    <button
      type="button"
      className="btn btn--ghost btn--icon theme-toggle"
      aria-label="Toggle theme"
      title={dark ? 'Light theme' : 'Dark theme'}
      onClick={toggle}
    >
      {dark ? <Sun size={24} aria-hidden="true" /> : <Moon size={24} aria-hidden="true" />}
    </button>
  )
}
