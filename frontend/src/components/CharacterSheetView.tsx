import { Coin, Hidden, Zap } from '@nsmr/pixelart-react'
import type { CharacterSheet } from '../api/types'
import { GUILD_ROLE_INFO, STAT_INFO, formatDate, plural, pt } from '../lib/game'
import { StatIcon } from './StatIcon'
import { Bar, PixelAvatar } from './ui'

/** Veikejo kortele: lygis, XP, sesios charakteristikos, ikvepimas, serija. */
export function CharacterSheetView({ sheet }: { sheet: CharacterSheet }) {
  return (
    <article className="sheet" aria-label={`${sheet.username} veikėjo kortelė`}>
      <header className="sheet__head">
        <PixelAvatar name={sheet.username} size={112} />
        <div className="sheet__who">
          <h2 className="sheet__name">{sheet.username}</h2>
          <p className="muted">
            {sheet.guildName
              ? `${sheet.role ? GUILD_ROLE_INFO[sheet.role].name : 'Narys'} grupėje „${sheet.guildName}“`
              : 'Visų grupių suvestinė'}
          </p>
          <div className="sheet__level">
            <span className="level-badge" aria-label={`Lygis ${sheet.level}`}>
              <span aria-hidden="true">lygis</span> {sheet.level}
            </span>
            <div className="sheet__xp">
              <Bar
                value={sheet.xpIntoLevel}
                max={sheet.xpForNextLevel}
                label={`Patirties taškai iki kito lygio: ${sheet.xpIntoLevel} iš ${sheet.xpForNextLevel}`}
                animate
              />
              <span className="muted">
                {sheet.xpIntoLevel} / {pt(sheet.xpForNextLevel)} iki {sheet.level + 1} lygio, iš viso {pt(sheet.totalXp)}
              </span>
            </div>
          </div>
        </div>
      </header>

      <div className="sheet__counters">
        <div className="counter">
          <Coin size={28} aria-hidden="true" color="var(--gold)" />
          <span className="counter__value">{sheet.inspiration}</span>
          <span className="muted">{plural(sheet.inspiration, 'įkvėpimo taškas', 'įkvėpimo taškai', 'įkvėpimo taškų')}</span>
        </div>
        <div className="counter">
          <Zap size={28} aria-hidden="true" color="var(--ember)" />
          <span className="counter__value">{sheet.streakDays}</span>
          <span className="muted">{plural(sheet.streakDays, 'dienos serija', 'dienų serija', 'dienų serija')}</span>
        </div>
        {sheet.view === 'FULL' && sheet.pendingCount != null && (
          <div className="counter">
            <span className="counter__value">{sheet.pendingCount}</span>
            <span className="muted">{plural(sheet.pendingCount, 'įrašas laukia vedėjo', 'įrašai laukia vedėjo', 'įrašų laukia vedėjo')}</span>
          </div>
        )}
      </div>

      <h3>Charakteristikos</h3>
      <ul className="stats">
        {sheet.stats.map((s) => (
          <li key={s.stat} className="stat">
            <span className="stat__icon" style={{ color: STAT_INFO[s.stat].color }}>
              <StatIcon stat={s.stat} size={24} />
            </span>
            <span className="stat__name">{STAT_INFO[s.stat].name}</span>
            <span className="stat__score">{s.score}</span>
            <Bar
              value={s.score - 10}
              max={10}
              color={STAT_INFO[s.stat].color}
              label={`${STAT_INFO[s.stat].name}: ${s.score} (${pt(s.xp)})`}
              animate
            />
          </li>
        ))}
      </ul>

      {sheet.view === 'FULL' && sheet.recentLogs && (
        <>
          <h3>Paskutiniai patvirtinti žygiai</h3>
          {sheet.recentLogs.length === 0 ? (
            <p className="muted">Dar nėra patvirtintų įrašų.</p>
          ) : (
            <ul className="list">
              {sheet.recentLogs.map((l) => (
                <li key={l.logId} className="row row--between">
                  <span>
                    <span style={{ color: STAT_INFO[l.stat].color }}>
                      <StatIcon stat={l.stat} size={18} />
                    </span>{' '}
                    {l.questTitle}
                  </span>
                  <span className="muted">
                    +{pt(l.xpAwarded)}, {formatDate(l.occurredAt)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
      {sheet.view === 'PUBLIC' && (
        <p className="muted sheet__public">
          <Hidden size={18} aria-hidden="true" /> Viešas vaizdas: užduotys ir įrašai matomi tik pačiam žaidėjui ir jo žaidimo vedėjui.
        </p>
      )}
    </article>
  )
}
