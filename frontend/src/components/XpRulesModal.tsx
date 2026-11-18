import { DIFFICULTIES, DIFFICULTY_INFO, STATS, STAT_INFO } from '../lib/game'
import { Modal } from './ui'

/** Modalinis langas su zaidimo taisyklemis - tos pacios formules kaip backend CharacterSheet. */
export function XpRulesModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  return (
    <Modal open={open} onClose={onClose} title="Kaip skaičiuojama patirtis">
      <p>Patirtį (XP) duoda tik Dungeon Master patvirtinti įrašai. Bazinė vertė priklauso nuo užduoties sunkumo:</p>
      <div className="table-wrap">
        <table className="table">
          <caption className="visually-hidden">Patirtis pagal sunkumą</caption>
          <thead>
            <tr>
              <th scope="col">Sunkumas</th>
              <th scope="col">XP</th>
            </tr>
          </thead>
          <tbody>
            {DIFFICULTIES.map((d) => (
              <tr key={d}>
                <td>{DIFFICULTY_INFO[d].name}</td>
                <td>{DIFFICULTY_INFO[d].xp}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ul>
        <li>Kas 100 XP – naujas lygis.</li>
        <li>Charakteristika prasideda nuo 10 ir pakyla 1 tašku už kas 50 jos XP (daugiausia 20).</li>
        <li>DM tvirtindamas gali skirti kitokį XP ir įkvėpimo taškų už neįprastą žygį.</li>
        <li>Serija – kiek dienų iš eilės turi patvirtintą įrašą.</li>
      </ul>
      <p className="muted">
        Charakteristikos: {STATS.map((s) => `${STAT_INFO[s].name} (${s})`).join(', ')}.
      </p>
    </Modal>
  )
}
