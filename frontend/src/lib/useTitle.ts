import { useEffect } from 'react'

const APP = 'Done n Dusted'

/** Every page gets its own <title>: "Log in – Done n Dusted". Without a title, just the app name. */
export function useTitle(title?: string | null) {
  useEffect(() => {
    document.title = title ? `${title} – ${APP}` : APP
  }, [title])
}
