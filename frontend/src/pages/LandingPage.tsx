import { useState } from 'react'
import { Link, Navigate } from 'react-router'
import { CheckDouble, InfoBox, NotePlus, TrendingUp } from '@nsmr/pixelart-react'
import { useAuth } from '../auth/AuthContext'
import { CharacterPreview } from '../components/CharacterPreview'
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
          <div className="hero__actions">
            <Link to="/registracija" className="btn">
              Sukurti veikėją
            </Link>
            <Link to="/prisijungti" className="btn btn--ghost">
              Jau turiu paskyrą
            </Link>
          </div>
        </div>
        <CharacterPreview />
      </section>

      <section aria-labelledby="kaip-veikia">
        <h2 id="kaip-veikia">Kaip tai veikia</h2>
        <ol className="steps grid grid--3">
          <li className="window">
            <h3 className="step__title">
              <NotePlus size={32} aria-hidden="true" color="var(--sky)" /> 1. Užregistruok žygį
            </h3>
            <p>Prie grupės užduoties pridėk, ką padarei ir kada. Gali įdėti nuorodą į nuotrauką.</p>
          </li>
          <li className="window">
            <h3 className="step__title">
              <CheckDouble size={32} aria-hidden="true" color="var(--leaf)" /> 2. DM patvirtina
            </h3>
            <p>Grupės Dungeon Master peržiūri įrašą, skiria patirtį ir, jei nusipelnei, įkvėpimą.</p>
          </li>
          <li className="window">
            <h3 className="step__title">
              <TrendingUp size={32} aria-hidden="true" color="var(--ember)" /> 3. Veikėjas auga
            </h3>
            <p>Patirtis kelia lygį ir tą charakteristiką, kurią užduotis treniruoja: jėgą, išmintį ar kitą.</p>
          </li>
        </ol>
        <button type="button" className="link-button" onClick={() => setRulesOpen(true)}>
          <InfoBox size={20} aria-hidden="true" /> Kaip skaičiuojama patirtis?
        </button>
      </section>

      <section className="banner" aria-labelledby="iki-sesijos">
        <img className="banner__art pixel" src="/hero.svg" alt="Pikselinė pilis naktį su laužu prie kelio" width={960} height={480} />
        <div className="banner__text">
          <h2 id="iki-sesijos">Kitą sesiją – stipresnis</h2>
          <p>Visa grupė mato, kas pasikeitė nuo praėjusio karto. DM nebereikia skaičiuoti iš atminties.</p>
          <Link to="/registracija" className="btn btn--ghost">
            Pradėti
          </Link>
        </div>
      </section>

      <XpRulesModal open={rulesOpen} onClose={() => setRulesOpen(false)} />
    </div>
  )
}
