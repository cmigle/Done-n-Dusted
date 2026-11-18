import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Mail, UserPlus, Users } from '@nsmr/pixelart-react'
import { friends } from '../api/endpoints'
import type { Friendship } from '../api/types'
import { CharacterSheetView } from '../components/CharacterSheetView'
import { useToast } from '../components/toast'
import { Empty, ErrorNote, Field, Loading, Modal, PixelAvatar, Window } from '../components/ui'
import { formatDate } from '../lib/game'
import { useTitle } from '../lib/useTitle'

export function FriendsPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [username, setUsername] = useState('')
  const [error, setError] = useState<string>()
  const [viewing, setViewing] = useState<Friendship | null>(null)
  useTitle('Friends')
  const all = useQuery({ queryKey: ['friends', 'all'], queryFn: () => friends.list() })

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['friends'] })
  const request = useMutation({
    mutationFn: () => friends.request(username.trim()),
    onSuccess: async (f) => {
      await refresh()
      setUsername('')
      toast('success', `Friend request sent to ${f.username}`)
    },
    onError: (err) => setError(err.message),
  })
  const respond = useMutation({
    mutationFn: ({ id, status }: { id: number; status: 'ACCEPTED' | 'BLOCKED' }) => friends.respond(id, status),
    onSuccess: async (f) => {
      await refresh()
      toast('success', f.status === 'ACCEPTED' ? `You and ${f.username} are now friends` : `${f.username} blocked`)
    },
    onError: (err) => toast('error', err.message),
  })
  const remove = useMutation({
    mutationFn: (id: number) => friends.remove(id),
    onSuccess: async () => {
      await refresh()
      toast('info', 'Updated')
    },
    onError: (err) => toast('error', err.message),
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    setError(undefined)
    request.mutate()
  }

  const list = all.data?.content ?? []
  const incoming = list.filter((f) => f.status === 'PENDING' && f.direction === 'INCOMING')
  const outgoing = list.filter((f) => f.status === 'PENDING' && f.direction === 'OUTGOING')
  const accepted = list.filter((f) => f.status === 'ACCEPTED')
  const blocked = list.filter((f) => f.status === 'BLOCKED')

  return (
    <>
      <div className="page-head">
        <h1>Friends</h1>
        <p>Friends see your public character sheet – level and stats, but not your log entries.</p>
      </div>

      <div className="grid grid--sidebar">
        <div className="stack">
          <Window title="Add a friend" icon={<UserPlus size={24} aria-hidden="true" />} headingLevel={2}>
            <form className="form" onSubmit={submit}>
              <Field label="Username" error={error}>
                {(p) => <input {...p} className="input" value={username} onChange={(e) => setUsername(e.target.value)} required />}
              </Field>
              <div className="form__actions">
                <button className="btn" disabled={request.isPending || !username.trim()}>
                  Send request
                </button>
              </div>
            </form>
          </Window>
          {incoming.length > 0 && (
            <Window title={`Friend requests (${incoming.length})`} icon={<Mail size={24} aria-hidden="true" />} headingLevel={2}>
              <ul className="list">
                {incoming.map((f) => (
                  <li key={f.id} className="friend">
                    <PixelAvatar name={f.username} size={40} />
                    <span className="friend__name">{f.username}</span>
                    <div className="row">
                      <button type="button" className="btn btn--success btn--small" onClick={() => respond.mutate({ id: f.id, status: 'ACCEPTED' })}>
                        Accept
                      </button>
                      <button type="button" className="btn btn--ghost btn--small" onClick={() => remove.mutate(f.id)}>
                        Decline
                      </button>
                      <button type="button" className="btn btn--ghost btn--small" onClick={() => respond.mutate({ id: f.id, status: 'BLOCKED' })}>
                        Block
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            </Window>
          )}
        </div>

        <div className="stack">
          <Window title={`Friends (${accepted.length})`} icon={<Users size={24} aria-hidden="true" />} headingLevel={2}>
            {all.isPending && <Loading />}
            {all.isError && <ErrorNote error={all.error} />}
            {all.isSuccess && accepted.length === 0 && (
              <Empty>No friends yet. Add someone from your guild!</Empty>
            )}
            <ul className="list">
              {accepted.map((f) => (
                <li key={f.id} className="friend">
                  <PixelAvatar name={f.username} size={40} />
                  <span className="friend__name">
                    {f.username}
                    <span className="muted"> since {formatDate(f.respondedAt ?? f.createdAt)}</span>
                  </span>
                  <div className="row">
                    <button type="button" className="btn btn--small" onClick={() => setViewing(f)}>
                      Sheet
                    </button>
                    <button type="button" className="btn btn--ghost btn--small" onClick={() => remove.mutate(f.id)}>
                      Remove
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </Window>
          {(outgoing.length > 0 || blocked.length > 0) && (
            <Window title="Other" headingLevel={2}>
              <ul className="list">
                {outgoing.map((f) => (
                  <li key={f.id} className="friend">
                    <PixelAvatar name={f.username} size={32} />
                    <span className="friend__name">
                      {f.username} <span className="badge badge--info">Awaiting reply</span>
                    </span>
                    <button type="button" className="btn btn--ghost btn--small" onClick={() => remove.mutate(f.id)}>
                      Cancel
                    </button>
                  </li>
                ))}
                {blocked.map((f) => (
                  <li key={f.id} className="friend">
                    <PixelAvatar name={f.username} size={32} />
                    <span className="friend__name">
                      {f.username} <span className="badge badge--danger">Blocked</span>
                    </span>
                    <button type="button" className="btn btn--ghost btn--small" onClick={() => remove.mutate(f.id)}>
                      Unblock
                    </button>
                  </li>
                ))}
              </ul>
            </Window>
          )}
        </div>
      </div>

      {viewing && <FriendSheetModal friendship={viewing} onClose={() => setViewing(null)} />}
    </>
  )
}

function FriendSheetModal({ friendship, onClose }: { friendship: Friendship; onClose: () => void }) {
  const sheet = useQuery({ queryKey: ['friend-sheet', friendship.id], queryFn: () => friends.sheet(friendship.id) })
  return (
    <Modal open onClose={onClose} title={`${friendship.username}'s sheet`}>
      {sheet.isPending && <Loading />}
      {sheet.isError && <ErrorNote error={sheet.error} />}
      {sheet.data && <CharacterSheetView sheet={sheet.data} />}
    </Modal>
  )
}
