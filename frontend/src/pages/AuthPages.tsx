import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { Login as LoginIcon, UserPlus } from '@nsmr/pixelart-react'
import { ApiError } from '../api/client'
import { auth } from '../api/endpoints'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../components/toast'
import { Field, Window } from '../components/ui'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState<string>()
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(undefined)
    try {
      await login(username.trim(), password)
      const from = (location.state as { from?: string } | null)?.from
      navigate(from ?? '/taverna', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Nepavyko prisijungti')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <Window title="Prisijungti" icon={<LoginIcon size={24} aria-hidden="true" />}>
        <form className="form" onSubmit={submit} noValidate>
          <Field label="Vardas">
            {(p) => (
              <input
                {...p}
                className="input"
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            )}
          </Field>
          <Field label="Slaptažodis" error={error}>
            {(p) => (
              <input
                {...p}
                className="input"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            )}
          </Field>
          <label className="choice">
            <input type="checkbox" checked={showPassword} onChange={(e) => setShowPassword(e.target.checked)} />
            Rodyti slaptažodį
          </label>
          <div className="form__actions">
            <button className="btn" disabled={busy || !username || !password}>
              {busy ? 'Jungiamasi…' : 'Prisijungti'}
            </button>
          </div>
        </form>
        <p className="muted auth__hint">
          Demonstraciniai naudotojai: <code>rokas</code>, <code>ieva</code>, <code>jonas</code>, <code>admin</code> –
          slaptažodis <code>Demo123!</code>. Neturi paskyros? <Link to="/registracija">Registruokis</Link>.
        </p>
      </Window>
    </div>
  )
}

export function RegisterPage() {
  const { login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [repeat, setRepeat] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    const local: Record<string, string> = {}
    if (!/^[A-Za-z0-9_.-]{3,30}$/.test(username)) local.username = '3–30 simbolių: raidės, skaičiai, _ . -'
    if (password.length < 8) local.password = 'Bent 8 simboliai'
    if (password !== repeat) local.repeat = 'Slaptažodžiai nesutampa'
    setErrors(local)
    if (Object.keys(local).length) return

    setBusy(true)
    try {
      await auth.register(username, password)
      await login(username, password)
      toast('success', `Sveikas atvykęs, ${username}!`)
      navigate('/taverna', { replace: true })
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors({
          username: err.errorFor('username') ?? (err.status === 409 ? err.message : ''),
          password: err.errorFor('password') ?? err.errorFor('passwordWithinBcryptLimit') ?? '',
          form: err.details.length || err.status === 409 ? '' : err.message,
        })
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <Window title="Sukurti veikėją" icon={<UserPlus size={24} aria-hidden="true" />}>
        <form className="form" onSubmit={submit} noValidate>
          <Field label="Vardas" hint="3–30 simbolių: raidės, skaičiai, _ . -" error={errors.username || undefined}>
            {(p) => (
              <input
                {...p}
                className="input"
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            )}
          </Field>
          <Field label="Slaptažodis" hint="Bent 8 simboliai" error={errors.password || undefined}>
            {(p) => (
              <input
                {...p}
                className="input"
                type="password"
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            )}
          </Field>
          <Field label="Pakartok slaptažodį" error={errors.repeat || undefined}>
            {(p) => (
              <input
                {...p}
                className="input"
                type="password"
                autoComplete="new-password"
                value={repeat}
                onChange={(e) => setRepeat(e.target.value)}
                required
              />
            )}
          </Field>
          {errors.form && <p className="field__error">{errors.form}</p>}
          <div className="form__actions">
            <button className="btn" disabled={busy}>
              {busy ? 'Kuriama…' : 'Sukurti veikėją'}
            </button>
          </div>
        </form>
        <p className="muted auth__hint">
          Jau turi paskyrą? <Link to="/prisijungti">Prisijunk</Link>.
        </p>
      </Window>
    </div>
  )
}
