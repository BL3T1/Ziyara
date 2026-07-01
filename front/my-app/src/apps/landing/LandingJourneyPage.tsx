import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { landingPublicApi, type JourneyRecommendation } from './landingPublicApi'
import type { ServiceDto } from '../../types/api'

const TYPE_TO_SLUG: Record<string, string> = {
  HOTEL: 'hotels', RESORT: 'resorts', RESTAURANT: 'restaurants', TAXI: 'taxis', TRIP: 'trips',
}

const inputCls =
  'w-full rounded-2xl border border-[rgba(90,122,130,0.25)] bg-white/60 px-4 py-3 text-sm text-[var(--ink)] placeholder:text-[var(--ink-muted)] focus:border-[var(--accent-teal)] focus:outline-none dark:bg-white/5'

function ServiceCard({ svc, t }: { svc: ServiceDto; t: (k: string) => string }) {
  const slug = TYPE_TO_SLUG[svc.type ?? ''] ?? 'hotels'
  const detailPath = `/services/${slug}/${svc.id}`
  const price = (svc as Record<string, unknown>).basePrice as number | null ?? svc.price
  return (
    <article className="lp-glass-card group/card !p-5">
      {(svc as Record<string, unknown>).imageUrl ? (
        <div className="relative mb-4 overflow-hidden rounded-xl">
          <img
            src={(svc as Record<string, unknown>).imageUrl as string}
            alt={svc.name}
            className="h-40 w-full object-cover transition duration-500 group-hover/card:scale-[1.03]"
            loading="lazy"
          />
        </div>
      ) : (
        <div className="mb-4 h-40 w-full rounded-xl" style={{ background: 'rgba(90,100,110,0.08)' }} />
      )}
      <h3 className="text-base font-semibold lp-text-heading">{svc.name}</h3>
      {svc.city && <p className="mt-0.5 text-xs lp-text-muted">{svc.city}</p>}
      {price != null && (
        <p className="mt-1 text-sm font-semibold lp-text-accent">
          {t('landingJourney.from')} {price} {(svc as Record<string, unknown>).currency as string ?? 'USD'}
        </p>
      )}
      <Link to={detailPath} className="mt-3 inline-flex items-center gap-1 text-sm font-semibold lp-text-accent hover:gap-2 transition-all">
        {t('landingJourney.bookNow')}
      </Link>
    </article>
  )
}

function ResultSection({ title, items, t }: { title: string; items: ServiceDto[]; t: (k: string) => string }) {
  return (
    <div>
      <h2 className="mb-4 text-lg font-bold lp-text-heading">{title}</h2>
      {items.length === 0 ? (
        <p className="text-sm lp-text-muted">{t('landingJourney.noResults')}</p>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((svc) => <ServiceCard key={svc.id} svc={svc} t={t} />)}
        </div>
      )}
    </div>
  )
}

export function LandingJourneyPage() {
  const { t } = useLanguage()
  useDocumentMeta({
    title: 'Plan your journey · Ziyara',
    description: 'Get hotel, taxi, and restaurant recommendations tailored to your arrival.',
  })

  const [airport, setAirport] = useState('')
  const [city, setCity] = useState('')
  const [date, setDate] = useState('')
  const [guests, setGuests] = useState(1)
  const [maxBudget, setMaxBudget] = useState('')

  const [results, setResults] = useState<JourneyRecommendation | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!city.trim()) return
    setLoading(true)
    setError('')
    try {
      const data = await landingPublicApi.journeyRecommend({
        city: city.trim(),
        guests,
        maxBudget: maxBudget.trim() || undefined,
      })
      setResults(data)
    } catch {
      setError('Could not load recommendations. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="landing-scroll-parallax lp-sheet">
      <p className="landing-fade-up lp-eyebrow lp-eyebrow--tight">{t('landingJourney.eyebrow')}</p>
      <h1 className="landing-fade-up landing-fade-up-delay-1 lp-h1 mt-3">{t('landingJourney.title')}</h1>
      <p className="landing-fade-up landing-fade-up-delay-2 mt-4 text-base lp-text-muted max-w-2xl">
        {t('landingJourney.subtitle')}
      </p>

      {!results ? (
        <form onSubmit={handleSubmit} className="mt-8 lp-glass-card !p-8 max-w-2xl space-y-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium lp-text-muted">{t('landingJourney.fieldAirport')}</label>
            <input
              className={inputCls}
              placeholder={t('landingJourney.fieldAirportPlaceholder')}
              value={airport}
              onChange={(e) => setAirport(e.target.value)}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="mb-1.5 block text-sm font-medium lp-text-muted">{t('landingJourney.fieldCity')}</label>
              <input
                required
                className={inputCls}
                placeholder={t('landingJourney.fieldCityPlaceholder')}
                value={city}
                onChange={(e) => setCity(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium lp-text-muted">{t('landingJourney.fieldDate')}</label>
              <input
                type="date"
                className={inputCls}
                value={date}
                onChange={(e) => setDate(e.target.value)}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="mb-1.5 block text-sm font-medium lp-text-muted">{t('landingJourney.fieldGuests')}</label>
              <input
                type="number"
                min={1}
                className={inputCls}
                value={guests}
                onChange={(e) => setGuests(Math.max(1, Number(e.target.value)))}
              />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium lp-text-muted">{t('landingJourney.fieldBudget')}</label>
              <input
                type="number"
                min={0}
                className={inputCls}
                placeholder="e.g. 150"
                value={maxBudget}
                onChange={(e) => setMaxBudget(e.target.value)}
              />
            </div>
          </div>
          {error && <p className="text-sm text-red-500">{error}</p>}
          <button
            type="submit"
            disabled={loading || !city.trim()}
            className="lp-btn lp-btn-primary w-full disabled:opacity-50"
          >
            {loading ? t('landingJourney.loading') : t('landingJourney.submit')}
          </button>
        </form>
      ) : (
        <div className="mt-8 space-y-10">
          <button
            type="button"
            onClick={() => setResults(null)}
            className="text-sm font-medium lp-text-accent hover:underline"
          >
            {t('landingJourney.backToForm')}
          </button>
          <ResultSection title={t('landingJourney.sectionHotels')} items={results.hotels} t={t} />
          <ResultSection title={t('landingJourney.sectionTaxis')} items={results.taxis} t={t} />
          <ResultSection title={t('landingJourney.sectionRestaurants')} items={results.restaurants} t={t} />
        </div>
      )}
    </section>
  )
}
