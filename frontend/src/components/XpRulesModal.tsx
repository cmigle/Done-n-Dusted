import { DIFFICULTIES, DIFFICULTY_INFO, STATS, STAT_INFO, pt } from '../lib/game'
import { Modal } from './ui'

/** Modalinis langas su zaidimo taisyklemis - tos pacios formules kaip backend CharacterSheet. */
export function XpRulesModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  return (
    <Modal open={open} onClose={onClose} title="Kaip skaičiuojama patirtis">
      <p>
        Patirties taškus (PT) duoda tik žaidimo vedėjo (angl. Dungeon Master) patvirtinti įrašai. Pagrindinė vertė
        priklauso nuo užduoties sunkumo:
      </p>
      <div className="table-wrap">
        <table className="table">
          <caption className="visually-hidden">Patirties taškai pagal sunkumą</caption>
          <thead>
            <tr>
              <th scope="col">Sunkumas</th>
              <th scope="col">Patirtis</th>
            </tr>
          </thead>
          <tbody>
            {DIFFICULTIES.map((d) => (
              <tr key={d}>
                <td>{DIFFICULTY_INFO[d].name}</td>
                <td>{pt(DIFFICULTY_INFO[d].xp)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ul>
        <li>Kas {pt(100)} – naujas lygis.</li>
        <li>Pradinė charakteristikos vertė – 10; ji pakyla 1 tašku už kas {pt(50)} šiai charakteristikai (daugiausia 20).</li>
        <li>
          Žaidimo vedėjas, tvirtindamas įrašą, gali skirti kitokį patirties taškų kiekį ir įkvėpimo taškų už neįprastą
          žygį.
        </li>
        <li>Serija – kiek dienų iš eilės turi patvirtintą įrašą.</li>
      </ul>
      <p className="muted">
        Charakteristikos: {STATS.map((s) => `${STAT_INFO[s].name} (${STAT_INFO[s].abbr})`).join(', ')}.
      </p>
    </Modal>
  )
}
