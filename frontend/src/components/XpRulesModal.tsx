import { DIFFICULTIES, DIFFICULTY_INFO, STATS, STAT_INFO, xp } from '../lib/game'
import { Modal } from './ui'

/** The game rules, using the same formulas as the backend CharacterSheet. */
export function XpRulesModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  return (
    <Modal open={open} onClose={onClose} title="How XP works">
      <p>
        Only log entries approved by the game master earn experience points (XP). The base amount depends on the
        quest difficulty:
      </p>
      <div className="table-wrap">
        <table className="table">
          <caption className="visually-hidden">XP by difficulty</caption>
          <thead>
            <tr>
              <th scope="col">Difficulty</th>
              <th scope="col">XP</th>
            </tr>
          </thead>
          <tbody>
            {DIFFICULTIES.map((d) => (
              <tr key={d}>
                <td>{DIFFICULTY_INFO[d].name}</td>
                <td>{xp(DIFFICULTY_INFO[d].xp)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ul>
        <li>Every {xp(100)} is a new level.</li>
        <li>Each stat starts at 10 and rises by 1 for every {xp(50)} earned in it (up to 20).</li>
        <li>When approving an entry, the game master can award a different amount of XP and add inspiration for an unusual feat.</li>
        <li>Streak – how many days in a row you have an approved entry.</li>
      </ul>
      <p className="muted">Stats: {STATS.map((s) => `${STAT_INFO[s].name} (${STAT_INFO[s].abbr})`).join(', ')}.</p>
    </Modal>
  )
}
