import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { LoginPage, RegisterPage } from './AuthPages'
import { ToastProvider } from '../components/toast'

function renderPage(page: React.ReactNode) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <AuthProvider>
        <ToastProvider>
          <MemoryRouter>{page}</MemoryRouter>
        </ToastProvider>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

afterEach(() => vi.unstubAllGlobals())

describe('LoginPage', () => {
  it('parodo serverio klaida prie formos', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ status: 401, message: 'Neteisingas naudotojo vardas arba slaptazodis' }), { status: 401 }),
      ),
    )
    renderPage(<LoginPage />)
    await userEvent.type(screen.getByLabelText('Vardas'), 'jonas')
    await userEvent.type(screen.getByLabelText('Slaptažodis'), 'blogas')
    await userEvent.click(screen.getByRole('button', { name: 'Prisijungti' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Neteisingas naudotojo vardas arba slaptazodis')
  })
})

describe('RegisterPage', () => {
  it('tikrina formą dar prieš siunčiant', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    renderPage(<RegisterPage />)
    await userEvent.type(screen.getByLabelText('Vardas'), 'ab')
    await userEvent.type(screen.getByLabelText('Slaptažodis'), 'Demo123!')
    await userEvent.type(screen.getByLabelText('Pakartok slaptažodį'), 'Kitas123!')
    await userEvent.click(screen.getByRole('button', { name: 'Sukurti veikėją' }))
    expect(screen.getByText('Slaptažodžiai nesutampa')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
