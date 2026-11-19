import type { Stat } from '../api/types'
import { STATS, STAT_INFO, xp } from '../lib/game'
import { StatTile } from './StatIcon'
import { Bar, PixelAvatar } from './ui'

/** A sample character sheet for the landing page: shows at a glance what "your character grows" means. */
const DEMO: Record<Stat, number> = { STR: 13, DEX: 11, CON: 15, INT: 12, WIS: 16, CHA: 11 }

export function CharacterPreview() {
  return (
    <figure className="window preview">
      <div className="preview__head">
        <PixelAvatar name="Aurelia" size={64} />
        <div className="preview__who">
          <p className="preview__name">Aurelia</p>
          <p className="muted">Player in “Iron Vow”</p>
        </div>
        <span className="level-badge" aria-label="Level 4">
          <span aria-hidden="true">lv</span> 4
        </span>
      </div>
      <Bar value={72} max={100} segments={10} label="Experience: 72 of 100 to level 5" animate />
      <p className="preview__xp">
        <span className="xp-chip">72 / {xp(100)}</span> to level 5
      </p>
      <ul className="stats stats--compact">
        {STATS.map((stat) => (
          <li key={stat} className="stat">
            <span className="stat__icon">
              <StatTile stat={stat} size="sm" />
            </span>
            <span className="stat__name">{STAT_INFO[stat].name}</span>
            <span className="stat__score">{DEMO[stat]}</span>
            <Bar value={DEMO[stat] - 10} max={10} color={STAT_INFO[stat].color} label={`${STAT_INFO[stat].name}: ${DEMO[stat]}`} animate />
          </li>
        ))}
      </ul>
      <figcaption className="preview__log">
        <span className="preview__log-label">Latest entry</span>
        <span>
          Ran 5 km along the river → <span className="xp-chip">+{xp(20)}</span> Constitution
        </span>
      </figcaption>
    </figure>
  )
}
