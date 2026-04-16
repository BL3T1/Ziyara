import { describe, it, expect } from 'vitest'
import { serviceTypeToListingsPath } from './serviceTypeSegment'

describe('serviceTypeToListingsPath', () => {
  it('maps hotel and resort types', () => {
    expect(serviceTypeToListingsPath('HOTEL')).toBe('hotels')
    expect(serviceTypeToListingsPath('RESORT')).toBe('resorts')
  })

  it('maps other verticals', () => {
    expect(serviceTypeToListingsPath('RESTAURANT')).toBe('restaurants')
    expect(serviceTypeToListingsPath('TAXI')).toBe('taxis')
    expect(serviceTypeToListingsPath('TRIP')).toBe('trips')
  })
})
