import { Link } from 'react-router'
import type { Quest } from '../api/types'
import { DIFFICULTY_INFO, QUEST_TYPE_INFO, STAT_INFO, xp } from '../lib/game'
import type { CSSProperties } from 'react'
import { StatTile } from './StatIcon'

export function QuestCard({ quest }: { quest: Quest }) {
  const stat = STAT_INFO[quest.stat]
  return (
    <Link
      to={`/guilds/${quest.guildId}/quests/${quest.id}`}
      className={`window window--link quest-card${quest.active ? '' : ' quest-card--inactive'}`}
      style={{ '--accent': stat.color } as CSSProperties}
    >
      <div className="quest-card__head">
        <StatTile stat={quest.stat} />
        <h3>{quest.title}</h3>
      </div>
      <div className="row quest-card__meta">
        <span className="badge badge--stat">
          {stat.name}
        </span>
        <span className="badge badge--gold">{xp(quest.xpPerCompletion)}</span>
        <span className="muted">
          {QUEST_TYPE_INFO[quest.type].name}, {DIFFICULTY_INFO[quest.difficulty].name.toLowerCase()}
        </span>
        {!quest.active && <span className="badge">Inactive</span>}
      </div>
    </Link>
  )
}
