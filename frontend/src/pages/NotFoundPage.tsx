import { Link } from 'react-router'
import { Window } from '../components/ui'
import { useTitle } from '../lib/useTitle'

export function NotFoundPage() {
  useTitle('Page not found')
  return (
    <div className="auth">
      <Window>
        <div className="empty">
          <p className="game-over" aria-hidden="true">
            404
          </p>
          <h1>This road leads nowhere</h1>
          <p>There is no such page. Maybe the map is out of date?</p>
          <Link to="/" className="btn">
            Back to the start
          </Link>
        </div>
      </Window>
    </div>
  )
}
