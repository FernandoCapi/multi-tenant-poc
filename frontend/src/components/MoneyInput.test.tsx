import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { MoneyInput } from './MoneyInput'

function Harness() {
  const [cents, setCents] = useState<number | null>(null)
  return (
    <label>
      Valor
      <MoneyInput cents={cents} onChange={setCents} />
      <output>{cents === null ? 'vazio' : cents}</output>
    </label>
  )
}

// Intl separates "R$" from the number with a non-breaking space
const brl = (text: string) => text.replace(' ', ' ')

describe('MoneyInput', () => {
  it('formats as BRL while digits are typed, filling from the right', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    const input = screen.getByLabelText('Valor')

    await user.type(input, '2')
    expect(input).toHaveValue(brl('R$ 0,02'))
    await user.type(input, '5')
    expect(input).toHaveValue(brl('R$ 0,25'))
    await user.type(input, '50')
    expect(input).toHaveValue(brl('R$ 25,50'))
    await user.type(input, '00')
    expect(input).toHaveValue(brl('R$ 2.550,00'))
    expect(screen.getByRole('status')).toHaveTextContent('255000')
  })

  it('ignores anything that is not a digit', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    const input = screen.getByLabelText('Valor')

    await user.type(input, 'a1-2,3.4')
    expect(input).toHaveValue(brl('R$ 12,34'))
  })

  it('removes the last digit on backspace and becomes empty again', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    const input = screen.getByLabelText('Valor')

    await user.type(input, '2550')
    await user.type(input, '{Backspace}')
    expect(input).toHaveValue(brl('R$ 2,55'))
    await user.type(input, '{Backspace}{Backspace}{Backspace}')
    expect(input).toHaveValue('')
    expect(screen.getByRole('status')).toHaveTextContent('vazio')
  })

  it('accepts a pasted value already formatted in BRL', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    const input = screen.getByLabelText('Valor')

    await user.click(input)
    await user.paste('R$ 1.234,56')
    expect(input).toHaveValue(brl('R$ 1.234,56'))
  })
})
