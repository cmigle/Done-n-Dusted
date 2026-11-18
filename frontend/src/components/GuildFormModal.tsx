import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/client'
import { guilds } from '../api/endpoints'
import type { Guild } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { useToast } from './toast'
import { Field, Modal } from './ui'

/** Grupes kurimas (guild nera) arba redagavimas (guild perduota). */
export function GuildFormModal({
  open,
  onClose,
  guild,
  onSaved,
}: {
  open: boolean
  onClose: () => void
  guild?: Guild
  onSaved?: (guild: Guild) => void
}) {
  const queryClient = useQueryClient()
  const { reloadUser } = useAuth()
  const toast = useToast()
  const [name, setName] = useState(guild?.name ?? '')
  const [description, setDescription] = useState(guild?.description ?? '')
  const [error, setError] = useState<ApiError>()

  const save = useMutation({
    mutationFn: () =>
      guild ? guilds.update(guild.id, { name, description }) : guilds.create({ name, description }),
    onSuccess: async (saved) => {
      await queryClient.invalidateQueries({ queryKey: ['guilds'] })
      await reloadUser() // nauja naryste (savininkas tampa DM) matosi /me
      toast('success', guild ? 'Grupė atnaujinta' : `Grupė „${saved.name}“ įkurta – tu esi jos žaidimo vedėjas`)
      onSaved?.(saved)
      onClose()
    },
    onError: (err) => setError(err instanceof ApiError ? err : undefined),
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    setError(undefined)
    save.mutate()
  }

  return (
    <Modal open={open} onClose={onClose} title={guild ? 'Redaguoti grupę' : 'Įkurti grupę'}>
      <form className="form" onSubmit={submit} noValidate>
        <Field
          label="Pavadinimas"
          hint="3–60 simbolių, unikalus"
          error={error?.errorFor('name') ?? (error?.status === 409 ? error.message : undefined)}
        >
          {(p) => (
            <input {...p} className="input" value={name} maxLength={60} onChange={(e) => setName(e.target.value)} required />
          )}
        </Field>
        <Field label="Aprašymas" hint="Kas jus vienija? Iki 500 simbolių" error={error?.errorFor('description')}>
          {(p) => (
            <textarea
              {...p}
              className="textarea"
              value={description}
              maxLength={500}
              onChange={(e) => setDescription(e.target.value)}
            />
          )}
        </Field>
        {error && !error.details.length && error.status !== 409 && <p className="field__error">{error.message}</p>}
        <div className="form__actions">
          <button type="button" className="btn btn--ghost" onClick={onClose}>
            Atšaukti
          </button>
          <button className="btn" disabled={save.isPending || name.trim().length < 3}>
            {guild ? 'Išsaugoti' : 'Įkurti grupę'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
