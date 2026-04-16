/**
 * Service category page: Hotels, Resorts, Restaurants, Taxis, Trips.
 * Uses a centralized data hook and reusable gallery card components.
 */

import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { CreateProviderModal } from '../../components/CreateProviderModal'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { Card } from '../../components/Card'
import { canCreateProvider } from '../../types/auth'
import {
  CATEGORY_TO_PROVIDER_SERVICE_TYPE,
  parseServiceCategorySlug,
  type ServiceCategorySlug,
} from './serviceModel'
import { ServiceGallery } from './components/ServiceGallery'
import { useServiceCatalog } from './useServiceCatalog'
import { safeRedirect } from '../../utils/safeRedirect'

const ADD_PARTNER_LABEL_KEY: Record<ServiceCategorySlug, string> = {
  hotels: 'servicesPage.ctaAddPartnerHotel',
  resorts: 'servicesPage.ctaAddPartnerResort',
  restaurants: 'servicesPage.ctaAddPartnerRestaurant',
  trips: 'servicesPage.ctaAddPartnerTrip',
  taxis: 'servicesPage.ctaAddPartnerTaxi',
}

const SERVICE_TYPE_CONFIG: Record<ServiceCategorySlug, { titleKey: string; descriptionKey: string }> = {
  hotels: {
    titleKey: 'title.hotels',
    descriptionKey: 'servicesPage.descHotels',
  },
  resorts: {
    titleKey: 'title.resorts',
    descriptionKey: 'servicesPage.descResorts',
  },
  restaurants: {
    titleKey: 'title.restaurants',
    descriptionKey: 'servicesPage.descRestaurants',
  },
  trips: {
    titleKey: 'title.trips',
    descriptionKey: 'servicesPage.descTrips',
  },
  taxis: {
    titleKey: 'title.taxis',
    descriptionKey: 'servicesPage.descTaxis',
  },
}

export function ServiceTypePage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const { type } = useParams<{ type: string }>()
  const navigate = useNavigate()
  const [createPartnerOpen, setCreatePartnerOpen] = useState(false)
  const category = parseServiceCategorySlug(type)
  const config = category ? SERVICE_TYPE_CONFIG[category] : null
  const showAddPartner = user?.role ? canCreateProvider(user.role) : false
  const {
    services,
    loading,
    error,
    reload,
    totalListings,
    activeBookings,
  } = useServiceCatalog(category && config ? category : 'hotels')

  if (!type || !category || !config) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-900/20">
        <h2 className="text-xl font-semibold text-amber-800 dark:text-amber-200">{t('servicesPage.unknownType')}</h2>
        <p className="mt-2 text-amber-700 dark:text-amber-300">{t('servicesPage.unknownHint')}</p>
        <button
          type="button"
          onClick={() => navigate('/dashboard')}
          className="mt-4 rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700"
        >
          {t('ui.dashboard')}
        </button>
      </div>
    )
  }

  const pageTitle = t(config.titleKey)
  const presetProviderType = CATEGORY_TO_PROVIDER_SERVICE_TYPE[category]

  return (
    <>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">{pageTitle}</h1>
        </div>
        {showAddPartner && (
          <button
            type="button"
            onClick={() => setCreatePartnerOpen(true)}
            className="dashboard-btn-primary shrink-0"
          >
            {t(ADD_PARTNER_LABEL_KEY[category])}
          </button>
        )}
      </div>

      <CreateProviderModal
        open={createPartnerOpen}
        onClose={() => setCreatePartnerOpen(false)}
        variant="management"
        presetServiceType={presetProviderType}
        onCreated={() => reload()}
      />

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="p-4">
          <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('servicesPage.totalListings')}</p>
          <p className="mt-1 text-2xl font-bold text-slate-900 dark:text-slate-100">{loading ? t('ui.emDash') : totalListings}</p>
        </Card>
        <Card className="p-4">
          <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{t('home.activeBookings')}</p>
          <p className="mt-1 text-2xl font-bold text-slate-900 dark:text-slate-100">{loading ? t('ui.emDash') : activeBookings}</p>
        </Card>
      </div>

      {error && (
        <div className="mt-6 rounded-xl border border-red-200 bg-red-50 p-4 dark:border-red-800 dark:bg-red-900/20">
          <p className="text-sm font-medium text-red-800 dark:text-red-200">{t('servicesPage.loadFailed')}</p>
          <p className="mt-1 text-sm text-red-700 dark:text-red-300">{error}</p>
          <p className="mt-2 text-xs text-slate-600 dark:text-slate-400">{t('servicesPage.loadHint')}</p>
          <button
            type="button"
            onClick={reload}
            className="mt-3 rounded-lg bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700"
          >
            {t('ui.retry')}
          </button>
        </div>
      )}

      <div className="mt-8">
        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('servicesPage.allServices', { type: pageTitle })}</h2>
        {loading ? (
          <p className="mt-4 text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
        ) : services.length === 0 && !error ? (
          <p className="mt-4 text-slate-500 dark:text-slate-400">{t('servicesPage.noListed', { type: pageTitle.toLowerCase() })}</p>
        ) : (
          <ServiceGallery
            services={services}
            onOpen={(service) => {
              const slug = category ?? 'hotels'
              navigate(safeRedirect(`/${slug}/${service.id}`, '/dashboard'))
            }}
          />
        )}
      </div>
    </>
  )
}
