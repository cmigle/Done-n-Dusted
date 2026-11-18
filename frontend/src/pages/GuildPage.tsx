import { useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router'
import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import { Edit, Plus, Script, Trash, UserPlus, Users } from '@nsmr/pixelart-react'
import { guilds, logs, members, quests, type QuestFilters } from '../api/endpoints'
import type { Difficulty, GuildRole, LogEntry, Member, Quest, QuestType, Stat } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { GuildFormModal } from '../components/GuildFormModal'
import { QuestFormModal } from '../components/QuestFormModal'
import { ConfirmModal, ReviewModal } from '../components/ReviewModal'
import { QuestCard } from '../components/QuestCard'
import { useToast } from '../components/toast'
import { Empty, ErrorNote, Field, Loading, PixelAvatar, Window } from '../components/ui'
import {
  DIFFICULTIES,
  DIFFICULTY_INFO,
  GUILD_ROLE_INFO,
  QUEST_TYPES,
  QUEST_TYPE_INFO,
  STATS,
  STAT_INFO,
  formatDate,
  formatDateTime,
} from '../lib/game'
import { useTitle } from '../lib/useTitle'

type Tab = 'uzduotys' | 'nariai' | 'perziura'

export function GuildPage() {
  const guildId = Number(useParams().guildId)
  const { user, isAdmin, roleIn, reloadUser } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [params, setParams] = useSearchParams()
  const [editing, setEditing] = useState(false)
  const [deleting, setDeleting] = useState(false)

  const guild = useQuery({ queryKey: ['guilds', guildId], queryFn: () => guilds.get(guildId) })
  useTitle(guild.data?.name ?? 'Grupė')
  const memberList = useQuery({ queryKey: ['members', guildId], queryFn: () => members.list(guildId), enabled: guild.isSuccess })

  const role = roleIn(guildId)
  const isDm = role === 'DUNGEON_MASTER'
  const isOwner = guild.data?.ownerId === user?.id
  const tab = (params.get('skirtukas') as Tab) ?? 'uzduotys'
  const names = useMemo(
    () => new Map(memberList.data?.content.map((m) => [m.userId, m.username]) ?? []),
    [memberList.data],
  )

  const remove = useMutation({
    mutationFn: () => guilds.remove(guildId),
    onSuccess: async () => {
      await reloadUser()
      queryClient.removeQueries({ queryKey: ['guilds'] })
      toast('success', 'Grupė ištrinta')
      navigate('/taverna', { replace: true })
    },
    onError: (err) => toast('error', err.message),
  })

  if (guild.isPending) return <Loading />
  if (guild.isError) {
    return (
      <Window title="Grupė nepasiekiama">
        <ErrorNote error={guild.error} />
        <Link to="/taverna">Grįžti į taverną</Link>
      </Window>
    )
  }

  const g = guild.data
  const owner = memberList.data?.content.find((m) => m.userId === g.ownerId)
  const tabs: { id: Tab; label: string; show: boolean }[] = [
    { id: 'uzduotys', label: 'Užduotys', show: true },
    { id: 'nariai', label: 'Nariai', show: true },
    { id: 'perziura', label: 'Peržiūra', show: isDm },
  ]

  return (
    <>
      <nav aria-label="Naršymo kelias" className="crumbs">
        <Link to="/taverna">Taverna</Link> / <span aria-current="page">{g.name}</span>
      </nav>
      <div className="page-head row row--between">
        <div>
          <h1>{g.name}</h1>
          <p>{g.description || 'Grupė be aprašymo.'}</p>
          <div className="row">
            {role && <span className={`badge ${isDm ? 'badge--plum' : 'badge--info'}`}>{GUILD_ROLE_INFO[role].name}</span>}
            {!role && isAdmin && <span className="badge badge--plum">Administratoriaus peržiūra</span>}
            {owner && <span className="muted">Savininkas: {owner.username}</span>}
          </div>
        </div>
        {(isOwner || isAdmin) && (
          <div className="row">
            {isOwner && (
              <button type="button" className="btn btn--secondary btn--small" onClick={() => setEditing(true)}>
                <Edit size={18} aria-hidden="true" /> Redaguoti
              </button>
            )}
            <button type="button" className="btn btn--danger btn--small" onClick={() => setDeleting(true)}>
              <Trash size={18} aria-hidden="true" /> Ištrinti
            </button>
          </div>
        )}
      </div>

      <div className="tabs" role="tablist" aria-label="Grupės skiltys">
        {tabs
          .filter((t) => t.show)
          .map((t) => (
            <button
              key={t.id}
              type="button"
              role="tab"
              id={`tab-${t.id}`}
              aria-selected={tab === t.id}
              aria-controls={`panel-${t.id}`}
              className="tab"
              onClick={() => setParams({ skirtukas: t.id }, { replace: true })}
            >
              {t.label}
            </button>
          ))}
      </div>

      <div role="tabpanel" id={`panel-${tab}`} aria-labelledby={`tab-${tab}`}>
        {tab === 'uzduotys' && <QuestsTab guildId={guildId} isDm={isDm} canCreate={!!role} />}
        {tab === 'nariai' && (
          <MembersTab guildId={guildId} ownerId={g.ownerId} canManage={isOwner || isAdmin} list={memberList.data?.content} />
        )}
        {tab === 'perziura' && isDm && <ReviewTab guildId={guildId} names={names} />}
      </div>

      {editing && <GuildFormModal open guild={g} onClose={() => setEditing(false)} />}
      {deleting && (
        <ConfirmModal
          title="Ištrinti grupę?"
          confirmLabel="Ištrinti visam laikui"
          busy={remove.isPending}
          onConfirm={() => remove.mutate()}
          onClose={() => setDeleting(false)}
        >
          <p>
            „{g.name}“ bus ištrinta kartu su visomis užduotimis ir įrašais. Šio veiksmo atšaukti negalima.
          </p>
        </ConfirmModal>
      )}
    </>
  )
}

/* ---------- Uzduotys ---------- */
function QuestsTab({ guildId, isDm, canCreate }: { guildId: number; isDm: boolean; canCreate: boolean }) {
  const [filters, setFilters] = useState<QuestFilters>({ stat: '', type: '', difficulty: '', active: false, page: 0 })
  const [creating, setCreating] = useState(false)
  const list = useQuery({
    queryKey: ['quests', guildId, filters],
    queryFn: () => quests.list(guildId, filters),
    placeholderData: (previous) => previous,
  })
  const update = (patch: Partial<QuestFilters>) => setFilters((f) => ({ ...f, page: 0, ...patch }))

  return (
    <div className="stack">
      <Window className="window--compact">
        <form className="filters" onSubmit={(e) => e.preventDefault()} aria-label="Užduočių filtrai">
          <Field label="Charakteristika">
            {(p) => (
              <select {...p} className="select" value={filters.stat} onChange={(e) => update({ stat: e.target.value as Stat | '' })}>
                <option value="">Visos</option>
                {STATS.map((s) => (
                  <option key={s} value={s}>
                    {STAT_INFO[s].name}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Tipas">
            {(p) => (
              <select {...p} className="select" value={filters.type} onChange={(e) => update({ type: e.target.value as QuestType | '' })}>
                <option value="">Visi</option>
                {QUEST_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {QUEST_TYPE_INFO[t].name}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Sunkumas">
            {(p) => (
              <select
                {...p}
                className="select"
                value={filters.difficulty}
                onChange={(e) => update({ difficulty: e.target.value as Difficulty | '' })}
              >
                <option value="">Visi</option>
                {DIFFICULTIES.map((d) => (
                  <option key={d} value={d}>
                    {DIFFICULTY_INFO[d].name}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <label className="choice filters__check">
            <input type="checkbox" checked={!!filters.active} onChange={(e) => update({ active: e.target.checked })} />
            Tik aktyvios
          </label>
        </form>
      </Window>

      <div className="row row--between">
        <p className="muted" aria-live="polite">
          {list.data ? `Rasta užduočių: ${list.data.totalElements}` : ' '}
        </p>
        {canCreate && (
          <button type="button" className="btn" onClick={() => setCreating(true)}>
            <Plus size={20} aria-hidden="true" /> Nauja užduotis
          </button>
        )}
      </div>

      {list.isPending && <Loading />}
      {list.isError && <ErrorNote error={list.error} />}
      {list.data?.content.length === 0 && (
        <Window>
          <Empty icon={<Script size={48} aria-hidden="true" />}>
            {filters.stat || filters.type || filters.difficulty || filters.active
              ? 'Pagal šiuos filtrus užduočių nėra – pabandyk kitus.'
              : 'Užduočių dar nėra. Paskelbk pirmąją!'}
          </Empty>
        </Window>
      )}
      <ul className="card-list grid grid--3">
        {list.data?.content.map((q) => (
          <li key={q.id}>
            <QuestCard quest={q} />
          </li>
        ))}
      </ul>
      {list.data && list.data.page.totalPages > 1 && (
        <nav className="pager" aria-label="Puslapiai">
          <button
            type="button"
            className="btn btn--ghost btn--small"
            disabled={!list.data.links.prev}
            onClick={() => setFilters((f) => ({ ...f, page: (f.page ?? 0) - 1 }))}
          >
            Atgal
          </button>
          <span>
            {list.data.page.number + 1} / {list.data.page.totalPages}
          </span>
          <button
            type="button"
            className="btn btn--ghost btn--small"
            disabled={!list.data.links.next}
            onClick={() => setFilters((f) => ({ ...f, page: (f.page ?? 0) + 1 }))}
          >
            Toliau
          </button>
        </nav>
      )}

      {creating && <QuestFormModal guildId={guildId} isDm={isDm} onClose={() => setCreating(false)} />}
    </div>
  )
}

/* ---------- Nariai ---------- */
function MembersTab({
  guildId,
  ownerId,
  canManage,
  list,
}: {
  guildId: number
  ownerId: number
  canManage: boolean
  list?: Member[]
}) {
  const { user, reloadUser } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [username, setUsername] = useState('')
  const [role, setRole] = useState<GuildRole>('PLAYER')
  const [error, setError] = useState<string>()

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['members', guildId] })
  const add = useMutation({
    mutationFn: () => members.add(guildId, username.trim(), role),
    onSuccess: async (m) => {
      await refresh()
      setUsername('')
      toast('success', `${m.username} prisijungė prie grupės`)
    },
    onError: (err) => setError(err.message),
  })
  const changeRole = useMutation({
    mutationFn: ({ userId, newRole }: { userId: number; newRole: GuildRole }) => members.setRole(guildId, userId, newRole),
    onSuccess: async () => {
      await refresh()
      await reloadUser()
      toast('success', 'Rolė pakeista')
    },
    onError: (err) => toast('error', err.message),
  })
  const remove = useMutation({
    mutationFn: (userId: number) => members.remove(guildId, userId),
    onSuccess: async (_, userId) => {
      if (userId === user?.id) {
        await reloadUser()
        toast('success', 'Išėjai iš grupės')
        navigate('/taverna')
        return
      }
      await refresh()
      toast('success', 'Narys pašalintas')
    },
    onError: (err) => toast('error', err.message),
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    setError(undefined)
    add.mutate()
  }

  if (!list) return <Loading />

  return (
    <div className="grid grid--sidebar">
      {canManage ? (
        <Window title="Pakviesti narį" icon={<UserPlus size={24} aria-hidden="true" />} headingLevel={3}>
          <form className="form" onSubmit={submit}>
            <Field label="Naudotojo vardas" error={error}>
              {(p) => (
                <input {...p} className="input" value={username} onChange={(e) => setUsername(e.target.value)} required />
              )}
            </Field>
            <fieldset className="choices">
              <legend>Rolė</legend>
              {(['PLAYER', 'DUNGEON_MASTER'] as GuildRole[]).map((r) => (
                <label key={r} className="choice">
                  <input type="radio" name="member-role" checked={role === r} onChange={() => setRole(r)} />
                  {GUILD_ROLE_INFO[r].name}
                </label>
              ))}
            </fieldset>
            <div className="form__actions">
              <button className="btn" disabled={add.isPending || !username.trim()}>
                Pridėti narį
              </button>
            </div>
          </form>
        </Window>
      ) : (
        <Window title="Nariai" icon={<Users size={24} aria-hidden="true" />} headingLevel={3}>
          <p className="muted">Narius kviečia grupės savininkas. Kiekvieno nario kortelėje matyti jo lygis ir charakteristikos.</p>
        </Window>
      )}

      <Window title={`Nariai (${list.length})`} headingLevel={3}>
        <ul className="list">
          {list.map((m) => (
            <li key={m.userId} className="member">
              <PixelAvatar name={m.username} size={44} />
              <div className="member__info">
                <Link to={`/grupes/${guildId}/nariai/${m.userId}`} className="member__name">
                  {m.username}
                </Link>
                <span className="muted">nuo {formatDate(m.joinedAt)}</span>
              </div>
              <div className="member__actions">
                {canManage && m.userId !== ownerId ? (
                  <label className="visually-hidden-label">
                    <span className="visually-hidden">{m.username} rolė</span>
                    <select
                      className="select select--small"
                      value={m.role}
                      onChange={(e) => changeRole.mutate({ userId: m.userId, newRole: e.target.value as GuildRole })}
                    >
                      <option value="PLAYER">{GUILD_ROLE_INFO.PLAYER.name}</option>
                      <option value="DUNGEON_MASTER">{GUILD_ROLE_INFO.DUNGEON_MASTER.name}</option>
                    </select>
                  </label>
                ) : (
                  <span className={`badge ${m.role === 'DUNGEON_MASTER' ? 'badge--plum' : 'badge--info'}`}>
                    {GUILD_ROLE_INFO[m.role].short}
                  </span>
                )}
                {m.userId === ownerId && <span className="badge badge--plum">Savininkas</span>}
                {m.userId !== ownerId && (canManage || m.userId === user?.id) && (
                  <button
                    type="button"
                    className="btn btn--ghost btn--small"
                    onClick={() => remove.mutate(m.userId)}
                    aria-label={m.userId === user?.id ? 'Išeiti iš grupės' : `Pašalinti ${m.username}`}
                  >
                    {m.userId === user?.id ? 'Išeiti' : 'Pašalinti'}
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      </Window>
    </div>
  )
}

/* ---------- DM perziuros eile ---------- */
function ReviewTab({ guildId, names }: { guildId: number; names: Map<number, string> }) {
  const [reviewing, setReviewing] = useState<{ quest: Quest; entry: LogEntry } | null>(null)
  const questList = useQuery({
    queryKey: ['quests', guildId, 'all'],
    queryFn: () => quests.list(guildId, { page: 0, size: 100 }),
  })
  const allQuests = questList.data?.content ?? []
  const pendingQueries = useQueries({
    queries: allQuests.map((q) => ({
      queryKey: ['logs', guildId, q.id, 'PENDING'],
      queryFn: () => logs.list(guildId, q.id, { status: 'PENDING', size: 50 }),
    })),
  })
  const pending = pendingQueries.flatMap((res, i) => (res.data?.content ?? []).map((entry) => ({ quest: allQuests[i], entry })))
  const loading = questList.isPending || pendingQueries.some((q) => q.isPending)

  if (loading) return <Loading />

  return (
    <Window title={`Laukia peržiūros (${pending.length})`} headingLevel={3}>
      {pending.length === 0 ? (
        <Empty icon={<Script size={48} aria-hidden="true" />}>Visi įrašai peržiūrėti. Puikus darbas!</Empty>
      ) : (
        <ul className="list">
          {pending
            .sort((a, b) => a.entry.occurredAt.localeCompare(b.entry.occurredAt))
            .map(({ quest, entry }) => (
              <li key={entry.id} className="log">
                <PixelAvatar name={names.get(entry.authorId) ?? String(entry.authorId)} size={44} />
                <div className="log__body">
                  <p className="log__title">
                    <strong>{names.get(entry.authorId) ?? `#${entry.authorId}`}</strong> –{' '}
                    <Link to={`/grupes/${guildId}/uzduotys/${quest.id}`}>{quest.title}</Link>
                  </p>
                  {entry.note && <p className="log__note">{entry.note}</p>}
                  <span className="muted">{formatDateTime(entry.occurredAt)}</span>
                </div>
                <button type="button" className="btn btn--small" onClick={() => setReviewing({ quest, entry })}>
                  Peržiūrėti
                </button>
              </li>
            ))}
        </ul>
      )}
      {reviewing && (
        <ReviewModal
          guildId={guildId}
          quest={reviewing.quest}
          entry={reviewing.entry}
          authorName={names.get(reviewing.entry.authorId) ?? `#${reviewing.entry.authorId}`}
          onClose={() => setReviewing(null)}
        />
      )}
    </Window>
  )
}
