import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { Bar, Field } from './ui'

describe('Bar', () => {
  it('pranesa progresa ekrano skaitytuvams ir neislipa uz 100 %', () => {
    render(<Bar value={150} max={100} label="Patirtis" />)
    const bar = screen.getByRole('progressbar', { name: 'Patirtis' })
    expect(bar).toHaveAttribute('aria-valuenow', '150')
    expect(bar).toHaveAttribute('aria-valuemax', '100')
    expect(bar.style.getPropertyValue('--value')).toBe('100%')
  })
})

describe('Field', () => {
  it('susieja etikete, uzuomina ir klaida su lauku', async () => {
    render(
      <Field label="Pavadinimas" hint="3–60 simboliu" error="Per trumpas">
        {(p) => <input {...p} />}
      </Field>,
    )
    const input = screen.getByLabelText('Pavadinimas')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAccessibleDescription('3–60 simboliu Per trumpas')
    await userEvent.type(input, 'abc')
    expect(input).toHaveValue('abc')
  })
})
