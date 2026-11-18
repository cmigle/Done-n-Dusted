import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/client'
import { quests, type QuestInput } from '../api/endpoints'
import type { Difficulty, Quest, QuestType, Stat } from '../api/types'
import {
  DIFFICULTIES,
  DIFFICULTY_INFO,
  QUEST_TYPES,
  QUEST_TYPE_INFO,
  STATS,
  STAT_INFO,
  xp,
} from '../lib/game'
import { useToast } from './toast'
import { Field, Modal } from './ui'

/** Creates or edits a quest: text, textarea, select, radio, number and checkbox inputs. */
export function QuestFormModal({
  guildId,
  quest,
  isDm,
  onClose,
}: {
  guildId: number
  quest?: Quest
  isDm: boolean
  onClose: () => void
}) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [title, setTitle] = useState(quest?.title ?? '')
  const [description, setDescription] = useState(quest?.description ?? '')
  const [stat, setStat] = useState<Stat>(quest?.stat ?? 'STR')
  const [type, setType] = useState<QuestType>(quest?.type ?? 'MILESTONE')
  const [difficulty, setDifficulty] = useState<Difficulty>(quest?.difficulty ?? 'MEDIUM')
  const [targetCount, setTargetCount] = useState<string>(quest?.targetCount ? String(quest.targetCount) : '')
  const [active, setActive] = useState(quest?.active ?? true)
  const [error, setError] = useState<ApiError>()

  const save = useMutation({
    mutationFn: () => {
      const input: QuestInput = {
        title,
        description,
        stat,
        type,
        difficulty,
        active,
        targetCount: type === 'GRIND' ? Number(targetCount) || undefined : undefined,
      }
      return quest ? quests.update(guildId, quest.id, input) : quests.create(guildId, input)
    },
    onSuccess: async (saved) => {
      await queryClient.invalidateQueries({ queryKey: ['quests', guildId] })
      toast('success', quest ? 'Quest updated' : `Quest “${saved.title}” posted`)
      onClose()
    },
    onError: (err) => setError(err instanceof ApiError ? err : undefined),
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    setError(undefined)
    save.mutate()
  }

  const fieldError = (name: string) => error?.errorFor(name)

  return (
    <Modal open onClose={onClose} title={quest ? 'Edit quest' : 'New quest'}>
      <form className="form" onSubmit={submit} noValidate>
        <Field label="Title" hint="3–120 characters" error={fieldError('title') ?? (error?.status === 409 ? error.message : undefined)}>
          {(p) => <input {...p} className="input" value={title} maxLength={120} onChange={(e) => setTitle(e.target.value)} required />}
        </Field>
        <Field label="Description" error={fieldError('description')}>
          {(p) => (
            <textarea {...p} className="textarea" value={description} maxLength={1000} onChange={(e) => setDescription(e.target.value)} />
          )}
        </Field>
        <div className="form__row form__row--2">
          <Field label="Stat" hint={STAT_INFO[stat].hint}>
            {(p) => (
              <select {...p} className="select" value={stat} onChange={(e) => setStat(e.target.value as Stat)}>
                {STATS.map((s) => (
                  <option key={s} value={s}>
                    {STAT_INFO[s].name} ({STAT_INFO[s].abbr})
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Type" hint={type === 'SIDE_QUEST' && !isDm ? 'Only the game master can assign side quests' : QUEST_TYPE_INFO[type].hint}>
            {(p) => (
              <select {...p} className="select" value={type} onChange={(e) => setType(e.target.value as QuestType)}>
                {QUEST_TYPES.map((t) => (
                  <option key={t} value={t} disabled={t === 'SIDE_QUEST' && !isDm}>
                    {QUEST_TYPE_INFO[t].name}
                  </option>
                ))}
              </select>
            )}
          </Field>
        </div>
        <fieldset className="choices">
          <legend>Difficulty</legend>
          {DIFFICULTIES.map((d) => (
            <label key={d} className="choice">
              <input type="radio" name="difficulty" value={d} checked={difficulty === d} onChange={() => setDifficulty(d)} />
              {DIFFICULTY_INFO[d].name} <span className="muted">({xp(DIFFICULTY_INFO[d].xp)})</span>
            </label>
          ))}
        </fieldset>
        {type === 'GRIND' && (
          <Field label="Target (times)" hint="How many times it has to be done" error={fieldError('targetCount') ?? (error?.status === 422 ? error.message : undefined)}>
            {(p) => (
              <input
                {...p}
                className="input"
                type="number"
                min={1}
                max={100000}
                inputMode="numeric"
                value={targetCount}
                onChange={(e) => setTargetCount(e.target.value)}
                required
              />
            )}
          </Field>
        )}
        <label className="choice">
          <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
          Active – players can log progress
        </label>
        {error && !error.details.length && error.status !== 409 && !(type === 'GRIND' && error.status === 422) && (
          <p className="field__error">{error.message}</p>
        )}
        <div className="form__actions">
          <button type="button" className="btn btn--ghost" onClick={onClose}>
            Cancel
          </button>
          <button className="btn" disabled={save.isPending || title.trim().length < 3}>
            {quest ? 'Save' : 'Post quest'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
