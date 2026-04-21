import { Card } from '../../components/Card'
import { useLanguage } from '../../context/LanguageContext'
import { Link } from 'react-router-dom'
import { useLandingLiveData } from './useLandingLiveData'
import { useLandingPageContent } from './useLandingPageContent'

const SERVICE_TYPE_TO_LINK: Record<string, string> = {
  HOTEL: '/hotels',
  RESORT: '/resorts',
  RESTAURANT: '/restaurants',
  TRIP: '/trips',
  TAXI: '/taxis',
}

export function LandingServicesPage() {
  const { t } = useLanguage()
  const { services } = useLandingLiveData()
  const { readString: pageText } = useLandingPageContent('services')
  const cards = services.slice(0, 6)

  return (
    <section className="landing-scroll-parallax lp-sheet">
      <p className="landing-fade-up lp-eyebrow lp-eyebrow--tight">{pageText('eyebrow', t('landingBusiness.servicesEyebrow'))}</p>
      <h1 className="landing-fade-up landing-fade-up-delay-1 lp-h1" style={{ marginTop: 12 }}>
        {pageText('title', t('landingBusiness.servicesTitle'))}
      </h1>
      <div className="mt-8 grid gap-6 md:grid-cols-3">
        {cards.map((card) => (
          <Card key={card.id} surface="landing" className="group/card !p-5">
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
            ) : null}
            <h2 className="text-lg font-semibold" style={{ color: 'var(--ink-heading)' }}>
              {card.name}
            </h2>
            <p className="mt-2 text-sm leading-relaxed" style={{ color: 'var(--ink-muted)' }}>
              {card.description || card.city || t('landingBusiness.servicesTitle')}
            </p>
            <Link
              to={SERVICE_TYPE_TO_LINK[card.type] ?? '/services'}
              className="mt-4 inline-flex items-center gap-1.5 text-sm font-semibold transition hover:gap-2"
              style={{ color: 'var(--accent-teal)' }}
            >
              {t('servicesPage.viewDetails')}
              <span aria-hidden>→</span>
            </Link>
          </Card>
        ))}
        {cards.length === 0 ? (
          <Card surface="landing" className="md:col-span-3 !p-8">
            <h2 className="text-lg font-semibold" style={{ color: 'var(--ink-heading)' }}>
              {t('landingBusiness.servicesTitle')}
            </h2>
            <p className="mt-2 text-sm" style={{ color: 'var(--ink-muted)' }}>
              {t('landingBusiness.heroBody')}
            </p>
          </Card>
        ) : null}
      </div>
    </section>
  )
}
