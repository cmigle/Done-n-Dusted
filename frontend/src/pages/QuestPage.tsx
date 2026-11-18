import { useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Edit, NotePlus, Script, Trash } from '@nsmr/pixelart-react'
import { ApiError } from '../api/client'
import { guilds, logs, members, quests } from '../api/endpoints'
import type { LogEntry, LogStatus } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { QuestFormModal } from '../components/QuestFormModal'
import { ConfirmModal, ReviewModal } from '../components/ReviewModal'
import { StatTile } from '../components/StatIcon'
import { useToast } from '../components/toast'
import { Empty, ErrorNote, Field, Loading, PixelAvatar, Window, WithDigits } from '../components/ui'
import {
  DIFFICULTY_INFO,
  QUEST_TYPE_INFO,
  STATUS_INFO,
  STAT_INFO,
  formatDateTime,
  localInputToIso,
  plural,
  toLocalInputValue,
  xp,
} from '../lib/game'
import { useTitle } from '../lib/useTitle'

export function QuestPage() {
  const params = useParams()
  const guildId = Number(params.guildId)
  const questId = Number(params.questId)
  const { user, isAdmin, roleIn } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [status, setStatus] = useState<LogStatus | ''>('')
  const [editing, setEditing] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [reviewing, setReviewing] = useState<LogEntry | null>(null)

  const guild = useQuery({ queryKey: ['guilds', guildId], queryFn: () => guilds.get(guildId) })
  const quest = useQuery({ queryKey: ['quests', guildId, 'one', questId], queryFn: () => quests.get(guildId, questId) })
  const memberList = useQuery({ queryKey: ['members', guildId], queryFn: () => members.list(guildId) })
  useTitle(quest.data?.title ?? 'Quest')
  const entries = useQuery({
    queryKey: ['logs', guildId, questId, status],
    queryFn: () => logs.list(guildId, questId, { status }),
  })
  const names = useMemo(
    () => new Map(memberList.data?.content.map((m) => [m.userId, m.username]) ?? []),
    [memberList.data],
  )

  const role = roleIn(guildId)
  const isDm = role === 'DUNGEON_MASTER'

  const removeQuest = useMutation({
    mutationFn: () => quests.remove(guildId, questId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['quests', guildId] })
      toast('success', 'Quest deleted')
      navigate(`/guilds/${guildId}`, { replace: true })
    },
    onError: (err) => toast('error', err.message),
  })
  const removeLog = useMutation({
    mutationFn: (logId: number) => logs.remove(guildId, questId, logId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['logs', guildId] })
      toast('success', 'Entry deleted')
    },
    onError: (err) => toast('error', err.message),
  })

  if (quest.isPending) return <Loading />
  if (quest.isError) {
    return (
      <Window title="Quest unavailable">
        <ErrorNote error={quest.error} />
        <Link to={`/guilds/${guildId}`}>Back to the guild</Link>
      </Window>
    )
  }

  const q = quest.data
  const stat = STAT_INFO[q.stat]
  const canEdit = isDm || isAdmin || guild.data?.ownerId === user?.id || q.createdBy === user?.id

  return (
    <>
      <nav aria-label="Breadcrumb" className="crumbs">
        <Link to="/tavern">Tavern</Link> / <Link to={`/guilds/${guildId}`}>{guild.data?.name ?? 'Guild'}</Link> /{' '}
        <span aria-current="page">{q.title}</span>
      </nav>

      <div className="grid grid--sidebar">
        <div className="stack">
          <Window className="quest-detail">
            <div className="quest-detail__icon">
              <StatTile stat={q.stat} size="lg" />
            </div>
            <h1 className="quest-detail__title">
              <WithDigits text={q.title} />
            </h1>
            {q.description && <p>{q.description}</p>}
            <dl className="facts">
              <div>
                <dt>Stat</dt>
                <dd className="stat-name">
                  <StatTile stat={q.stat} size="sm" /> {stat.name}
                </dd>
              </div>
              <div>
                <dt>Difficulty</dt>
                <dd>{DIFFICULTY_INFO[q.difficulty].name}</dd>
              </div>
              <div>
                <dt>Type</dt>
                <dd>{QUEST_TYPE_INFO[q.type].name}</dd>
              </div>
              <div>
                <dt>Reward</dt>
                <dd>
                  <span className="xp-chip">{xp(q.xpPerCompletion)}</span>
                </dd>
              </div>
              {q.targetCount && (
                <div>
                  <dt>Target</dt>
                  <dd>
                    {q.targetCount} {plural(q.targetCount, 'time', 'times')}
                  </dd>
                </div>
              )}
              <div>
                <dt>Created by</dt>
                <dd>{(q.createdBy && names.get(q.createdBy)) || '—'}</dd>
              </div>
            </dl>
            {!q.active && <p className="badge">Inactive – not accepting new entries</p>}
            {canEdit && (
              <div className="row">
                <button type="button" className="btn btn--secondary btn--small" onClick={() => setEditing(true)}>
                  <Edit size={18} aria-hidden="true" /> Edit
                </button>
                <button type="button" className="btn btn--danger btn--small" onClick={() => setDeleting(true)}>
                  <Trash size={18} aria-hidden="true" /> Delete
                </button>
              </div>
            )}
          </Window>
          {role && q.active && <LogForm guildId={guildId} questId={questId} />}
        </div>

        <Window
          title="Progress log"
          headingLevel={2}
          actions={
            <Field label="Status">
              {(p) => (
                <select {...p} className="select select--small" value={status} onChange={(e) => setStatus(e.target.value as LogStatus | '')}>
                  <option value="">All</option>
                  <option value="PENDING">{STATUS_INFO.PENDING.name}</option>
                  <option value="APPROVED">{STATUS_INFO.APPROVED.name}</option>
                  <option value="REJECTED">{STATUS_INFO.REJECTED.name}</option>
                </select>
              )}
            </Field>
          }
        >
          {!isDm && !isAdmin && <p className="muted">You see only your own entries – the game master sees everyone's.</p>}
          {entries.isPending && <Loading />}
          {entries.isError && <ErrorNote error={entries.error} />}
          {entries.data?.content.length === 0 && (
            <Empty icon={<Script size={48} aria-hidden="true" />}>No entries yet.</Empty>
          )}
          <ul className="list">
            {entries.data?.content.map((entry) => {
              const author = names.get(entry.authorId) ?? `#${entry.authorId}`
              const mine = entry.authorId === user?.id
              return (
                <li key={entry.id} className="log">
                  <PixelAvatar name={author} size={44} />
                  <div className="log__body">
                    <p className="log__title">
                      <strong>{author}</strong>{' '}
                      <span className={`badge ${STATUS_INFO[entry.status].badge}`}>{STATUS_INFO[entry.status].name}</span>{' '}
                      {entry.status === 'APPROVED' && (
                        <span className="badge badge--gold">
                          +{xp(entry.xpAwarded)}
                          {entry.inspirationAwarded > 0
                            ? `, +${entry.inspirationAwarded} inspiration`
                            : ''}
                        </span>
                      )}
                    </p>
                    {entry.note && <p className="log__note">{entry.note}</p>}
                    {entry.evidenceUrl && (
                      <a href={entry.evidenceUrl} target="_blank" rel="noreferrer">
                        Evidence
                      </a>
                    )}
                    {entry.reviewerNote && (
                      <p className="log__review">
                        Game master's note: <em>{entry.reviewerNote}</em>
                      </p>
                    )}
                    <span className="muted">{formatDateTime(entry.occurredAt)}</span>
                  </div>
                  <div className="log__actions">
                    {isDm && !mine && entry.status === 'PENDING' && (
                      <button type="button" className="btn btn--secondary btn--small" onClick={() => setReviewing(entry)}>
                        Review
                      </button>
                    )}
                    {((mine && entry.status === 'PENDING') || isDm || isAdmin) && (
                      <button
                        type="button"
                        className="btn btn--ghost btn--small btn--icon"
                        aria-label="Delete entry"
                        onClick={() => removeLog.mutate(entry.id)}
                      >
                        <Trash size={18} aria-hidden="true" />
                      </button>
                    )}
                  </div>
                </li>
              )
            })}
          </ul>
        </Window>
      </div>

      {editing && <QuestFormModal guildId={guildId} quest={q} isDm={isDm} onClose={() => setEditing(false)} />}
      {deleting && (
        <ConfirmModal
          title="Delete this quest?"
          confirmLabel="Delete"
          busy={removeQuest.isPending}
          onConfirm={() => removeQuest.mutate()}
          onClose={() => setDeleting(false)}
        >
          <p>All entries for “{q.title}” will be deleted too, including approved ones.</p>
        </ConfirmModal>
      )}
      {reviewing && (
        <ReviewModal
          guildId={guildId}
          quest={q}
          entry={reviewing}
          authorName={names.get(reviewing.authorId) ?? `#${reviewing.authorId}`}
          onClose={() => setReviewing(null)}
        />
      )}
    </>
  )
}

/** Logging progress: textarea, url and datetime-local inputs. */
function LogForm({ guildId, questId }: { guildId: number; questId: number }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [note, setNote] = useState('')
  const [evidenceUrl, setEvidenceUrl] = useState('')
  const [occurredAt, setOccurredAt] = useState(() => toLocalInputValue(new Date()))
  const [error, setError] = useState<ApiError>()

  const create = useMutation({
    mutationFn: () =>
      logs.create(guildId, questId, {
        note: note || undefined,
        evidenceUrl: evidenceUrl || undefined,
        occurredAt: localInputToIso(occurredAt),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['logs', guildId] })
      setNote('')
      setEvidenceUrl('')
      toast('success', 'Entry logged – waiting for the game master to approve it')
    },
    onError: (err) => setError(err instanceof ApiError ? err : undefined),
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    setError(undefined)
    create.mutate()
  }

  return (
    <Window title="Log progress" icon={<NotePlus size={24} aria-hidden="true" />} headingLevel={2}>
      <form className="form" onSubmit={submit} noValidate>
        <Field label="What did you do?" error={error?.errorFor('note')}>
          {(p) => (
            <textarea
              {...p}
              className="textarea"
              placeholder="E.g. ran 8 km along the river"
              value={note}
              maxLength={1000}
              onChange={(e) => setNote(e.target.value)}
            />
          )}
        </Field>
        <Field label="Evidence link" hint="Optional, e.g. a photo" error={error?.errorFor('evidenceUrl')}>
          {(p) => (
            <input
              {...p}
              className="input"
              type="url"
              inputMode="url"
              placeholder="https://"
              value={evidenceUrl}
              onChange={(e) => setEvidenceUrl(e.target.value)}
            />
          )}
        </Field>
        <Field label="When?" error={error?.errorFor('occurredAt') ?? (error && !error.details.length ? error.message : undefined)}>
          {(p) => (
            <input
              {...p}
              className="input"
              type="datetime-local"
              max={toLocalInputValue(new Date())}
              value={occurredAt}
              onChange={(e) => setOccurredAt(e.target.value)}
              required
            />
          )}
        </Field>
        <div className="form__actions">
          <button className="btn" disabled={create.isPending || !occurredAt}>
            Log entry
          </button>
        </div>
      </form>
    </Window>
  )
}
