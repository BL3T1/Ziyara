import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { Card } from '../../components/Card'
import { getApiErrorMessage, servicesAPI } from '../../services/api'
import type { RestaurantMenuDto, ServiceDto, ServiceImageDto } from '../../types/api'
import { ServiceDetailView } from '../../pages/services/components/ServiceDetailView'
import { LandingServiceBookingPanel } from './LandingServiceBookingPanel'
import type { ServiceCategorySlug } from '../../pages/services/serviceModel'
import { normalizeService } from '../../pages/services/serviceModel'
import { sanitizeText, safeImageUrl } from '../../utils/safeRendering'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { useLanguage } from '../../context/LanguageContext'

function DetailSkeleton() {
  return (
    <div className="animate-pulse space-y-5">
      <div className="h-5 w-32 rounded-lg bg-slate-200/80" />
      <div className="h-64 w-full rounded-2xl bg-slate-200/80" />
      <div className="h-6 w-48 rounded-lg bg-slate-200/80" />
      <div className="space-y-2">
        <div className="h-4 w-full rounded-md bg-slate-200/80" />
        <div className="h-4 w-3/4 rounded-md bg-slate-200/80" />
      </div>
    </div>
  )
}

export function LandingServiceDetailPage() {
  const { category, id } = useParams<{ category: string; id: string }>()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { t } = useLanguage()
  const fromPath = pathname.split('/').filter(Boolean)
  const inferredCategory = fromPath[0] === 'services' ? fromPath[1] : fromPath[0]
  const seg = category ?? inferredCategory
  const safeCategory = (
    seg === 'hotels' || seg === 'resorts' || seg === 'restaurants' || seg === 'trips' || seg === 'taxis' ? seg : null
  ) as ServiceCategorySlug | null

  const [rawService, setRawService] = useState<ServiceDto | null>(null)
  const serviceName = rawService ? sanitizeText(rawService.name ?? '') : ''
  const serviceCity = rawService ? sanitizeText(rawService.city ?? '') : ''
  useDocumentMeta({
    title: serviceName ? `${serviceName}${serviceCity ? ` · ${serviceCity}` : ''} · Ziyara` : 'Ziyara',
    description: serviceName ? `Book ${serviceName} on Ziyara — instant confirmation, best rates.` : undefined,
  })
  const [menu, setMenu] = useState<RestaurantMenuDto | null>(null)
  const [images, setImages] = useState<ServiceImageDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    Promise.resolve().then(() => {
      setLoading(true)
      setError(null)
      return Promise.all([
        servicesAPI.get(id),
        servicesAPI.getImages(id).catch(() => ({ data: [] as ServiceImageDto[] })),
        servicesAPI.getMenu(id).catch(() => ({ data: { serviceId: id, sections: [] } as RestaurantMenuDto })),
      ])
    })
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
        setError(getApiErrorMessage(err, t('landingServiceDetail.loadError')))
      })
      .finally(() => setLoading(false))
  }, [id, safeCategory, t])

  const service = useMemo(() => (rawService ? normalizeService(rawService, images) : null), [rawService, images])
  const backPath = safeCategory ? `/services/${safeCategory}` : '/services'
  const currencyFallback = rawService?.currency ?? 'USD'

  if (loading) {
    return (
      <div className="lp-sheet">
        <DetailSkeleton />
      </div>
    )
  }

  if (!service) {
    return (
      <div className="lp-sheet" style={{ borderColor: 'rgba(220, 100, 100, 0.35)', background: 'rgba(255, 245, 245, 0.9)' }}>
        <p className="font-semibold text-red-900">{error ? t('landingServiceDetail.loadError') : t('landingServiceDetail.notFound')}</p>
        {error ? <p className="mt-1 text-sm text-red-800/90">{error}</p> : null}
        <button type="button" onClick={() => navigate(backPath)} className="lp-link-quiet mt-3 inline-block font-semibold lp-text-accent">
          {t('landingServiceDetail.backBtn')}
        </button>
      </div>
    )
  }

  const categoryLabel = safeCategory ? safeCategory.charAt(0).toUpperCase() + safeCategory.slice(1) : t('landingServiceDetail.breadcrumbRoot')
  const crumbBtn = 'border-0 bg-transparent p-0 font-medium cursor-pointer hover:underline'
  const crumbStyle = { color: 'var(--accent-teal)' } as const

  return (
    <div className="lp-sheet">
      {/* Single breadcrumb — no duplicate back button */}
      <nav className="mb-4 flex items-center gap-1 text-sm lp-muted" aria-label="Breadcrumb">
        <button type="button" onClick={() => navigate('/services')} className={crumbBtn} style={crumbStyle}>
          {t('landingServiceDetail.breadcrumbRoot')}
        </button>
        <span className="mx-1 text-slate-400">/</span>
        <button type="button" onClick={() => navigate(backPath)} className={crumbBtn} style={crumbStyle}>
          {categoryLabel}
        </button>
        <span className="mx-1 text-slate-400">/</span>
        <span style={{ color: 'var(--ink-heading)', fontWeight: 600 }}>{service.name}</span>
      </nav>

      {/* Two-column layout on desktop: content left, sticky booking panel right */}
      <div className="lp-service-detail-layout">
        <div className="lp-service-detail-main">
          <ServiceDetailView service={service} />

          {rawService?.type === 'RESTAURANT' && menu ? (
        <div className="mt-8 space-y-6">
          <h2 className="text-lg font-semibold lp-text-heading">
            {t('landingServiceDetail.menuHeading')}
          </h2>
          {menu.sections.length === 0 ? (
            <p className="text-sm lp-muted">{t('landingServiceDetail.menuEmpty')}</p>
          ) : (
            menu.sections.map((section) => (
              <Card key={section.id} surface="landing" className="!p-5">
                <h3 className="text-base font-semibold lp-text-heading">
                  {section.title}
                </h3>
                <ul className="mt-4 divide-y" style={{ borderColor: 'rgba(90, 122, 130, 0.15)' }}>
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
                          <span className="font-medium lp-text-heading">
                            {item.name}
                          </span>
                          {item.price != null ? (
                            <span className="text-sm lp-text-muted">
                              {item.currency ?? currencyFallback} {item.price}
                            </span>
                          ) : null}
                        </div>
                        {item.description ? (
                          <p className="mt-1 text-sm lp-text-muted">
                            {sanitizeText(item.description)}
                          </p>
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
        </div>{/* end lp-service-detail-main */}

        {/* Sticky booking sidebar */}
        <aside className="lp-service-detail-sidebar">
          <div className="lp-service-booking-sticky">
            <LandingServiceBookingPanel service={service} />
          </div>
        </aside>
      </div>{/* end lp-service-detail-layout */}
    </div>
  )
}
