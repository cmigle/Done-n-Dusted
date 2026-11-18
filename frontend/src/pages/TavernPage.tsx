import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { Mail, Plus, Script } from '@nsmr/pixelart-react'
import { friends, guilds } from '../api/endpoints'
import { useAuth } from '../auth/AuthContext'
import { GuildFormModal } from '../components/GuildFormModal'
import { Empty, ErrorNote, Loading, PixelAvatar, Window } from '../components/ui'
import { GUILD_ROLE_INFO, USER_ROLE_INFO, formatDate, plural } from '../lib/game'
import { useTitle } from '../lib/useTitle'

export function TavernPage() {
  const { user, roleIn } = useAuth()
  const [creating, setCreating] = useState(false)
  useTitle('Tavern')
  const guildList = useQuery({ queryKey: ['guilds'], queryFn: () => guilds.list() })
  const pending = useQuery({ queryKey: ['friends', 'PENDING'], queryFn: () => friends.list('PENDING'), retry: false })
  const incoming = pending.data?.content.filter((f) => f.direction === 'INCOMING').length ?? 0

  if (!user) return null

  return (
    <>
      <div className="page-head">
        <h1>Tavern</h1>
        <p>Your guilds meet here. Pick where you are heading today.</p>
      </div>

      <div className="grid grid--sidebar">
        <aside className="stack">
          <Window>
            <div className="hero-card">
              <PixelAvatar name={user.username} size={96} />
              <div>
                <h2 className="hero-card__name">{user.username}</h2>
                <p className="muted">
                  {USER_ROLE_INFO[user.role]} since {formatDate(user.createdAt)}
                </p>
                <p>
                  Guilds: <strong>{user.memberships.length}</strong>
                </p>
              </div>
            </div>
          </Window>
          {incoming > 0 && (
            <Window title="Invitations" icon={<Mail size={24} aria-hidden="true" />} headingLevel={3}>
              <p>
                You have {incoming} new {plural(incoming, 'friend request', 'friend requests')}.
              </p>
              <Link to="/friends" className="btn btn--small">
                View
              </Link>
            </Window>
          )}
        </aside>

        <section aria-labelledby="my-guilds">
          <div className="row row--between section-head">
            <h2 id="my-guilds">Your guilds</h2>
            <button type="button" className="btn" onClick={() => setCreating(true)}>
              <Plus size={20} aria-hidden="true" /> Found a guild
            </button>
          </div>

          {guildList.isPending && <Loading />}
          {guildList.isError && <ErrorNote error={guildList.error} />}
          {guildList.data && guildList.data.content.length === 0 && (
            <Window>
              <Empty icon={<Script size={48} aria-hidden="true" />}>
                You are not in a guild yet. Found your own or ask a guild owner to add you.
              </Empty>
            </Window>
          )}
          <ul className="card-list grid grid--2">
            {guildList.data?.content.map((g) => {
              const role = roleIn(g.id)
              return (
                <li key={g.id}>
                  <Link to={`/guilds/${g.id}`} className="window window--link guild-card">
                    <h3>{g.name}</h3>
                    <p className="muted guild-card__desc">{g.description || 'No description'}</p>
                    <span className={`badge ${role === 'DUNGEON_MASTER' ? 'badge--plum' : role ? 'badge--info' : ''}`}>
                      {role ? GUILD_ROLE_INFO[role].name : 'Admin view'}
                    </span>
                  </Link>
                </li>
              )
            })}
          </ul>
        </section>
      </div>

      {creating && <GuildFormModal open onClose={() => setCreating(false)} />}
    </>
  )
}
