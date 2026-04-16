import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { Card } from '../../components/Card'
import { getApiErrorMessage, servicesAPI } from '../../services/api'
import type { RestaurantMenuDto, ServiceDto, ServiceImageDto } from '../../types/api'
import { ServiceDetailView } from '../../pages/services/components/ServiceDetailView'
import type { ServiceCategorySlug } from '../../pages/services/serviceModel'
import { normalizeService } from '../../pages/services/serviceModel'
import { sanitizeText, safeImageUrl } from '../../utils/safeRendering'

export function LandingServiceDetailPage() {
  const { category, id } = useParams<{ category: string; id: string }>()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const fromPath = pathname.split('/').filter(Boolean)
  const inferredCategory = fromPath[0] === 'services' ? fromPath[1] : fromPath[0]
  const seg = category ?? inferredCategory
  const safeCategory = (
    seg === 'hotels' || seg === 'resorts' || seg === 'restaurants' || seg === 'trips' || seg === 'taxis' ? seg : null
  ) as ServiceCategorySlug | null

  const [rawService, setRawService] = useState<ServiceDto | null>(null)
  const [menu, setMenu] = useState<RestaurantMenuDto | null>(null)
  const [images, setImages] = useState<ServiceImageDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    setError(null)
    Promise.all([
      servicesAPI.get(id),
      servicesAPI.getImages(id).catch(() => ({ data: [] as ServiceImageDto[] })),
      servicesAPI.getMenu(id).catch(() => ({ data: { serviceId: id, sections: [] } as RestaurantMenuDto })),
    ])
      .then(([svcRes, imgRes, menuRes]) => {
        const svcData = svcRes.data
        setRawService(svcData && typeof svcData === 'object' ? (svcData as ServiceDto) : null)
        setImages(Array.isArray(imgRes.data) ? imgRes.data : [])
        setMenu(menuRes.data && typeof menuRes.data === 'object' ? menuRes.data : { serviceId: id, sections: [] })
      })
      .catch((err) => {
        setRawService(null)
        setImages([])
        setMenu(null)
        setError(getApiErrorMessage(err, 'Failed to load service'))
      })
      .finally(() => setLoading(false))
  }, [id, safeCategory])

  const service = useMemo(() => (rawService ? normalizeService(rawService, images) : null), [rawService, images])
  const backPath = safeCategory ? `/services/${safeCategory}` : '/services'
  const currencyFallback = rawService?.currency ?? 'USD'

  if (loading) return <p className="text-slate-500 dark:text-slate-400">Loading…</p>

  if (!service) {
    return (
      <div className="rounded-xl border border-red-200 bg-red-50 p-8 dark:border-red-800 dark:bg-red-900/20">
        <p className="text-red-800 dark:text-red-200">{error ? 'Could not load service.' : 'Service not found.'}</p>
        {error ? <p className="mt-1 text-sm text-red-700 dark:text-red-300">{error}</p> : null}
        <button type="button" onClick={() => navigate(backPath)} className="mt-2 text-primary hover:underline">
          Back
        </button>
      </div>
    )
  }

  return (
    <>
      <nav className="mb-3 text-sm text-slate-600 dark:text-slate-400">
        <button type="button" onClick={() => navigate('/services')} className="hover:text-primary">
          Services
        </button>
        <span className="mx-2">/</span>
        <button type="button" onClick={() => navigate(backPath)} className="hover:text-primary">
          {safeCategory ? safeCategory.charAt(0).toUpperCase() + safeCategory.slice(1) : 'Category'}
        </button>
        <span className="mx-2">/</span>
        <span className="text-slate-900 dark:text-slate-100">{service.name}</span>
      </nav>

      <button type="button" onClick={() => navigate(backPath)} className="mb-4 text-sm font-medium text-primary hover:underline">
        ← Back to list
      </button>

      <ServiceDetailView service={service} />

      {rawService?.type === 'RESTAURANT' && menu ? (
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
                          <img src={src} alt="" className="h-16 w-16 shrink-0 rounded-md object-cover" loading="lazy" />
                        ) : null
                      })()}
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-baseline justify-between gap-2">
                          <span className="font-medium text-slate-900 dark:text-slate-100">{item.name}</span>
                          {item.price != null ? (
                            <span className="text-sm text-slate-600 dark:text-slate-300">
                              {item.currency ?? currencyFallback} {item.price}
                            </span>
                          ) : null}
                        </div>
                        {item.description ? (
                          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{sanitizeText(item.description)}</p>
                        ) : null}
                      </div>
                    </li>
                  ))}
                </ul>
              </Card>
            ))
          )}
        </div>
      ) : null}
    </>
  )
}
