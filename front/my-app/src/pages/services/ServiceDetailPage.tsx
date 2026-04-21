/**
 * Service detail page with full description, carousel, metadata, and pricing.
 */

import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { getApiErrorMessage, servicesAPI } from '../../services/api'
import { Card } from '../../components/Card'
import type { RestaurantMenuDto, ServiceDto, ServiceImageDto, ServiceTypeDto } from '../../types/api'
import { ServiceDetailView } from './components/ServiceDetailView'
import { ServiceDetailMediaEditor } from './ServiceDetailMediaEditor'
import type { ServiceCategorySlug } from './serviceModel'
import { normalizeService } from './serviceModel'
import { sanitizeText, safeImageUrl } from '../../utils/safeRendering'

function mapTypeToCategory(type: ServiceTypeDto): ServiceCategorySlug | null {
  if (type === 'HOTEL') return 'hotels'
  if (type === 'RESORT') return 'resorts'
  if (type === 'RESTAURANT') return 'restaurants'
  if (type === 'TRIP') return 'trips'
  if (type === 'TAXI') return 'taxis'
  return null
}

export function ServiceDetailPage() {
  const { type, id } = useParams<{ type?: string; id: string }>()
  const location = useLocation()
  const navigate = useNavigate()
  const [rawService, setRawService] = useState<ServiceDto | null>(null)
  const [menu, setMenu] = useState<RestaurantMenuDto | null>(null)
  const [images, setImages] = useState<ServiceImageDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const serviceId = id
    if (!serviceId) {
      setLoading(false)
      return
    }

    const load = () =>
      Promise.all([
        servicesAPI.get(serviceId),
        servicesAPI.getImages(serviceId).catch(() => ({ data: [] as ServiceImageDto[] })),
        servicesAPI.getMenu(serviceId).catch(() => ({ data: { serviceId, sections: [] } as RestaurantMenuDto })),
      ])
        .then(([svcRes, imgRes, menuRes]) => {
          const svcData = svcRes.data
          setRawService(svcData && typeof svcData === 'object' ? (svcData as ServiceDto) : null)
          setImages(Array.isArray(imgRes.data) ? imgRes.data : [])
          setMenu(menuRes.data && typeof menuRes.data === 'object' ? menuRes.data : { serviceId, sections: [] })
        })
        .catch((err) => {
          setRawService(null)
          setImages([])
          setMenu(null)
          setError(getApiErrorMessage(err, 'Failed to load service'))
        })
        .finally(() => setLoading(false))

    setLoading(true)
    setError(null)
    void load()
  }, [id])

  const service = useMemo(() => (rawService ? normalizeService(rawService, images) : null), [rawService, images])
  const currentCategory = useMemo<ServiceCategorySlug | null>(() => {
    if (type === 'hotels' || type === 'resorts' || type === 'restaurants' || type === 'trips' || type === 'taxis') return type
    const segment = location.pathname.split('/')[1]
    if (segment === 'hotels' || segment === 'resorts' || segment === 'restaurants' || segment === 'trips' || segment === 'taxis')
      return segment
    if (!rawService) return null
    return mapTypeToCategory(rawService.type)
  }, [location.pathname, rawService, type])

  const backPath = currentCategory ? `/${currentCategory}` : '/dashboard'
  const currencyFallback = rawService?.currency ?? 'USD'

  if (!id) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 dark:border-amber-800 dark:bg-amber-900/20">
        <p className="text-amber-800 dark:text-amber-200">Missing service ID.</p>
        <button type="button" onClick={() => navigate(backPath)} className="mt-2 text-primary hover:underline">
          Back
        </button>
      </div>
    )
  }

  if (loading) {
    return <p className="text-slate-500 dark:text-slate-400">Loading…</p>
  }

  if (!service) {
    return (
      <div className="rounded-xl border border-red-200 bg-red-50 p-8 dark:border-red-800 dark:bg-red-900/20">
        <p className="text-red-800 dark:text-red-200">{error ? 'Could not load service.' : 'Service not found.'}</p>
        {error && <p className="mt-1 text-sm text-red-700 dark:text-red-300">{error}</p>}
        <button type="button" onClick={() => navigate(backPath)} className="mt-2 text-primary hover:underline">
          Back
        </button>
      </div>
    )
  }

  return (
    <>
      <nav className="mb-3 text-sm text-slate-600 dark:text-slate-400">
        <button type="button" onClick={() => navigate('/dashboard')} className="hover:text-primary">
          Dashboard
        </button>
        <span className="mx-2">/</span>
        <button type="button" onClick={() => navigate(backPath)} className="hover:text-primary">
          {currentCategory ? currentCategory.charAt(0).toUpperCase() + currentCategory.slice(1) : 'Services'}
        </button>
        <span className="mx-2">/</span>
        <span className="text-slate-900 dark:text-slate-100">{service.name}</span>
      </nav>
      <button
        type="button"
        onClick={() => navigate(backPath)}
        className="mb-4 text-sm font-medium text-primary hover:underline"
      >
        ← Back to list
      </button>
      <ServiceDetailView service={service} />

      {rawService?.type === 'RESTAURANT' && menu && (
        <div className="mt-8 space-y-6">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">Menu</h2>
          {menu.sections.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">No menu sections yet.</p>
          ) : (
            menu.sections.map((section) => (
              <Card key={section.id} className="p-5">
                <h3 className="text-base font-semibold text-slate-900 dark:text-slate-100">{section.title}</h3>
                <ul className="mt-4 divide-y divide-slate-200 dark:divide-slate-700">
                  {section.items.map((item) => (
                    <li key={item.id} className="flex gap-3 py-3 first:pt-0">
                      {(() => {
                        const src = safeImageUrl(item.imageUrl)
                        return src ? (
                          <img
                          src={src}
                          alt=""
                          className="h-16 w-16 shrink-0 rounded-md object-cover"
                          loading="lazy"
                          />
                        ) : null
                      })()}
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-baseline justify-between gap-2">
                          <span className="font-medium text-slate-900 dark:text-slate-100">{item.name}</span>
                          {item.price != null && (
                            <span className="text-sm text-slate-600 dark:text-slate-300">
                              {item.currency ?? currencyFallback} {item.price}
                            </span>
                          )}
                        </div>
                        {item.description && (
                          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{sanitizeText(item.description)}</p>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              </Card>
            ))
          )}
        </div>
      )}
      {rawService ? (
        <ServiceDetailMediaEditor
          serviceId={id}
          serviceType={rawService.type}
          currency={currencyFallback}
          images={images}
          menu={menu}
          variant="company"
          onRefresh={async () => {
            const [imgRes, menuRes] = await Promise.all([
              servicesAPI.getImages(id).catch(() => ({ data: [] as ServiceImageDto[] })),
              servicesAPI.getMenu(id).catch(() => ({ data: { serviceId: id, sections: [] } as RestaurantMenuDto })),
            ])
            setImages(Array.isArray(imgRes.data) ? imgRes.data : [])
            setMenu(menuRes.data && typeof menuRes.data === 'object' ? menuRes.data : { serviceId: id, sections: [] })
          }}
        />
      ) : null}
    </>
  )
}
