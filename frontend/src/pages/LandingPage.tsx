import { useState } from 'react'
import { Link, Navigate } from 'react-router'
import { CheckDouble, InfoBox, NotePlus, TrendingUp } from '@nsmr/pixelart-react'
import { useAuth } from '../auth/AuthContext'
import { XpRulesModal } from '../components/XpRulesModal'

export function LandingPage() {
  const { status } = useAuth()
  const [rulesOpen, setRulesOpen] = useState(false)

  if (status === 'authenticated') return <Navigate to="/taverna" replace />

  return (
    <div className="stack">
      <section className="hero">
        <div className="hero__text">
          <h1>Tavo veikėjas auga tarp sesijų</h1>
          <p>
            Bėgi, skaitai, susitinki su draugais – užregistruok tai savo grupėje. Kai Dungeon Master patvirtina,
            veikėjas gauna patirties ir kyla charakteristikos.
          </p>
          <div className="row">
            <Link to="/registracija" className="btn">
              Sukurti veikėją
            </Link>
            <Link to="/prisijungti" className="btn btn--ghost">
              Jau turiu paskyrą
            </Link>
          </div>
        </div>
        <img className="hero__art pixel" src="/hero.svg" alt="Pikselinė pilis naktį su laužu prie kelio" width={960} height={480} />
      </section>

      <section aria-labelledby="kaip-veikia">
        <div className="row row--between">
          <h2 id="kaip-veikia">Kaip tai veikia</h2>
          <button type="button" className="btn btn--secondary btn--small" onClick={() => setRulesOpen(true)}>
            <InfoBox size={18} aria-hidden="true" /> Kaip skaičiuojama patirtis?
          </button>
        </div>
        <ol className="steps grid grid--3">
          <li className="window">
            <h3 className="window__title">
              <NotePlus size={24} aria-hidden="true" /> 1. Užregistruok žygį
            </h3>
            <p>Prie grupės užduoties pridėk, ką padarei ir kada. Gali įdėti nuorodą į nuotrauką.</p>
          </li>
          <li className="window">
            <h3 className="window__title">
              <CheckDouble size={24} aria-hidden="true" /> 2. DM patvirtina
            </h3>
            <p>Grupės Dungeon Master peržiūri įrašą, skiria patirtį ir, jei nusipelnei, įkvėpimą.</p>
          </li>
          <li className="window">
            <h3 className="window__title">
              <TrendingUp size={24} aria-hidden="true" /> 3. Veikėjas auga
            </h3>
            <p>Patirtis kelia lygį ir tą charakteristiką, kurią užduotis treniruoja: jėgą, išmintį ar kitą.</p>
          </li>
        </ol>
      </section>

      <XpRulesModal open={rulesOpen} onClose={() => setRulesOpen(false)} />
    </div>
  )
}
