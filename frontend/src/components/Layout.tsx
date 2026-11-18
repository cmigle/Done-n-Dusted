import { useState } from 'react'
import { Link, NavLink, Navigate, Outlet, useLocation } from 'react-router'
import { Close, Home, Login, Logout, Menu, Shield, UserPlus, Users } from '@nsmr/pixelart-react'
import { API_DOCS_URL } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { PixelAvatar, Spinner } from './ui'

function Header() {
  const { status, user, isAdmin, logout } = useAuth()
  const location = useLocation()
  // Meniu atidarytas tik tame puslapyje, kuriame ji atidare: pasirinkus punkta - uzsidaro savaime.
  const [openOn, setOpenOn] = useState<string | null>(null)
  const open = openOn === location.pathname

  const link = ({ isActive }: { isActive: boolean }) => `nav__link${isActive ? ' active' : ''}`

  return (
    <header className="site-header">
      <div className="container site-header__bar">
        <Link to={user ? '/taverna' : '/'} className="brand">
          <img src="/logo.svg" alt="" width={40} height={40} />
          Done n Dusted
        </Link>

        <button
          type="button"
          className="btn btn--ghost btn--icon menu-toggle"
          aria-expanded={open}
          aria-controls="main-nav"
          aria-label={open ? 'Uždaryti meniu' : 'Atidaryti meniu'}
          onClick={() => setOpenOn(open ? null : location.pathname)}
        >
          {open ? <Close size={24} aria-hidden="true" /> : <Menu size={24} aria-hidden="true" />}
        </button>

        <nav id="main-nav" className={`nav${open ? ' is-open' : ''}`} aria-label="Pagrindinis meniu">
          <ul className="nav__list">
            {status === 'authenticated' ? (
              <>
                <li>
                  <NavLink to="/taverna" className={link}>
                    <Home size={20} aria-hidden="true" /> Taverna
                  </NavLink>
                </li>
                <li>
                  <NavLink to="/draugai" className={link}>
                    <Users size={20} aria-hidden="true" /> Draugai
                  </NavLink>
                </li>
                {isAdmin && (
                  <li>
                    <NavLink to="/administravimas" className={link}>
                      <Shield size={20} aria-hidden="true" /> Administravimas
                    </NavLink>
                  </li>
                )}
                <li>
                  <button type="button" className="nav__link" onClick={() => void logout()}>
                    <Logout size={20} aria-hidden="true" /> Atsijungti
                  </button>
                </li>
              </>
            ) : (
              <>
                <li>
                  <NavLink to="/prisijungti" className={link}>
                    <Login size={20} aria-hidden="true" /> Prisijungti
                  </NavLink>
                </li>
                <li>
                  <NavLink to="/registracija" className={link}>
                    <UserPlus size={20} aria-hidden="true" /> Registruotis
                  </NavLink>
                </li>
              </>
            )}
          </ul>
        </nav>

        {user && (
          <div className="header-user" aria-label="Prisijungęs naudotojas">
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
      <div className="container">
        <div className="site-footer__grid">
          <div>
            <h2>Done n Dusted</h2>
            <p>Realūs žygiai tarp sesijų – patvirtina tavo grupės Dungeon Master.</p>
          </div>
          <div>
            <h2>Projektas</h2>
            <ul>
              <li>
                <a href="https://github.com/cmigle/Done-n-Dusted" target="_blank" rel="noreferrer">
                  GitHub
                </a>
              </li>
              <li>
                <a href={API_DOCS_URL}>
                  API dokumentacija
                </a>
              </li>
            </ul>
          </div>
          <div>
            <h2>Kursas</h2>
            <p>KTU T120B165 Saityno taikomųjų programų projektavimas</p>
          </div>
        </div>
        <p className="site-footer__legal">© {YEAR} Miglė Cirtautaitė</p>
      </div>
    </footer>
  )
}

export function Layout() {
  return (
    <>
      <a className="skip-link" href="#turinys">
        Pereiti prie turinio
      </a>
      <Header />
      <main id="turinys" className="site-main" tabIndex={-1}>
        <div className="container">
          <Outlet />
        </div>
      </main>
      <Footer />
    </>
  )
}

/** Tik prisijungusiems; kol atkuriama sesija - laukiam, kad nebutu "mirksejimo" i prisijungima. */
export function RequireAuth({ admin = false }: { admin?: boolean }) {
  const { status, isAdmin, signedOut } = useAuth()
  const location = useLocation()
  if (status === 'loading') {
    return (
      <div className="empty">
        <Spinner label="Atkuriama sesija" />
      </div>
    )
  }
  // Pasibaigus sesijai - i prisijungima ir atgal i ta pati puslapi; atsijungus pačiam - i pradzia.
  if (status === 'anonymous') {
    return signedOut ? <Navigate to="/" replace /> : <Navigate to="/prisijungti" replace state={{ from: location.pathname }} />
  }
  if (admin && !isAdmin) return <Navigate to="/taverna" replace />
  return <Outlet />
}
