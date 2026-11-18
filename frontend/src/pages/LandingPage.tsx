import { useState } from 'react'
import { Link, Navigate } from 'react-router'
import { CheckDouble, InfoBox, NotePlus, TrendingUp } from '@nsmr/pixelart-react'
import { useAuth } from '../auth/AuthContext'
import { CharacterPreview } from '../components/CharacterPreview'
import { XpRulesModal } from '../components/XpRulesModal'
import { useTitle } from '../lib/useTitle'

export function LandingPage() {
  const { status } = useAuth()
  const [rulesOpen, setRulesOpen] = useState(false)
  useTitle('Veikėjas auga tarp žaidimų')

  if (status === 'authenticated') return <Navigate to="/taverna" replace />

  return (
    <div className="stack">
      <section className="hero">
        <div className="hero__text">
          <h1>Tavo veikėjas auga tarp žaidimų</h1>
          <p>
            Bėgi, skaitai, susitinki su draugais – užregistruok tai savo grupėje. Kai žaidimo vedėjas (angl. Dungeon
            Master) patvirtina, veikėjas gauna patirties taškų, o jo charakteristikos kyla.
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
              <CheckDouble size={32} aria-hidden="true" color="var(--leaf)" /> 2. Vedėjas patvirtina
            </h3>
            <p>Žaidimo vedėjas (ŽV) peržiūri įrašą, skiria patirties taškų ir, jei nusipelnei, įkvėpimo taškų.</p>
          </li>
          <li className="window">
            <h3 className="step__title">
              <TrendingUp size={32} aria-hidden="true" color="var(--ember)" /> 3. Veikėjas auga
            </h3>
            <p>Patirtis kelia lygį ir tą charakteristiką, kurią užduotis lavina: jėgą, išmintį ar kurią nors kitą.</p>
          </li>
        </ol>
        <button type="button" className="link-button" onClick={() => setRulesOpen(true)}>
          <InfoBox size={20} aria-hidden="true" /> Kaip skaičiuojama patirtis?
        </button>
      </section>

      <section className="banner" aria-labelledby="kitas-zaidimas">
        <img className="banner__art pixel" src="/hero.svg" alt="Pikselinė pilis naktį su laužu prie kelio" width={960} height={480} />
        <div className="banner__text">
          <h2 id="kitas-zaidimas">Į kitą žaidimą – stipresnis</h2>
          <p>Visa grupė mato, kas pasikeitė nuo praėjusio karto. Žaidimo vedėjui nebereikia skaičiuoti iš atminties.</p>
          <Link to="/registracija" className="btn btn--ghost">
            Pradėti
          </Link>
        </div>
      </section>

      <XpRulesModal open={rulesOpen} onClose={() => setRulesOpen(false)} />
    </div>
  )
}
