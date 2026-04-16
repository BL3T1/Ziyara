import { useLocation, useNavigate } from 'react-router-dom'
import { Card } from '../../components/Card'
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

  const { services, loading, error, reload, totalListings, activeBookings } = useServiceCatalog(safeCategory ?? 'hotels')

  if (!safeCategory || !config) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-900/20">
        <h2 className="text-xl font-semibold text-amber-800 dark:text-amber-200">{t('servicesPage.unknownType')}</h2>
        <p className="mt-2 text-amber-700 dark:text-amber-300">{t('servicesPage.unknownHint')}</p>
      </div>
    )
  }

  const pageTitle = t(config.titleKey)
  const pageDescription = t(config.descriptionKey)

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">{pageTitle}</h1>
      <p className="mt-2 text-slate-600 dark:text-slate-300">{pageDescription}</p>

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <Card className="p-4">
          <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('servicesPage.totalListings')}</p>
          <p className="mt-1 text-2xl font-bold text-slate-900 dark:text-slate-100">{loading ? t('ui.emDash') : totalListings}</p>
        </Card>
        <Card className="p-4">
          <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('home.activeBookings')}</p>
          <p className="mt-1 text-2xl font-bold text-slate-900 dark:text-slate-100">{loading ? t('ui.emDash') : activeBookings}</p>
        </Card>
      </div>

      {error ? (
        <div className="mt-6 rounded-xl border border-red-200 bg-red-50 p-4 dark:border-red-800 dark:bg-red-900/20">
          <p className="text-sm font-medium text-red-800 dark:text-red-200">{t('servicesPage.loadFailed')}</p>
          <p className="mt-1 text-sm text-red-700 dark:text-red-300">{error}</p>
          <button type="button" onClick={reload} className="mt-3 rounded-lg bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700">
            {t('ui.retry')}
          </button>
        </div>
      ) : null}

      {loading ? (
        <p className="mt-4 text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
      ) : services.length === 0 ? (
        <p className="mt-4 text-slate-500 dark:text-slate-400">{t('servicesPage.noListed', { type: pageTitle.toLowerCase() })}</p>
      ) : (
        <ServiceGallery services={services} onOpen={(service) => navigate(`/services/${safeCategory}/${service.id}`)} />
      )}
    </>
  )
}
