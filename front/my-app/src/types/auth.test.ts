import { describe, it, expect } from 'vitest'
import {
  backendRoleToFrontend,
  isCompanyStaffRole,
  isProviderPortalRole,
} from './auth'

describe('backendRoleToFrontend', () => {
  it('maps SUPER_ADMIN to super_admin', () => {
    expect(backendRoleToFrontend('SUPER_ADMIN')).toBe('super_admin')
  })

  it('maps CUSTOMER to user', () => {
    expect(backendRoleToFrontend('CUSTOMER')).toBe('user')
  })

  it('maps STAFF without portal access to staff', () => {
    expect(backendRoleToFrontend('STAFF')).toBe('staff')
    expect(backendRoleToFrontend('STAFF', false)).toBe('staff')
  })

  it('maps STAFF with portal access to provider', () => {
    expect(backendRoleToFrontend('STAFF', true)).toBe('provider')
  })

  it('returns staff for unknown role without portal access', () => {
    expect(backendRoleToFrontend('UNKNOWN')).toBe('staff')
  })

  it('returns provider for unknown role with portal access', () => {
    expect(backendRoleToFrontend('UNKNOWN', true)).toBe('provider')
  })

  it('handles empty string', () => {
    expect(backendRoleToFrontend('')).toBe('staff')
  })
})

describe('isCompanyStaffRole', () => {
  it('allows internal staff roles', () => {
    expect(isCompanyStaffRole('super_admin')).toBe(true)
    expect(isCompanyStaffRole('admin')).toBe(true)
    expect(isCompanyStaffRole('staff')).toBe(true)
  })

  it('rejects provider and customer', () => {
    expect(isCompanyStaffRole('provider')).toBe(false)
    expect(isCompanyStaffRole('user')).toBe(false)
  })
})

describe('isProviderPortalRole', () => {
  it('allows only provider', () => {
    expect(isProviderPortalRole('provider')).toBe(true)
    expect(isProviderPortalRole('admin')).toBe(false)
  })
})
