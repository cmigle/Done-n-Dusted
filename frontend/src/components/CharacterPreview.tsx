import type { Stat } from '../api/types'
import { STATS, STAT_INFO } from '../lib/game'
import { StatIcon } from './StatIcon'
import { Bar, PixelAvatar } from './ui'

/** Pavyzdine veikejo kortele pradzios puslapiui - is karto parodo, kas yra "veikejas auga". */
const DEMO: Record<Stat, number> = { STR: 13, DEX: 11, CON: 15, INT: 12, WIS: 16, CHA: 11 }

export function CharacterPreview() {
  return (
    <figure className="window preview">
      <div className="preview__head">
        <PixelAvatar name="Aurelija" size={64} />
        <div className="preview__who">
          <p className="preview__name">Aurelija</p>
          <p className="muted">Žaidėja grupėje „Geležinė priesaika“</p>
        </div>
        <span className="level-badge" aria-label="Lygis 4">
          <span aria-hidden="true">Lv</span> 4
        </span>
      </div>
      <Bar value={65} max={100} label="Patirtis: 65 iš 100 iki 5 lygio" animate />
      <p className="preview__xp">
        <span className="xp-number">65 / 100 XP</span> iki 5 lygio
      </p>
      <ul className="stats stats--compact">
        {STATS.map((stat) => (
          <li key={stat} className="stat">
            <span className="stat__icon" style={{ color: STAT_INFO[stat].color }}>
              <StatIcon stat={stat} size={20} />
            </span>
            <span className="stat__name">{STAT_INFO[stat].name}</span>
            <span className="stat__score">{DEMO[stat]}</span>
            <Bar value={DEMO[stat] - 10} max={10} color={STAT_INFO[stat].color} label={`${STAT_INFO[stat].name}: ${DEMO[stat]}`} animate />
          </li>
        ))}
      </ul>
      <figcaption className="preview__log">
        <span className="preview__log-label">Paskutinis žygis</span>
        <span>
          Nubėgau 5 km palei upę → <span className="xp-number">+20 XP</span>{' '}
          <span style={{ color: STAT_INFO.CON.color }}>Ištvermė</span>
        </span>
      </figcaption>
    </figure>
  )
}
