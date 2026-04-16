import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { LanguageProvider } from '../../context/LanguageContext'
import { ClientPortalOverview } from './ClientPortalOverview'
import * as api from '../../services/api'

vi.mock('../../services/api', () => ({
  portalAPI: {
    getDashboard: vi.fn(),
  },
}))

function renderOverview() {
  return render(
    <BrowserRouter>
      <LanguageProvider>
        <ClientPortalOverview />
      </LanguageProvider>
    </BrowserRouter>,
  )
}

describe('ClientPortalOverview', () => {
  beforeEach(() => {
    vi.mocked(api.portalAPI.getDashboard).mockResolvedValue({
      data: {
        serviceCount: 2,
        totalBookings: 5,
        activeBookings: 1,
        totalRevenue: 1200,
        revenueCurrency: 'USD',
      },
    })
  })

  it('loads portal dashboard KPIs', async () => {
    renderOverview()
    await waitFor(() => {
      expect(api.portalAPI.getDashboard).toHaveBeenCalled()
    })
    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument()
    })
    expect(screen.getByText(/USD\s*1,200/)).toBeInTheDocument()
  })
})
