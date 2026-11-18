import { Link } from 'react-router'
import type { Quest } from '../api/types'
import { DIFFICULTY_INFO, QUEST_TYPE_INFO, STAT_INFO, pt } from '../lib/game'
import { StatIcon } from './StatIcon'

export function QuestCard({ quest }: { quest: Quest }) {
  const stat = STAT_INFO[quest.stat]
  return (
    <Link
      to={`/grupes/${quest.guildId}/uzduotys/${quest.id}`}
      className={`window window--link quest-card${quest.active ? '' : ' quest-card--inactive'}`}
      style={{ borderColor: stat.color }}
    >
      <div className="quest-card__head">
        <span className="quest-card__stat" style={{ color: stat.color }}>
          <StatIcon stat={quest.stat} size={28} />
        </span>
        <h3>{quest.title}</h3>
      </div>
      <div className="row quest-card__meta">
        <span className="badge badge--outline" style={{ borderColor: stat.color }}>
          {stat.name}
        </span>
        <span className="badge badge--gold">{pt(quest.xpPerCompletion)}</span>
        <span className="muted">
          {QUEST_TYPE_INFO[quest.type].name}, {DIFFICULTY_INFO[quest.difficulty].name.toLowerCase()}
        </span>
        {!quest.active && <span className="badge">Neaktyvi</span>}
      </div>
    </Link>
  )
}
