import { useState } from 'react'
import { Link, NavLink, Navigate, Outlet, useLocation } from 'react-router'
import { BookOpen, Close, Github, Home, Login, Logout, Menu, Shield, UserPlus, Users } from '@nsmr/pixelart-react'
import { API_DOCS_URL } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { GrassStrip } from './Scene'
import { ThemeToggle } from './ThemeToggle'
import { PixelAvatar, Spinner } from './ui'

function Header() {
  const { status, user, isAdmin, logout } = useAuth()
  const location = useLocation()
  // The menu stays open only on the page where it was opened, so picking a link closes it.
  const [openOn, setOpenOn] = useState<string | null>(null)
  const open = openOn === location.pathname

  const link = ({ isActive }: { isActive: boolean }) => `nav__link${isActive ? ' active' : ''}`

  return (
    <header className="site-header">
      {open && <div className="nav-backdrop" aria-hidden="true" onClick={() => setOpenOn(null)} />}
      <div className="container site-header__bar">
        <Link to={user ? '/tavern' : '/'} className="brand">
          <img src="/logo.svg" alt="" width={40} height={40} />
          Done n Dusted
        </Link>

        <div className="header-tools">
          <ThemeToggle />
          <button
            type="button"
            className="btn btn--ghost btn--icon menu-toggle"
            aria-expanded={open}
            aria-controls="main-nav"
            aria-label={open ? 'Close menu' : 'Open menu'}
            onClick={() => setOpenOn(open ? null : location.pathname)}
          >
            {open ? <Close size={24} aria-hidden="true" /> : <Menu size={24} aria-hidden="true" />}
          </button>
        </div>

        <nav id="main-nav" className={`nav${open ? ' is-open' : ''}`} aria-label="Main menu">
          <ul className="nav__list">
            {status === 'authenticated' ? (
              <>
                <li>
                  <NavLink to="/tavern" className={link}>
                    <Home size={20} aria-hidden="true" /> Tavern
                  </NavLink>
                </li>
                <li>
                  <NavLink to="/friends" className={link}>
                    <Users size={20} aria-hidden="true" /> Friends
                  </NavLink>
                </li>
                {isAdmin && (
                  <li>
                    <NavLink to="/admin" className={link}>
                      <Shield size={20} aria-hidden="true" /> Admin
                    </NavLink>
                  </li>
                )}
                <li>
                  <button type="button" className="nav__link" onClick={() => void logout()}>
                    <Logout size={20} aria-hidden="true" /> Log out
                  </button>
                </li>
              </>
            ) : (
              <>
                <li>
                  <NavLink to="/login" className={link}>
                    <Login size={20} aria-hidden="true" /> Log in
                  </NavLink>
                </li>
                <li>
                  <NavLink to="/register" className={link}>
                    <UserPlus size={20} aria-hidden="true" /> Sign up
                  </NavLink>
                </li>
              </>
            )}
          </ul>
        </nav>

        {user && (
          <div className="header-user" aria-label="Signed in as">
            <PixelAvatar name={user.username} size={36} />
            <span>{user.username}</span>
          </div>
        )}
      </div>
    </header>
  )
}

const YEAR = new Date().getFullYear()

function Footer() {
  return (
    <footer className="site-footer">
      <GrassStrip className="site-footer__grass" />
      <div className="container site-footer__bar">
        <p className="site-footer__legal">© {YEAR} Miglė Cirtautaitė</p>
        <ul className="site-footer__links">
          <li>
            <a href="https://github.com/cmigle/Done-n-Dusted" target="_blank" rel="noreferrer">
              <Github size={18} aria-hidden="true" /> GitHub
            </a>
          </li>
          <li>
            <a href={API_DOCS_URL}>
              <BookOpen size={18} aria-hidden="true" /> API docs
            </a>
          </li>
        </ul>
      </div>
    </footer>
  )
}

export function Layout() {
  return (
    <>
      <a className="skip-link" href="#content">
        Skip to content
      </a>
      <Header />
      <main id="content" className="site-main" tabIndex={-1}>
        <div className="container">
          <Outlet />
        </div>
      </main>
      <Footer />
    </>
  )
}

/** Signed-in users only. While the session is being restored we wait, so the page never flashes to the login form. */
export function RequireAuth({ admin = false }: { admin?: boolean }) {
  const { status, isAdmin, signedOut } = useAuth()
  const location = useLocation()
  if (status === 'loading') {
    return (
      <div className="empty">
        <Spinner label="Restoring your session" />
      </div>
    )
  }
  // Expired session: to the login form and back to the same page afterwards. Logged out on purpose: to the home page.
  if (status === 'anonymous') {
    return signedOut ? <Navigate to="/" replace /> : <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (admin && !isAdmin) return <Navigate to="/tavern" replace />
  return <Outlet />
}
