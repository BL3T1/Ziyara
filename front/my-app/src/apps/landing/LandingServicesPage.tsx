import { Link } from 'react-router-dom'
import { useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useLandingLiveData } from './useLandingLiveData'
import { useLandingPageContent } from './useLandingPageContent'

const TYPE_TO_SLUG: Record<string, string> = {
  HOTEL: 'hotels',
  RESORT: 'resorts',
  RESTAURANT: 'restaurants',
  TAXI: 'taxis',
  TRIP: 'trips',
}

const CATEGORIES = [
  { key: 'all', labelKey: 'servicesPage.all', type: null },
  { key: 'hotels', labelKey: 'title.hotels', type: 'HOTEL' },
  { key: 'resorts', labelKey: 'title.resorts', type: 'RESORT' },
  { key: 'restaurants', labelKey: 'title.restaurants', type: 'RESTAURANT' },
  { key: 'trips', labelKey: 'title.trips', type: 'TRIP' },
  { key: 'taxis', labelKey: 'title.taxis', type: 'TAXI' },
] as const

function ServiceCardSkeleton() {
  return (
    <div className="lp-glass-card animate-pulse !p-0 overflow-hidden">
      <div className="h-44 w-full lp-skeleton" />
      <div className="p-5 space-y-2">
        <div className="h-5 w-3/4 rounded-md lp-skeleton" />
        <div className="h-4 w-full rounded-md" style={{ background: 'rgba(90,100,110,0.08)' }} />
        <div className="h-4 w-2/3 rounded-md" style={{ background: 'rgba(90,100,110,0.08)' }} />
      </div>
    </div>
  )
}

export function LandingServicesPage() {
  const { t } = useLanguage()
  const { services, loading } = useLandingLiveData()
  const { readString: pageText } = useLandingPageContent('services')
  const [activeCategory, setActiveCategory] = useState<string>('all')

  const cards = activeCategory === 'all'
    ? services
    : services.filter((s) => s.type === CATEGORIES.find((c) => c.key === activeCategory)?.type)

  return (
    <section className="landing-scroll-parallax lp-sheet">
      <p className="landing-fade-up lp-eyebrow lp-eyebrow--tight">{pageText('eyebrow', t('landingBusiness.servicesEyebrow'))}</p>
      <h1 className="landing-fade-up landing-fade-up-delay-1 lp-h1 mt-3">
        {pageText('title', t('landingBusiness.servicesTitle'))}
      </h1>

      {/* Category tabs */}
      <div className="mt-6 flex flex-wrap gap-2">
        {CATEGORIES.map((cat) => {
          const active = activeCategory === cat.key
          return (
            <button
              key={cat.key}
              type="button"
              onClick={() => setActiveCategory(cat.key)}
              className="rounded-full border px-4 py-1.5 text-sm font-medium transition-all"
              style={{
                borderColor: active ? 'var(--accent-teal)' : 'rgba(90,122,130,0.25)',
                background: active ? 'rgba(61,112,128,0.12)' : 'transparent',
                color: active ? 'var(--accent-teal)' : 'var(--ink-muted)',
              }}
            >
              {cat.key === 'all' ? t('servicesPage.all') || 'All' : t(cat.labelKey)}
            </button>
          )
        })}
      </div>

      {loading ? (
        <div className="mt-8 grid gap-6 md:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => <ServiceCardSkeleton key={i} />)}
        </div>
      ) : (
        <div className="mt-8 grid gap-6 md:grid-cols-3">
          {cards.map((card) => {
            const slug = TYPE_TO_SLUG[card.type ?? ''] ?? 'hotels'
            const detailPath = `/services/${slug}/${card.id}`
            return (
              <article key={card.id} className="lp-glass-card group/card !p-5">
                {card.imageUrl ? (
                  <div className="relative mb-4 overflow-hidden rounded-xl">
                    <img
                      src={card.imageUrl}
                      alt={card.name}
                      className="h-44 w-full object-cover transition duration-500 group-hover/card:scale-[1.03]"
                      loading="lazy"
                    />
                    <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-[rgb(12_20_36/0.35)] via-transparent to-transparent" />
                  </div>
                ) : (
                  <div className="mb-4 h-44 w-full rounded-xl" style={{ background: 'rgba(90,100,110,0.08)' }} />
                )}
                <h2 className="text-lg font-semibold lp-text-heading">
                  {card.name}
                </h2>
                <p className="mt-2 text-sm leading-relaxed lp-text-muted">
                  {card.description || card.city || t('landingBusiness.servicesTitle')}
                </p>
                <Link
                  to={detailPath}
                  className="mt-4 inline-flex items-center gap-1.5 text-sm font-semibold transition hover:gap-2 lp-text-accent"
                >
                  {t('servicesPage.viewDetails')}
                  <span aria-hidden>→</span>
                </Link>
              </article>
            )
          })}
          {cards.length === 0 && (
            <div className="lp-glass-card md:col-span-3 !p-8 text-center">
              <h2 className="text-lg font-semibold lp-text-heading">
                {t('landingBusiness.servicesTitle')}
              </h2>
              <p className="mt-2 text-sm lp-text-muted">
                {t('landingBusiness.heroBody')}
              </p>
              <Link to="/services/hotels" className="lp-btn lp-btn-primary lp-btn-sm mt-4 inline-block no-underline" style={{ textDecoration: 'none' }}>
                {t('landingBusiness.statsBrowseCta')}
              </Link>
            </div>
          )}
        </div>
      )}
    </section>
  )
}
