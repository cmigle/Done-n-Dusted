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
  useTitle('Your character grows between sessions')

  if (status === 'authenticated') return <Navigate to="/tavern" replace />

  return (
    <div className="stack">
      <section className="hero">
        <div className="hero__text">
          <h1>Your character grows between sessions</h1>
          <p>
            Go for a run, read a book, meet your friends – log it with your guild. Once the game master approves it,
            your character earns experience points and its stats go up.
          </p>
          <div className="hero__actions">
            <Link to="/register" className="btn">
              Create a character
            </Link>
            <Link to="/login" className="btn btn--ghost">
              I have an account
            </Link>
          </div>
        </div>
        <CharacterPreview />
      </section>

      <section aria-labelledby="how-it-works">
        <h2 id="how-it-works">How it works</h2>
        <ol className="steps grid grid--3">
          <li className="window">
            <h3 className="step__title">
              <NotePlus size={32} aria-hidden="true" color="var(--sky)" /> 1. Log what you did
            </h3>
            <p>Add an entry to one of your guild's quests: what you did and when. You can attach a link to a photo.</p>
          </li>
          <li className="window">
            <h3 className="step__title">
              <CheckDouble size={32} aria-hidden="true" color="var(--leaf)" /> 2. The game master approves
            </h3>
            <p>The game master (GM) reviews the entry, awards XP and, if you earned it, inspiration.</p>
          </li>
          <li className="window">
            <h3 className="step__title">
              <TrendingUp size={32} aria-hidden="true" color="var(--ember)" /> 3. Your character grows
            </h3>
            <p>XP raises your level and the stat the quest trains: strength, wisdom or any of the others.</p>
          </li>
        </ol>
        <button type="button" className="link-button" onClick={() => setRulesOpen(true)}>
          <InfoBox size={20} aria-hidden="true" /> How does XP work?
        </button>
      </section>

      <section className="banner" aria-labelledby="next-session">
        <img className="banner__art pixel" src="/hero.svg" alt="A pixel castle at night with a campfire by the road" width={960} height={480} />
        <div className="banner__text">
          <h2 id="next-session">Stronger at every session</h2>
          <p>The whole guild sees what changed since last time. The game master no longer has to keep score from memory.</p>
          <Link to="/register" className="btn btn--ghost">
            Get started
          </Link>
        </div>
      </section>

      <XpRulesModal open={rulesOpen} onClose={() => setRulesOpen(false)} />
    </div>
  )
}
