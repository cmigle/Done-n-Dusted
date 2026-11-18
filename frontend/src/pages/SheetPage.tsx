import { Link, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { guilds, members } from '../api/endpoints'
import { CharacterSheetView } from '../components/CharacterSheetView'
import { ErrorNote, Loading, Window } from '../components/ui'
import { useTitle } from '../lib/useTitle'

export function SheetPage() {
  const params = useParams()
  const guildId = Number(params.guildId)
  const userId = Number(params.userId)
  const guild = useQuery({ queryKey: ['guilds', guildId], queryFn: () => guilds.get(guildId) })
  const sheet = useQuery({ queryKey: ['sheet', guildId, userId], queryFn: () => members.sheet(guildId, userId) })
  useTitle(sheet.data ? `${sheet.data.username}'s character sheet` : 'Character sheet')

  return (
    <>
      <nav aria-label="Breadcrumb" className="crumbs">
        <Link to="/tavern">Tavern</Link> / <Link to={`/guilds/${guildId}?tab=members`}>{guild.data?.name ?? 'Guild'}</Link> /{' '}
        <span aria-current="page">Character sheet</span>
      </nav>
      <h1 className="visually-hidden">Character sheet</h1>
      {sheet.isPending && <Loading />}
      {sheet.isError && (
        <Window title="Sheet unavailable">
          <ErrorNote error={sheet.error} />
        </Window>
      )}
      {sheet.data && (
        <Window className="sheet-window">
          <CharacterSheetView sheet={sheet.data} />
        </Window>
      )}
    </>
  )
}
