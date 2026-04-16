import { useCallback, useEffect, useMemo, useState } from 'react'
import { dashboardAPI, getApiErrorMessage, servicesAPI } from '../../services/api'
import type { ServiceDto, ServiceHealthDto, ServiceImageDto } from '../../types/api'
import type { ServiceCategorySlug, ServiceEntity } from './serviceModel'
import { SERVICE_CATEGORY_TO_TYPES, normalizeService } from './serviceModel'

function extractContent(data: unknown): ServiceDto[] {
  if (!data || typeof data !== 'object') return []
  const page = data as Record<string, unknown>
  const content = page.content
  return Array.isArray(content) ? (content as ServiceDto[]) : []
}

export function useServiceCatalog(category: ServiceCategorySlug) {
  const [services, setServices] = useState<ServiceEntity[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [health, setHealth] = useState<ServiceHealthDto | null>(null)
  const [reloadToken, setReloadToken] = useState(0)

  const reload = useCallback(() => setReloadToken((x) => x + 1), [])

  useEffect(() => {
    const types = SERVICE_CATEGORY_TO_TYPES[category]
    setLoading(true)
    setError(null)

    const listPromise =
      types.length === 1
        ? servicesAPI.list({ page: 0, size: 100, type: types[0] }).then((r) => extractContent(r.data))
        : Promise.all(types.map((type) => servicesAPI.list({ page: 0, size: 50, type }))).then((results) =>
            results.flatMap((r) => extractContent(r.data)),
          )

    const healthPromise = dashboardAPI
      .getServiceHealth()
      .then((r) => r.data as ServiceHealthDto)
      .catch(() => null)

    Promise.all([listPromise, healthPromise])
      .then(async ([rawServices, healthData]) => {
        const imagePairs = await Promise.all(
          rawServices.map(async (item) => {
            try {
              const res = await servicesAPI.getImages(item.id)
              return [item.id, Array.isArray(res.data) ? res.data : []] as const
            } catch {
              return [item.id, [] as ServiceImageDto[]] as const
            }
          }),
        )
        const imagesByServiceId = new Map<string, ServiceImageDto[]>(imagePairs)
        const normalized = rawServices
          .map((item) => normalizeService(item, imagesByServiceId.get(item.id) ?? []))
          .filter((item): item is ServiceEntity => item != null)

        setServices(normalized)
        setHealth(healthData)
      })
      .catch((err) => {
        setServices([])
        setHealth(null)
        setError(getApiErrorMessage(err, 'Failed to load services'))
      })
      .finally(() => setLoading(false))
  }, [category, reloadToken])

  const totalListings = useMemo(() => {
    const count = health?.serviceCountByType ?? {}
    return SERVICE_CATEGORY_TO_TYPES[category].reduce((sum, backendType) => sum + (Number(count[backendType]) || 0), 0)
  }, [category, health])

  const activeBookings = useMemo(() => {
    const count = health?.activeBookingCountByType ?? {}
    return SERVICE_CATEGORY_TO_TYPES[category].reduce((sum, backendType) => sum + (Number(count[backendType]) || 0), 0)
  }, [category, health])

  return { services, loading, error, reload, totalListings, activeBookings }
}
