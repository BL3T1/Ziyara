import { useLocation, useNavigate } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { ServiceGallery } from '../../pages/services/components/ServiceGallery'
import type { ServiceCategorySlug } from '../../pages/services/serviceModel'
import { useServiceCatalog } from '../../pages/services/useServiceCatalog'

const CATEGORY_CONFIG: Record<ServiceCategorySlug, { titleKey: string; descriptionKey: string }> = {
  hotels: { titleKey: 'title.hotels', descriptionKey: 'servicesPage.descHotels' },
  resorts: { titleKey: 'title.resorts', descriptionKey: 'servicesPage.descResorts' },
  restaurants: { titleKey: 'title.restaurants', descriptionKey: 'servicesPage.descRestaurants' },
  trips: { titleKey: 'title.trips', descriptionKey: 'servicesPage.descTrips' },
  taxis: { titleKey: 'title.taxis', descriptionKey: 'servicesPage.descTaxis' },
}

export function LandingServiceTypePage() {
  const { pathname } = useLocation()
  const { t } = useLanguage()
  const navigate = useNavigate()
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

  const { services, loading, error, reload } = useServiceCatalog(safeCategory ?? 'hotels', {
    loadPartners: false,
  })

  if (!safeCategory || !config) {
    return (
      <div className="lp-sheet text-center" style={{ borderColor: 'rgba(180, 130, 70, 0.35)', background: 'rgba(255, 248, 235, 0.9)' }}>
        <h2 className="lp-h1" style={{ color: 'var(--accent-tan-dark)' }}>
          {t('servicesPage.unknownType')}
        </h2>
        <p className="lp-body" style={{ marginTop: 12 }}>
          {t('servicesPage.unknownHint')}
        </p>
      </div>
    )
  }

  const pageTitle = t(config.titleKey)
  const pageDescription = t(config.descriptionKey)

  return (
    <div className="lp-sheet">
      <h1 className="lp-h1">{pageTitle}</h1>
      <p className="lp-body" style={{ marginTop: 10 }}>
        {pageDescription}
      </p>

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
        <p className="mt-4 lp-muted">{t('ui.loading')}</p>
      ) : services.length === 0 ? (
        <p className="mt-4 lp-muted">{t('servicesPage.noListed', { type: pageTitle.toLowerCase() })}</p>
      ) : (
        <div className="mt-6">
          <ServiceGallery services={services} onOpen={(service) => navigate(`/services/${safeCategory}/${service.id}`)} />
        </div>
      )}
    </div>
  )
}
