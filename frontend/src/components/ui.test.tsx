import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { Bar, Field } from './ui'

describe('Bar', () => {
  it('reports progress to screen readers and never fills past 100%', () => {
    render(<Bar value={150} max={100} label="Experience" />)
    const bar = screen.getByRole('progressbar', { name: 'Experience' })
    expect(bar).toHaveAttribute('aria-valuenow', '150')
    expect(bar).toHaveAttribute('aria-valuemax', '100')
    expect(bar.style.getPropertyValue('--value')).toBe('100%')
  })
})

describe('Field', () => {
  it('links the label, hint and error to the input', async () => {
    render(
      <Field label="Name" hint="3–60 characters" error="Too short">
        {(p) => <input {...p} />}
      </Field>,
    )
    const input = screen.getByLabelText('Name')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAccessibleDescription('3–60 characters Too short')
    await userEvent.type(input, 'abc')
    expect(input).toHaveValue('abc')
  })
})
