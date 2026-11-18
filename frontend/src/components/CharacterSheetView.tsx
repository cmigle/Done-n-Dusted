import { Coin, Hidden, Zap } from '@nsmr/pixelart-react'
import type { CharacterSheet } from '../api/types'
import { GUILD_ROLE_INFO, STAT_INFO, formatDate, plural, xp } from '../lib/game'
import { StatIcon } from './StatIcon'
import { Bar, PixelAvatar } from './ui'

/** Character sheet: level, XP, the six stats, inspiration and streak. */
export function CharacterSheetView({ sheet }: { sheet: CharacterSheet }) {
  return (
    <article className="sheet" aria-label={`${sheet.username}'s character sheet`}>
      <header className="sheet__head">
        <PixelAvatar name={sheet.username} size={112} />
        <div className="sheet__who">
          <h2 className="sheet__name">{sheet.username}</h2>
          <p className="muted">
            {sheet.guildName
              ? `${sheet.role ? GUILD_ROLE_INFO[sheet.role].name : 'Member'} in “${sheet.guildName}”`
              : 'Across all guilds'}
          </p>
          <div className="sheet__level">
            <span className="level-badge" aria-label={`Level ${sheet.level}`}>
              <span aria-hidden="true">lv</span> {sheet.level}
            </span>
            <div className="sheet__xp">
              <Bar
                value={sheet.xpIntoLevel}
                max={sheet.xpForNextLevel}
                label={`Experience to the next level: ${sheet.xpIntoLevel} of ${sheet.xpForNextLevel}`}
                animate
              />
              <span className="muted">
                {sheet.xpIntoLevel} / {xp(sheet.xpForNextLevel)} to level {sheet.level + 1}, {xp(sheet.totalXp)} in total
              </span>
            </div>
          </div>
        </div>
      </header>

      <div className="sheet__counters">
        <div className="counter">
          <Coin size={28} aria-hidden="true" color="var(--gold)" />
          <span className="counter__value">{sheet.inspiration}</span>
          <span className="muted">inspiration</span>
        </div>
        <div className="counter">
          <Zap size={28} aria-hidden="true" color="var(--ember)" />
          <span className="counter__value">{sheet.streakDays}</span>
          <span className="muted">day streak</span>
        </div>
        {sheet.view === 'FULL' && sheet.pendingCount != null && (
          <div className="counter">
            <span className="counter__value">{sheet.pendingCount}</span>
            <span className="muted">{plural(sheet.pendingCount, 'entry awaiting GM', 'entries awaiting GM')}</span>
          </div>
        )}
      </div>

      <h3>Stats</h3>
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
              label={`${STAT_INFO[s.stat].name}: ${s.score} (${xp(s.xp)})`}
              animate
            />
          </li>
        ))}
      </ul>

      {sheet.view === 'FULL' && sheet.recentLogs && (
        <>
          <h3>Recently approved entries</h3>
          {sheet.recentLogs.length === 0 ? (
            <p className="muted">No approved entries yet.</p>
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
                    +{xp(l.xpAwarded)}, {formatDate(l.occurredAt)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
      {sheet.view === 'PUBLIC' && (
        <p className="muted sheet__public">
          <Hidden size={18} aria-hidden="true" /> Public view: quests and entries are visible only to the player and their game master.
        </p>
      )}
    </article>
  )
}
