import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { LanguageProvider } from '../../context/LanguageContext'
import { PortalSupportPage } from './PortalSupportPage'
import * as api from '../../services/api'

function renderSupport() {
  return render(
    <BrowserRouter>
      <LanguageProvider>
        <PortalSupportPage />
      </LanguageProvider>
    </BrowserRouter>,
  )
}

describe('PortalSupportPage', () => {
  beforeEach(() => {
    vi.spyOn(api.portalSupportAPI, 'list').mockResolvedValue({ data: [] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders FAQ, form section, and portal quick links', async () => {
    renderSupport()
    expect(screen.getByRole('heading', { level: 1, name: /^support$/i })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /message ziyara support/i })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /common questions/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /my bookings/i })).toHaveAttribute('href', '/portal/bookings')
    expect(screen.getByRole('link', { name: /my listings/i })).toHaveAttribute('href', '/portal/listings')
    await waitFor(() => {
      expect(api.portalSupportAPI.list).toHaveBeenCalled()
    })
  })
})
