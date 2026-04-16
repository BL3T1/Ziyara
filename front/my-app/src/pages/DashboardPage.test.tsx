import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '../context/AuthContext'
import { LanguageProvider } from '../context/LanguageContext'
import { DashboardPage } from './DashboardPage'
import * as api from '../services/api'

const mockUser = {
  id: '1',
  email: 'admin@test.com',
  name: 'Test Admin',
  role: 'admin' as const,
}

vi.mock('../context/DisplayCurrencyContext', () => ({
  useDisplayCurrency: () => ({
    defaultCurrency: 'USD',
    formatMoney: (n: number, c?: string) => `${c ?? 'USD'} ${n}`,
    displayInDefault: (n: number) =>
      `USD ${Number(n).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`,
    refreshDisplayCurrency: vi.fn(),
  }),
}))

function renderDashboard() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <LanguageProvider>
          <AuthProvider defaultUser={mockUser}>
            <DashboardPage />
          </AuthProvider>
        </LanguageProvider>
      </BrowserRouter>
    </QueryClientProvider>,
  )
}

vi.mock('../services/api', () => ({
  dashboardAPI: {
    getBootstrap: vi.fn(),
    getLive: vi.fn(),
    getKpis: vi.fn(),
    getActivity: vi.fn(),
    getServiceHealth: vi.fn(),
    getCommissionAnalysis: vi.fn(),
    getPayouts: vi.fn(),
  },
}))

describe('DashboardPage', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('token', 'test-token')
    const bootstrapPayload = {
      kpis: {
        totalRevenue: 5000,
        revenueCurrency: 'USD',
        activeBookings: 3,
        totalBookings: 10,
        totalProviders: 5,
        pendingComplaints: 1,
        openTickets: 0,
      },
      activity: [],
      serviceHealth: { serviceCountByType: {}, activeBookingCountByType: {} },
      commissionAnalysis: {
        start: '2025-01-01',
        end: '2025-01-31',
        totalBaseAmount: 0,
        totalCommissionAmount: 0,
        currency: 'USD',
      },
      payouts: { start: '2025-01-01', end: '2025-01-31', payouts: [] },
    }
    vi.mocked(api.dashboardAPI.getBootstrap).mockResolvedValue({ data: bootstrapPayload })
    vi.mocked(api.dashboardAPI.getLive).mockResolvedValue({
      data: {
        kpis: bootstrapPayload.kpis,
        activity: bootstrapPayload.activity,
        serviceHealth: bootstrapPayload.serviceHealth,
      },
    })
  })

  it('renders welcome heading', async () => {
    renderDashboard()
    const heading = await screen.findByRole('heading', { name: /welcome to ziyara/i })
    expect(heading).toBeInTheDocument()
  })

  it('loads dashboard via bootstrap then shows metrics blurb', async () => {
    renderDashboard()
    await waitFor(() => {
      expect(api.dashboardAPI.getBootstrap).toHaveBeenCalled()
    })
    await waitFor(() => {
      expect(screen.getByText(/Total Revenue/i)).toBeInTheDocument()
    })
  })

  it('displays revenue stat when KPIs load', async () => {
    renderDashboard()
    await waitFor(() => {
      expect(screen.getByText(/Total Revenue/i)).toBeInTheDocument()
    })
    expect(screen.getByText(/USD\s*5,000/)).toBeInTheDocument()
  })
})
