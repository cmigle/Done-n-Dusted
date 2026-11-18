import { Link, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { guilds, members } from '../api/endpoints'
import { CharacterSheetView } from '../components/CharacterSheetView'
import { ErrorNote, Loading, Window } from '../components/ui'

export function SheetPage() {
  const params = useParams()
  const guildId = Number(params.guildId)
  const userId = Number(params.userId)
  const guild = useQuery({ queryKey: ['guilds', guildId], queryFn: () => guilds.get(guildId) })
  const sheet = useQuery({ queryKey: ['sheet', guildId, userId], queryFn: () => members.sheet(guildId, userId) })

  return (
    <>
      <nav aria-label="Kelias" className="crumbs">
        <Link to="/taverna">Taverna</Link> / <Link to={`/grupes/${guildId}?skirtukas=nariai`}>{guild.data?.name ?? 'Grupė'}</Link> /{' '}
        <span aria-current="page">Veikėjo kortelė</span>
      </nav>
      <h1 className="visually-hidden">Veikėjo kortelė</h1>
      {sheet.isPending && <Loading />}
      {sheet.isError && (
        <Window title="Kortelė nepasiekiama">
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
