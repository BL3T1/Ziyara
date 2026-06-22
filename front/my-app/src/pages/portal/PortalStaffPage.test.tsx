import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { LanguageProvider } from '../../context/LanguageContext'
import { PortalStaffPage } from './PortalStaffPage'
import * as api from '../../services/api'

function renderStaff() {
  return render(
    <BrowserRouter>
      <LanguageProvider>
        <PortalStaffPage />
      </LanguageProvider>
    </BrowserRouter>,
  )
}

describe('PortalStaffPage', () => {
  beforeEach(() => {
    vi.spyOn(api.usersAPI, 'getMe').mockResolvedValue({
      data: {
        firstName: 'Sam',
        lastName: 'Provider',
        email: 'sam@example.com',
        phone: '+10000000000',
        role: 'STAFF',
        status: 'ACTIVE',
      },
    })
    vi.spyOn(api.providersAPI, 'getMe').mockResolvedValue({
      data: {
        id: 'p1',
        name: 'Desert Inn LLC',
        email: 'front@desert.test',
        status: 'APPROVED',
        type: 'HOTEL',
      },
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('loads account and provider from API', async () => {
    renderStaff()
    await waitFor(() => {
      expect(api.usersAPI.getMe).toHaveBeenCalled()
      expect(api.providersAPI.getMe).toHaveBeenCalled()
    })
    await waitFor(() => {
      expect(screen.getByText('Sam Provider')).toBeInTheDocument()
    })
    expect(screen.getByText('Desert Inn LLC')).toBeInTheDocument()
    expect(screen.getByText('sam@example.com')).toBeInTheDocument()
  })
})
