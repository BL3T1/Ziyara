import { useEffect, useMemo, useState } from 'react'
import type { ServiceDto } from '../../types/api'
import { landingPublicApi } from './landingPublicApi'

type LandingServiceWithImage = ServiceDto & { imageUrl?: string }

interface LandingLiveData {
  services: LandingServiceWithImage[]
  totalServices: number
  totalCities: number
  averageBasePrice: number
  popularCities: string[]
  loading: boolean
}

const FALLBACK_DATA: LandingLiveData = {
  services: [],
  totalServices: 0,
  totalCities: 0,
  averageBasePrice: 0,
  popularCities: [],
  loading: false,
}

let cachedServices: LandingServiceWithImage[] | null = null
let pending: Promise<LandingServiceWithImage[]> | null = null

async function loadLandingServices(): Promise<LandingServiceWithImage[]> {
  if (cachedServices) return cachedServices
  if (pending) return pending

  pending = (async () => {
    const page = await landingPublicApi.listServices({ page: 0, size: 120 })
    const content = page?.content ?? []
    const topForImages = content.slice(0, 18)
    const imagePairs = await Promise.all(
      topForImages.map(async (service) => {
        try {
          const images = await landingPublicApi.listServiceImages(service.id)
          const primary = images.find((img) => img.primary) ?? images[0]
          return [service.id, primary?.url] as const
        } catch {
          return [service.id, undefined] as const
        }
      }),
    )
    const imageMap = new Map<string, string | undefined>(imagePairs)
    const merged = content.map((service) => ({ ...service, imageUrl: imageMap.get(service.id) }))
    cachedServices = merged
    pending = null
    return merged
  })().catch((err) => {
    pending = null
    throw err
  })

  return pending
}

export function useLandingLiveData(): LandingLiveData {
  const [services, setServices] = useState<LandingServiceWithImage[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let mounted = true
    loadLandingServices()
      .then((items) => {
        if (!mounted) return
        setServices(items)
      })
      .catch(() => {
        if (!mounted) return
        setServices([])
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })
    return () => {
      mounted = false
    }
  }, [])

  const summary = useMemo(() => {
    if (!services.length) return FALLBACK_DATA
    const cityCount = new Map<string, number>()
    let pricedCount = 0
    let pricedTotal = 0
    for (const service of services) {
      const city = service.city?.trim()
      if (city) cityCount.set(city, (cityCount.get(city) ?? 0) + 1)
      if (typeof service.basePrice === 'number' && Number.isFinite(service.basePrice)) {
        pricedCount += 1
        pricedTotal += service.basePrice
      }
    }
    const popularCities = Array.from(cityCount.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 6)
      .map(([city]) => city)

    return {
      services,
      totalServices: services.length,
      totalCities: cityCount.size,
      averageBasePrice: pricedCount ? Math.round(pricedTotal / pricedCount) : 0,
      popularCities,
      loading,
    }
  }, [loading, services])

  return summary
}
