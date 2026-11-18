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
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      {
        element: <RequireAuth />,
        children: [
          { path: '/tavern', element: <TavernPage /> },
          { path: '/guilds/:guildId', element: <GuildPage /> },
          { path: '/guilds/:guildId/quests/:questId', element: <QuestPage /> },
          { path: '/guilds/:guildId/members/:userId', element: <SheetPage /> },
          { path: '/friends', element: <FriendsPage /> },
        ],
      },
      {
        element: <RequireAuth admin />,
        children: [{ path: '/admin', element: <AdminUsersPage /> }],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
