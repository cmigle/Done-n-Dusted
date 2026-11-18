import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/client'
import { guilds } from '../api/endpoints'
import type { Guild } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { useToast } from './toast'
import { Field, Modal } from './ui'

/** Creates a guild (no `guild` prop) or edits the given one. */
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
      await reloadUser() // the new membership (the owner becomes the game master) shows up in /me
      toast('success', guild ? 'Guild updated' : `Guild “${saved.name}” founded – you are its game master`)
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
    <Modal open={open} onClose={onClose} title={guild ? 'Edit guild' : 'Found a guild'}>
      <form className="form" onSubmit={submit} noValidate>
        <Field
          label="Name"
          hint="3–60 characters, must be unique"
          error={error?.errorFor('name') ?? (error?.status === 409 ? error.message : undefined)}
        >
          {(p) => (
            <input {...p} className="input" value={name} maxLength={60} onChange={(e) => setName(e.target.value)} required />
          )}
        </Field>
        <Field label="Description" hint="What brings you together? Up to 500 characters" error={error?.errorFor('description')}>
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
            Cancel
          </button>
          <button className="btn" disabled={save.isPending || name.trim().length < 3}>
            {guild ? 'Save' : 'Found guild'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
