import { useEffect } from 'react'

const APP = 'Done n Dusted'

/** Kiekvienam puslapiui - savas <title>: „Prisijungimas – Done n Dusted“. Be pavadinimo - tik programos vardas. */
export function useTitle(title?: string | null) {
  useEffect(() => {
    document.title = title ? `${title} – ${APP}` : APP
  }, [title])
}
