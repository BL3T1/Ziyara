import type { ServiceDto, ServiceImageDto, ServiceTypeDto } from '../../types/api'
import { safeImageUrl } from '../../utils/safeRendering'

export type ServiceCategorySlug = 'hotels' | 'resorts' | 'restaurants' | 'trips' | 'taxis'

/** Route param values allowed for `/services/:type` service category pages. */
export const SERVICE_CATEGORY_SLUGS: readonly ServiceCategorySlug[] = [
  'hotels',
  'resorts',
  'restaurants',
  'trips',
  'taxis',
] as const

export function parseServiceCategorySlug(raw: string | undefined): ServiceCategorySlug | null {
  if (!raw) return null
  return (SERVICE_CATEGORY_SLUGS as readonly string[]).includes(raw) ? (raw as ServiceCategorySlug) : null
}

export interface BaseService {
  id: string
  name: string
  description: string
  images: ServiceImageDto[]
  price: number | null
  currency: string
  rating: number | null
}

export interface HotelService extends BaseService {
  category: 'hotels'
  metadata: {
    amenities: string[]
  }
}

export interface ResortService extends BaseService {
  category: 'resorts'
  metadata: {
    amenities: string[]
  }
}

export interface RestaurantService extends BaseService {
  category: 'restaurants'
  metadata: {
    cuisine: string
  }
}

export interface TripService extends BaseService {
  category: 'trips'
  metadata: {
    duration: string
  }
}

export interface TaxiService extends BaseService {
  category: 'taxis'
  metadata: {
    vehicleType: string
  }
}

export type ServiceEntity = HotelService | ResortService | RestaurantService | TripService | TaxiService

export const SERVICE_CATEGORY_TO_TYPES: Record<ServiceCategorySlug, ServiceTypeDto[]> = {
  hotels: ['HOTEL'],
  resorts: ['RESORT'],
  restaurants: ['RESTAURANT'],
  trips: ['TRIP'],
  taxis: ['TAXI'],
}

/** Default provider `type` when onboarding from a service vertical page. */
export const CATEGORY_TO_PROVIDER_SERVICE_TYPE: Record<ServiceCategorySlug, ServiceTypeDto> = {
  hotels: 'HOTEL',
  resorts: 'RESORT',
  restaurants: 'RESTAURANT',
  trips: 'TRIP',
  taxis: 'TAXI',
}

export const SERVICE_TYPE_TO_CATEGORY: Partial<Record<ServiceTypeDto, ServiceCategorySlug>> = {
  HOTEL: 'hotels',
  RESORT: 'resorts',
  RESTAURANT: 'restaurants',
  TRIP: 'trips',
  TAXI: 'taxis',
}

function readString(value: unknown): string | null {
  return typeof value === 'string' && value.trim() ? value.trim() : null
}

function readStringList(value: unknown): string[] {
  if (!Array.isArray(value)) return []
  return value.filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
}

function getPrimaryImage(images: ServiceImageDto[]): ServiceImageDto | null {
  return images.find((img) => img.primary) ?? images[0] ?? null
}

export function getServicePrimaryImageUrl(service: ServiceEntity): string {
  const primary = getPrimaryImage(service.images)
  return safeImageUrl(primary?.url) ?? '/default-avatar.svg'
}

export function normalizeService(dto: ServiceDto, images: ServiceImageDto[]): ServiceEntity | null {
  const category = SERVICE_TYPE_TO_CATEGORY[dto.type]
  if (!category) return null

  const base: BaseService = {
    id: dto.id,
    name: dto.name,
    description: dto.description?.trim() || '',
    images,
    price: dto.basePrice ?? null,
    currency: dto.currency ?? 'USD',
    rating: dto.starRating ?? null,
  }

  const record = dto as unknown as Record<string, unknown>

  if (category === 'hotels' || category === 'resorts') {
    const amenities = readStringList(record.amenities)
    return {
      ...base,
      category,
      metadata: {
        amenities: amenities.length ? amenities : ['Wi-Fi', 'Breakfast', 'Parking'],
      },
    } as HotelService | ResortService
  }

  if (category === 'restaurants') {
    return {
      ...base,
      category,
      metadata: {
        cuisine: readString(record.cuisine) ?? 'International',
      },
    }
  }

  if (category === 'taxis') {
    return {
      ...base,
      category: 'taxis',
      metadata: {
        vehicleType: readString(record.vehicleType) ?? 'Standard',
      },
    }
  }

  return {
    ...base,
    category: 'trips',
    metadata: {
      duration: readString(record.duration) ?? 'Full day',
    },
  }
}
