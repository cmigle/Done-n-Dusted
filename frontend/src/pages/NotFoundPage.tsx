import { Link } from 'react-router'
import { Window } from '../components/ui'
import { useTitle } from '../lib/useTitle'

export function NotFoundPage() {
  useTitle('Puslapis nerastas')
  return (
    <div className="auth">
      <Window>
        <div className="empty">
          <p className="game-over" aria-hidden="true">
            404
          </p>
          <h1>Šis kelias veda niekur</h1>
          <p>Tokio puslapio nėra. Gal žemėlapis pasenęs?</p>
          <Link to="/" className="btn">
            Grįžti į pradžią
          </Link>
        </div>
      </Window>
    </div>
  )
}
