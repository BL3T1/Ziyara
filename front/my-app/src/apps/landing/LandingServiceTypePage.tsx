import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { ServiceGallery } from '../../pages/services/components/ServiceGallery'
import type { ServiceCategorySlug, ServiceEntity } from '../../pages/services/serviceModel'
import { useServiceCatalog } from '../../pages/services/useServiceCatalog'
import { ServicesFilterBar, EMPTY_FILTERS } from './ServicesFilterBar'
import type { ServiceFilters } from './ServicesFilterBar'

const CATEGORY_CONFIG: Record<ServiceCategorySlug, { titleKey: string; descriptionKey: string }> = {
  hotels:      { titleKey: 'title.hotels',      descriptionKey: 'servicesPage.descHotels' },
  resorts:     { titleKey: 'title.resorts',      descriptionKey: 'servicesPage.descResorts' },
  restaurants: { titleKey: 'title.restaurants',  descriptionKey: 'servicesPage.descRestaurants' },
  trips:       { titleKey: 'title.trips',        descriptionKey: 'servicesPage.descTrips' },
  taxis:       { titleKey: 'title.taxis',        descriptionKey: 'servicesPage.descTaxis' },
}

type ServiceWithMeta = ServiceEntity & { city?: string }

function filterServices(services: ServiceEntity[], f: ServiceFilters): ServiceEntity[] {
  const q = f.q.trim().toLowerCase()
  const city = f.city.trim().toLowerCase()
  const minP = f.minPrice !== '' ? parseFloat(f.minPrice) : null
  const maxP = f.maxPrice !== '' ? parseFloat(f.maxPrice) : null
  const minR = f.minRating !== '' ? parseInt(f.minRating, 10) : null

  return services.filter((svc) => {
    const s = svc as ServiceWithMeta
    if (q && !(s.name?.toLowerCase().includes(q) || s.description?.toLowerCase().includes(q))) return false
    if (city && s.city?.toLowerCase() !== city) return false
    if (minP !== null && (s.price ?? 0) < minP) return false
    if (maxP !== null && (s.price ?? 0) > maxP) return false
    if (minR !== null && (s.rating ?? 0) < minR) return false
    return true
  })
}

export function LandingServiceTypePage() {
  const { pathname } = useLocation()
  const { t } = useLanguage()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const parts = pathname.split('/').filter(Boolean)
  const pathSegment = parts[0] === 'services' ? parts[1] : parts[0]
  const safeCategory = (
    pathSegment === 'hotels' ||
    pathSegment === 'resorts' ||
    pathSegment === 'restaurants' ||
    pathSegment === 'trips' ||
    pathSegment === 'taxis'
      ? pathSegment
      : null
  ) as ServiceCategorySlug | null
  const config = safeCategory ? CATEGORY_CONFIG[safeCategory] : null
  const categoryLabel = config ? t(config.titleKey) : 'Services'
  useDocumentMeta({
    title: `${categoryLabel} in Lebanon · Ziyara`,
    description: config ? t(config.descriptionKey) : 'Browse and book services with Ziyara.',
  })

  // Filters — initialise from URL params
  const [filters, setFilters] = useState<ServiceFilters>(() => ({
    q:         searchParams.get('q')         ?? '',
    city:      searchParams.get('city')      ?? '',
    minPrice:  searchParams.get('minPrice')  ?? '',
    maxPrice:  searchParams.get('maxPrice')  ?? '',
    minRating: searchParams.get('minRating') ?? '',
  }))

  // Sync filters → URL
  useEffect(() => {
    const params: Record<string, string> = {}
    if (filters.q)         params.q         = filters.q
    if (filters.city)      params.city      = filters.city
    if (filters.minPrice)  params.minPrice  = filters.minPrice
    if (filters.maxPrice)  params.maxPrice  = filters.maxPrice
    if (filters.minRating) params.minRating = filters.minRating
    setSearchParams(params, { replace: true })
  }, [filters, setSearchParams])

  const { services, loading, error, reload } = useServiceCatalog(safeCategory ?? 'hotels', { loadPartners: false })

  const cities = useMemo(
    () => Array.from(new Set(services.map((s) => (s as ServiceWithMeta).city).filter(Boolean) as string[])).sort(),
    [services],
  )
  const filtered = useMemo(() => filterServices(services, filters), [services, filters])

  if (!safeCategory || !config) {
    return (
      <div className="lp-sheet text-center" style={{ borderColor: 'rgba(180, 130, 70, 0.35)', background: 'rgba(255, 248, 235, 0.9)' }}>
        <h2 className="lp-h1" style={{ color: 'var(--accent-tan-dark)' }}>
          {t('servicesPage.unknownType')}
        </h2>
        <p className="lp-body mt-3">
          {t('servicesPage.unknownHint')}
        </p>
      </div>
    )
  }

  return (
    <div className="lp-sheet">
      <h1 className="lp-h1">{t(config.titleKey)}</h1>
      <p className="lp-body mt-2.5">
        {t(config.descriptionKey)}
      </p>

      <ServicesFilterBar filters={filters} setFilters={setFilters} cities={cities} />

      {error ? (
        <div className="mt-6 rounded-[20px] border p-4" style={{ borderColor: 'rgba(220, 100, 100, 0.35)', background: 'rgba(255, 245, 245, 0.85)' }}>
          <p className="text-sm font-semibold text-red-900">{t('servicesPage.loadFailed')}</p>
          <p className="mt-1 text-sm text-red-800/90">{error}</p>
          <button type="button" onClick={reload} className="lp-btn lp-btn-primary lp-btn-sm mt-3 !bg-red-700 hover:!shadow-red-900/20">
            {t('ui.retry')}
          </button>
        </div>
      ) : null}

      {loading ? (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => (
            <div key={i} className="lp-glass-card animate-pulse !p-0 overflow-hidden">
              <div className="h-44 w-full lp-skeleton" />
              <div className="p-5 space-y-2">
                <div className="h-5 w-3/4 rounded-md lp-skeleton" />
                <div className="h-4 w-full rounded-md" style={{ background: 'rgba(90,100,110,0.08)' }} />
                <div className="h-4 w-2/3 rounded-md" style={{ background: 'rgba(90,100,110,0.06)' }} />
              </div>
            </div>
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="mt-6 space-y-2 text-center">
          <p className="lp-muted">{services.length === 0 ? t('servicesPage.noListed', { type: t(config.titleKey).toLowerCase() }) : t('landingFilter.noResultsFilters')}</p>
          {services.length > 0 && (
            <button type="button" onClick={() => setFilters(EMPTY_FILTERS)} className="lp-btn lp-btn-outline lp-btn-sm">
              {t('landingFilter.clearFilters')}
            </button>
          )}
        </div>
      ) : (
        <div className="mt-6">
          {filtered.length < services.length && (
            <p className="mb-3 text-xs text-slate-400">
              {t('landingFilter.resultsCount', { shown: filtered.length, total: services.length })}
            </p>
          )}
          <ServiceGallery services={filtered} onOpen={(service) => navigate(`/services/${safeCategory}/${service.id}`)} />
        </div>
      )}
    </div>
  )
}
