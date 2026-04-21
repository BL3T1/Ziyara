import { describe, it, expect } from 'vitest'
import { getPageTitleForPath, ROUTE_TITLES, getPageTitleKeyForPath, getPageIconForPath } from './routes'

describe('getPageTitleForPath', () => {
  it('returns title for known path', () => {
    expect(getPageTitleForPath('/dashboard')).toBe('Dashboard')
    expect(getPageTitleForPath('/management/users')).toBe('Groups')
    expect(getPageTitleForPath('/admin/roles')).toBe('Roles')
  })

  it('returns System Overview for unknown path', () => {
    expect(getPageTitleForPath('/unknown')).toBe('System Overview')
  })

  it('has titles for main dashboard routes', () => {
    expect(ROUTE_TITLES['/dashboard']).toBeDefined()
    expect(ROUTE_TITLES['/management/providers']).toBe('Providers')
    expect(ROUTE_TITLES['/management/providers/new']).toBe('New provider')
    expect(ROUTE_TITLES['/support/complaints']).toBe('Complaints')
  })
})

describe('getPageTitleKeyForPath', () => {
  it('resolves create provider route', () => {
    expect(getPageTitleKeyForPath('/management/providers/new')).toBe('title.providerNew')
  })

  it('resolves provider portal listing routes', () => {
    expect(getPageTitleKeyForPath('/portal/listings/new')).toBe('title.listingNew')
    expect(getPageTitleKeyForPath('/portal/listings')).toBe('title.listings')
    expect(getPageTitleKeyForPath('/portal/listings/550e8400-e29b-41d4-a716-446655440000')).toBe('title.listingEdit')
  })

  it('resolves management provider edit route by UUID', () => {
    expect(getPageTitleKeyForPath('/management/providers/550e8400-e29b-41d4-a716-446655440000')).toBe('title.providerEdit')
    expect(getPageTitleForPath('/management/providers/550e8400-e29b-41d4-a716-446655440000')).toBe('Edit provider')
    expect(getPageIconForPath('/management/providers/550e8400-e29b-41d4-a716-446655440000')).toBe('providers')
  })
})

describe('getPageIconForPath', () => {
  it('uses hotels icon for portal listings subtree', () => {
    expect(getPageIconForPath('/portal/listings')).toBe('hotels')
    expect(getPageIconForPath('/portal/listings/new')).toBe('hotels')
  })
})
