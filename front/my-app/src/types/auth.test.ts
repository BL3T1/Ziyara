import { describe, it, expect } from 'vitest'
import {
  backendRoleToFrontend,
  canViewProviderCommission,
  isCompanyStaffRole,
  isProviderPortalRole,
} from './auth'

describe('backendRoleToFrontend', () => {
  it('maps SUPER_ADMIN to super_admin', () => {
    expect(backendRoleToFrontend('SUPER_ADMIN')).toBe('super_admin')
  })

  it('maps HR_MANAGER to hr', () => {
    expect(backendRoleToFrontend('HR_MANAGER')).toBe('hr')
  })

  it('maps CEO to executive', () => {
    expect(backendRoleToFrontend('CEO')).toBe('executive')
  })

  it('maps SALES_MANAGER to admin', () => {
    expect(backendRoleToFrontend('SALES_MANAGER')).toBe('admin')
  })

  it('maps FINANCE_MANAGER to finance', () => {
    expect(backendRoleToFrontend('FINANCE_MANAGER')).toBe('finance')
  })

  it('maps SUPPORT_MANAGER to support', () => {
    expect(backendRoleToFrontend('SUPPORT_MANAGER')).toBe('support')
  })

  it('maps PROVIDER_MANAGER to provider', () => {
    expect(backendRoleToFrontend('PROVIDER_MANAGER')).toBe('provider')
  })

  it('maps CUSTOMER to user', () => {
    expect(backendRoleToFrontend('CUSTOMER')).toBe('user')
  })

  it('returns user for unknown role', () => {
    expect(backendRoleToFrontend('UNKNOWN')).toBe('user')
  })

  it('handles empty string', () => {
    expect(backendRoleToFrontend('')).toBe('user')
  })
})

describe('isCompanyStaffRole', () => {
  it('allows internal staff roles', () => {
    expect(isCompanyStaffRole('super_admin')).toBe(true)
    expect(isCompanyStaffRole('admin')).toBe(true)
    expect(isCompanyStaffRole('hr')).toBe(true)
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

describe('canViewProviderCommission', () => {
  it('allows super_admin, executive, finance', () => {
    expect(canViewProviderCommission('super_admin')).toBe(true)
    expect(canViewProviderCommission('executive')).toBe(true)
    expect(canViewProviderCommission('finance')).toBe(true)
  })

  it('denies support and sales-mapped admin by policy', () => {
    expect(canViewProviderCommission('support')).toBe(false)
    expect(canViewProviderCommission('admin')).toBe(false)
    expect(canViewProviderCommission('hr')).toBe(false)
  })
})
