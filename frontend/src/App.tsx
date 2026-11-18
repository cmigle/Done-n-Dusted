import { createBrowserRouter } from 'react-router'
import { Layout, RequireAuth } from './components/Layout'
import { AdminUsersPage } from './pages/AdminUsersPage'
import { LoginPage, RegisterPage } from './pages/AuthPages'
import { FriendsPage } from './pages/FriendsPage'
import { GuildPage } from './pages/GuildPage'
import { LandingPage } from './pages/LandingPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { QuestPage } from './pages/QuestPage'
import { SheetPage } from './pages/SheetPage'
import { TavernPage } from './pages/TavernPage'

export const router = createBrowserRouter([
  {
    element: <Layout />,
    children: [
      { path: '/', element: <LandingPage /> },
      { path: '/prisijungti', element: <LoginPage /> },
      { path: '/registracija', element: <RegisterPage /> },
      {
        element: <RequireAuth />,
        children: [
          { path: '/taverna', element: <TavernPage /> },
          { path: '/grupes/:guildId', element: <GuildPage /> },
          { path: '/grupes/:guildId/uzduotys/:questId', element: <QuestPage /> },
          { path: '/grupes/:guildId/nariai/:userId', element: <SheetPage /> },
          { path: '/draugai', element: <FriendsPage /> },
        ],
      },
      {
        element: <RequireAuth admin />,
        children: [{ path: '/administravimas', element: <AdminUsersPage /> }],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
