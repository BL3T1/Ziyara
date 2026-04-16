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
    <section className="landing-3d-stage landing-scroll-parallax relative overflow-hidden rounded-[2rem] border border-slate-200/85 bg-white/80 p-7 shadow-[0_20px_70px_-34px_rgba(15,23,42,0.3)] ring-1 ring-slate-900/[0.03] dark:border-slate-700/75 dark:bg-slate-900/55 dark:ring-white/[0.05] sm:p-10">
      <div
        className="landing-aurora landing-parallax-soft pointer-events-none absolute -left-20 top-0 h-56 w-56 rounded-full bg-primary/10 blur-3xl dark:bg-primary/20"
        aria-hidden
      />
      <div
        className="landing-aurora landing-parallax-strong pointer-events-none absolute -right-24 bottom-0 h-64 w-64 rounded-full bg-secondary/20 blur-3xl dark:bg-secondary/15"
        aria-hidden
      />
      <div className="relative z-[1]">
      <p className="landing-fade-up text-xs font-semibold uppercase tracking-[0.12em] text-secondary">
        {pageText('eyebrow', t('landingBusiness.servicesEyebrow'))}
      </p>
      <h1 className="landing-fade-up landing-fade-up-delay-1 font-display mt-3 text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100 md:text-4xl">
        {pageText('title', t('landingBusiness.servicesTitle'))}
      </h1>
      <div className="mt-8 grid gap-6 md:grid-cols-3">
        {cards.map((card) => (
          <Card key={card.id} className="landing-3d-card group/card">
            {card.imageUrl ? (
              <div className="relative mb-4 overflow-hidden rounded-xl">
                <img
                  src={card.imageUrl}
                  alt={card.name}
                  className="h-44 w-full object-cover transition duration-500 group-hover/card:scale-105"
                  loading="lazy"
                />
                <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-slate-900/35 via-transparent to-transparent" />
              </div>
            ) : null}
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-50">{card.name}</h2>
            <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
              {card.description || card.city || t('landingBusiness.servicesTitle')}
            </p>
            <Link
              to={SERVICE_TYPE_TO_LINK[card.type] ?? '/services'}
              className="mt-4 inline-flex items-center gap-1.5 text-sm font-semibold text-primary transition hover:gap-2"
            >
              {t('servicesPage.viewDetails')}
              <span aria-hidden>{'->'}</span>
            </Link>
          </Card>
        ))}
        {cards.length === 0 ? (
          <Card className="landing-3d-card md:col-span-3">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-50">{t('landingBusiness.servicesTitle')}</h2>
            <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">{t('landingBusiness.heroBody')}</p>
          </Card>
        ) : null}
      </div>
      </div>
    </section>
  )
}
