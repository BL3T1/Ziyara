import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { LanguageProvider } from '../../context/LanguageContext'
import { LandingContactPage } from './LandingContactPage'
import * as api from '../../services/api'

vi.mock('../../services/api', async () => {
  const actual = await vi.importActual<typeof import('../../services/api')>('../../services/api')
  return {
    ...actual,
    publicAPI: {
      submitContact: vi.fn(),
    },
  }
})

vi.mock('./useLandingLiveData', () => ({
  useLandingLiveData: () => ({ totalServices: 0, totalCities: 0 }),
}))

vi.mock('./useLandingPageContent', () => ({
  useLandingPageContent: () => ({
    readString: (_key: string, fallback: string) => fallback,
  }),
}))

function renderContact() {
  return render(
    <LanguageProvider>
      <LandingContactPage />
    </LanguageProvider>,
  )
}

describe('LandingContactPage', () => {
  beforeEach(() => {
    vi.mocked(api.publicAPI.submitContact).mockResolvedValue({ data: null })
  })

  it('submits valid form via publicAPI', async () => {
    renderContact()

    fireEvent.change(screen.getByPlaceholderText(/Full name/i), { target: { value: 'Jane Doe' } })
    fireEvent.change(screen.getByPlaceholderText(/Work email/i), { target: { value: 'jane@acme.com' } })
    fireEvent.change(screen.getByPlaceholderText(/^Company$/i), { target: { value: 'Acme Ltd' } })
    fireEvent.change(screen.getByPlaceholderText(/^Message$/i), {
      target: { value: 'This is a long enough message for validation.' },
    })

    fireEvent.click(screen.getByRole('button', { name: /Send request/i }))

    await waitFor(() => {
      expect(api.publicAPI.submitContact).toHaveBeenCalledWith({
        name: 'Jane Doe',
        email: 'jane@acme.com',
        company: 'Acme Ltd',
        message: 'This is a long enough message for validation.',
      })
    })
    expect(await screen.findByText(/Thanks, your request has been received/i)).toBeInTheDocument()
  })
})
