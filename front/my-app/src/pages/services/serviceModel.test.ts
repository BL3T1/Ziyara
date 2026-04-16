import { describe, expect, it } from 'vitest'
import { getServicePrimaryImageUrl, parseServiceCategorySlug, type ServiceEntity } from './serviceModel'

describe('parseServiceCategorySlug', () => {
  it('accepts known slugs only', () => {
    expect(parseServiceCategorySlug('hotels')).toBe('hotels')
    expect(parseServiceCategorySlug('resorts')).toBe('resorts')
    expect(parseServiceCategorySlug('restaurants')).toBe('restaurants')
    expect(parseServiceCategorySlug(undefined)).toBeNull()
    expect(parseServiceCategorySlug('')).toBeNull()
    expect(parseServiceCategorySlug('../../../etc/passwd')).toBeNull()
    expect(parseServiceCategorySlug('//evil')).toBeNull()
  })
})

describe('getServicePrimaryImageUrl', () => {
  const base: Omit<ServiceEntity, 'category' | 'metadata'> = {
    id: '1',
    name: 'Test',
    description: '',
    images: [],
    price: null,
    currency: 'USD',
    rating: null,
  }

  it('sanitizes malicious URLs to default avatar', () => {
    const svc: ServiceEntity = {
      ...base,
      category: 'hotels',
      metadata: { amenities: [] },
      images: [
        {
          id: 'i1',
          serviceId: '1',
          url: 'javascript:alert(1)',
          primary: true,
          displayOrder: 0,
          category: 'OTHER',
        },
      ],
    }
    expect(getServicePrimaryImageUrl(svc)).toBe('/default-avatar.svg')
  })

  it('allows safe relative URLs', () => {
    const svc: ServiceEntity = {
      ...base,
      category: 'hotels',
      metadata: { amenities: [] },
      images: [
        {
          id: 'i1',
          serviceId: '1',
          url: '/media/x.png',
          primary: true,
          displayOrder: 0,
          category: 'OTHER',
        },
      ],
    }
    expect(getServicePrimaryImageUrl(svc)).toBe('/media/x.png')
  })
})
