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
  it('shows the server error next to the form', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ status: 401, message: 'Invalid username or password' }), { status: 401 }),
      ),
    )
    renderPage(<LoginPage />)
    await userEvent.type(screen.getByLabelText('Username'), 'jonas')
    await userEvent.type(screen.getByLabelText('Password'), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password')
  })
})

describe('RegisterPage', () => {
  it('validates the form before sending it', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    renderPage(<RegisterPage />)
    await userEvent.type(screen.getByLabelText('Username'), 'ab')
    await userEvent.type(screen.getByLabelText('Password'), 'Demo123!')
    await userEvent.type(screen.getByLabelText('Repeat password'), 'Other123!')
    await userEvent.click(screen.getByRole('button', { name: 'Create character' }))
    expect(screen.getByText('Passwords do not match')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
