import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Search } from '@nsmr/pixelart-react'
import { users } from '../api/endpoints'
import type { UserRole } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../components/toast'
import { ErrorNote, Field, Loading, PixelAvatar, Window } from '../components/ui'
import { USER_ROLE_INFO, formatDate } from '../lib/game'
import { useTitle } from '../lib/useTitle'

export function AdminUsersPage() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  useTitle('Administravimas')
  const list = useQuery({
    queryKey: ['users', search, page],
    queryFn: () => users.list({ username: search || undefined, page }),
    placeholderData: (previous) => previous,
  })

  const update = useMutation({
    mutationFn: ({ id, input }: { id: number; input: { role?: UserRole; enabled?: boolean } }) => users.update(id, input),
    onSuccess: async (u) => {
      await queryClient.invalidateQueries({ queryKey: ['users'] })
      toast('success', `${u.username}: ${USER_ROLE_INFO[u.role].toLowerCase()}, ${u.enabled ? 'aktyvus' : 'užblokuotas'}`)
    },
    onError: (err) => toast('error', err.message),
  })

  return (
    <>
      <div className="page-head">
        <h1>Administravimas</h1>
        <p>Naudotojų rolės ir paskyrų blokavimas. Užblokuotas naudotojas atjungiamas nuo visų įrenginių.</p>
      </div>
      <Window>
        <div className="row row--between admin-tools">
          <Field label="Ieškoti pagal vardą">
            {(p) => (
              <span className="input-icon">
                <Search size={20} aria-hidden="true" />
                <input
                  {...p}
                  className="input"
                  type="search"
                  value={search}
                  onChange={(e) => {
                    setSearch(e.target.value)
                    setPage(0)
                  }}
                />
              </span>
            )}
          </Field>
          {list.data && <span className="muted">Iš viso: {list.data.totalElements}</span>}
        </div>
        {list.isPending && <Loading />}
        {list.isError && <ErrorNote error={list.error} />}
        {list.data && (
          <div className="table-wrap">
            <table className="table">
              <caption className="visually-hidden">Naudotojai</caption>
              <thead>
                <tr>
                  <th scope="col">Naudotojas</th>
                  <th scope="col">Rolė</th>
                  <th scope="col">Aktyvus</th>
                  <th scope="col">Užsiregistravo</th>
                </tr>
              </thead>
              <tbody>
                {list.data.content.map((u) => {
                  const self = u.id === user?.id
                  return (
                    <tr key={u.id}>
                      <td>
                        <span className="row">
                          <PixelAvatar name={u.username} size={32} /> {u.username}
                        </span>
                      </td>
                      <td>
                        <label>
                          <span className="visually-hidden">{u.username} rolė</span>
                          <select
                            className="select select--small"
                            value={u.role}
                            disabled={self}
                            onChange={(e) => update.mutate({ id: u.id, input: { role: e.target.value as UserRole } })}
                          >
                            <option value="USER">{USER_ROLE_INFO.USER}</option>
                            <option value="ADMIN">{USER_ROLE_INFO.ADMIN}</option>
                          </select>
                        </label>
                      </td>
                      <td>
                        <label className="choice">
                          <input
                            type="checkbox"
                            checked={u.enabled}
                            disabled={self}
                            onChange={(e) => update.mutate({ id: u.id, input: { enabled: e.target.checked } })}
                          />
                          <span className="visually-hidden">{u.username} aktyvus</span>
                          <span aria-hidden="true">{u.enabled ? 'Taip' : 'Ne'}</span>
                        </label>
                      </td>
                      <td>{formatDate(u.createdAt)}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
        {list.data && list.data.page.totalPages > 1 && (
          <nav className="pager" aria-label="Puslapiai">
            <button type="button" className="btn btn--ghost btn--small" disabled={!list.data.links.prev} onClick={() => setPage((p) => p - 1)}>
              Atgal
            </button>
            <span>
              {list.data.page.number + 1} / {list.data.page.totalPages}
            </span>
            <button type="button" className="btn btn--ghost btn--small" disabled={!list.data.links.next} onClick={() => setPage((p) => p + 1)}>
              Toliau
            </button>
          </nav>
        )}
      </Window>
    </>
  )
}
