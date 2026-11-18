import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { Mail, Plus, Script } from '@nsmr/pixelart-react'
import { friends, guilds } from '../api/endpoints'
import { useAuth } from '../auth/AuthContext'
import { GuildFormModal } from '../components/GuildFormModal'
import { Empty, ErrorNote, Loading, PixelAvatar, Window } from '../components/ui'
import { GUILD_ROLE_INFO, USER_ROLE_INFO, formatDate } from '../lib/game'

export function TavernPage() {
  const { user, roleIn } = useAuth()
  const [creating, setCreating] = useState(false)
  const guildList = useQuery({ queryKey: ['guilds'], queryFn: () => guilds.list() })
  const pending = useQuery({ queryKey: ['friends', 'PENDING'], queryFn: () => friends.list('PENDING'), retry: false })
  const incoming = pending.data?.content.filter((f) => f.direction === 'INCOMING').length ?? 0

  if (!user) return null

  return (
    <>
      <div className="page-head">
        <h1>Taverna</h1>
        <p>Čia susitinka tavo grupės. Pasirink, kur šiandien keliausi.</p>
      </div>

      <div className="grid grid--sidebar">
        <aside className="stack">
          <Window>
            <div className="hero-card">
              <PixelAvatar name={user.username} size={96} />
              <div>
                <h2 className="hero-card__name">{user.username}</h2>
                <p className="muted">
                  {USER_ROLE_INFO[user.role]}, nuo {formatDate(user.createdAt)}
                </p>
                <p>
                  Grupių: <strong>{user.memberships.length}</strong>
                </p>
              </div>
            </div>
          </Window>
          {incoming > 0 && (
            <Window title="Pakvietimai" icon={<Mail size={24} aria-hidden="true" />} headingLevel={3}>
              <p>
                Turi {incoming} {incoming === 1 ? 'naują draugystės kvietimą' : 'naujus draugystės kvietimus'}.
              </p>
              <Link to="/draugai" className="btn btn--small">
                Peržiūrėti
              </Link>
            </Window>
          )}
        </aside>

        <section aria-labelledby="mano-grupes">
          <div className="row row--between section-head">
            <h2 id="mano-grupes">Tavo grupės</h2>
            <button type="button" className="btn" onClick={() => setCreating(true)}>
              <Plus size={20} aria-hidden="true" /> Įkurti grupę
            </button>
          </div>

          {guildList.isPending && <Loading />}
          {guildList.isError && <ErrorNote error={guildList.error} />}
          {guildList.data && guildList.data.content.length === 0 && (
            <Window>
              <Empty icon={<Script size={48} aria-hidden="true" />}>
                Dar nepriklausai jokiai grupei. Įkurk savo arba paprašyk DM tave pridėti.
              </Empty>
            </Window>
          )}
          <ul className="card-list grid grid--2">
            {guildList.data?.content.map((g) => {
              const role = roleIn(g.id)
              return (
                <li key={g.id}>
                  <Link to={`/grupes/${g.id}`} className="window window--link guild-card">
                    <h3>{g.name}</h3>
                    <p className="muted guild-card__desc">{g.description || 'Be aprašymo'}</p>
                    <span className={`badge ${role === 'DUNGEON_MASTER' ? 'badge--gold' : role ? 'badge--info' : ''}`}>
                      {role ? GUILD_ROLE_INFO[role].name : 'Administratoriaus peržiūra'}
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
