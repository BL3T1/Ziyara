import { Link } from 'react-router-dom'
import { VITE_COMPANY_APP_URL } from '../../config/appSurface'
import { Card } from '../../components/Card'
import { useLanguage } from '../../context/LanguageContext'
import { LandingHeroArt } from './LandingHeroArt'
import { TRUST_ICONS } from './trustIcons'
import { useLandingLiveData } from './useLandingLiveData'
import { useLandingPageContent } from './useLandingPageContent'

function joinBase(base: string, path: string): string {
  const b = base.replace(/\/$/, '')
  const p = path.startsWith('/') ? path : `/${path}`
  return b ? `${b}${p}` : p
}

export function LandingHomePage() {
  const { t } = useLanguage()
  const { readString: pageText } = useLandingPageContent('home')
  const customerLogin = joinBase(VITE_COMPANY_APP_URL, '/login')
  const partnerContact = '/contact'
  const { services, totalServices, totalCities, averageBasePrice, popularCities } = useLandingLiveData()
  const priceDeals = [
    ...services
      .filter((item) => typeof item.basePrice === 'number' && item.basePrice > 0)
      .slice(0, 4)
      .map((item) => {
        const current = Number(item.basePrice ?? 0)
        const old = Math.round(current * 1.2)
        return {
          city: item.city || item.name,
          site: item.type,
          oldPrice: `$${old}`,
          newPrice: `$${current}`,
          diff: '20% lower',
        }
      }),
  ]
  const trustBlocks = [
    {
      title: t('landingBusiness.trustOneTitle'),
      body: `${totalServices || 0} active listings from the database right now.`,
    },
    {
      title: t('landingBusiness.trustTwoTitle'),
      body: `${totalCities || 0} cities currently covered by partner offerings.`,
    },
    {
      title: t('landingBusiness.trustThreeTitle'),
      body: averageBasePrice ? `Average starting price: $${averageBasePrice}.` : t('landingBusiness.trustThreeBody'),
    },
  ]
  const cityTiles = popularCities.length
    ? popularCities
    : [t('landingBusiness.cityOne'), t('landingBusiness.cityTwo'), t('landingBusiness.cityThree')]
  const dealTiles = priceDeals.length
    ? priceDeals
    : [
        { city: t('landingBusiness.dealCityOne'), site: t('landingBusiness.dealSiteOne'), oldPrice: '$210', newPrice: '$168', diff: '20% lower' },
        { city: t('landingBusiness.dealCityTwo'), site: t('landingBusiness.dealSiteTwo'), oldPrice: '$190', newPrice: '$149', diff: '22% lower' },
      ]

  return (
    <>
      <section className="landing-3d-stage landing-scroll-parallax relative overflow-hidden rounded-[2rem] border border-slate-200/90 bg-gradient-to-br from-white via-slate-50/95 to-primary/[0.08] p-8 shadow-[0_30px_80px_-28px_rgba(15,23,42,0.24)] ring-1 ring-slate-900/[0.04] dark:border-slate-700/70 dark:from-slate-900/95 dark:via-slate-950/90 dark:to-primary/[0.18] dark:shadow-[0_35px_90px_-35px_rgba(0,0,0,0.75)] dark:ring-white/[0.06] sm:p-10 lg:grid lg:min-h-[min(30rem,72vh)] lg:grid-cols-[1.05fr_minmax(0,1fr)] lg:items-center lg:gap-12 lg:p-12 xl:gap-16">
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.45] dark:opacity-[0.25]"
          style={{
            backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%231e4d6b' fill-opacity='0.06'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`,
          }}
          aria-hidden
        />
        <div
          className="landing-aurora landing-parallax-strong pointer-events-none absolute -right-24 -top-14 h-72 w-72 rounded-full bg-secondary/20 blur-3xl dark:bg-secondary/15"
          aria-hidden
        />
        <div className="relative z-[1]">
          <p className="landing-fade-up inline-flex items-center rounded-full border border-secondary/35 bg-secondary/10 px-3 py-1 text-xs font-bold uppercase tracking-[0.14em] text-secondary dark:bg-secondary/15 dark:text-secondary">
            {pageText('badge', t('landingBusiness.badge'))}
          </p>
          <h1 className="landing-fade-up landing-fade-up-delay-1 font-display mt-5 max-w-2xl text-4xl font-semibold leading-[1.08] tracking-tight text-slate-900 md:text-5xl md:leading-[1.04] lg:text-[3.4rem] dark:text-slate-50">
            {pageText('heroTitle', t('landingBusiness.heroTitle'))}
          </h1>
          <p className="landing-fade-up landing-fade-up-delay-2 mt-6 max-w-xl text-lg leading-relaxed text-slate-600 dark:text-slate-400">
            {pageText('heroBody', t('landingBusiness.heroBody'))}
          </p>
          <div className="landing-fade-up landing-fade-up-delay-3 mt-10 flex flex-wrap gap-3">
            <a
              href={customerLogin}
              className="inline-flex rounded-2xl bg-gradient-to-br from-primary to-primary/90 px-6 py-3.5 text-sm font-semibold text-white shadow-lg shadow-primary/25 ring-1 ring-white/15 transition hover:brightness-105 active:scale-[0.98]"
            >
              {t('landingBusiness.ctaPrimary')}
            </a>
            <Link
              to={partnerContact}
              className="inline-flex rounded-2xl border border-slate-300/90 bg-white/90 px-6 py-3.5 text-sm font-semibold text-slate-800 shadow-sm backdrop-blur-sm transition hover:border-primary/35 hover:bg-white dark:border-slate-600 dark:bg-slate-800/85 dark:text-slate-100 dark:hover:border-primary/45"
            >
              {t('landingBusiness.ctaSecondary')}
            </Link>
          </div>
          <div className="mt-8 grid max-w-xl grid-cols-3 gap-3">
            <div className="landing-3d-card rounded-2xl border border-slate-200/80 bg-white/80 p-3 text-center shadow-sm dark:border-slate-700/70 dark:bg-slate-900/60">
              <p className="text-lg font-bold text-primary">24/7</p>
              <p className="text-xs text-slate-600 dark:text-slate-400">Support</p>
            </div>
            <div className="landing-3d-card rounded-2xl border border-slate-200/80 bg-white/80 p-3 text-center shadow-sm dark:border-slate-700/70 dark:bg-slate-900/60">
              <p className="text-lg font-bold text-primary">150+</p>
              <p className="text-xs text-slate-600 dark:text-slate-400">Partners</p>
            </div>
            <div className="landing-3d-card rounded-2xl border border-slate-200/80 bg-white/80 p-3 text-center shadow-sm dark:border-slate-700/70 dark:bg-slate-900/60">
              <p className="text-lg font-bold text-primary">99%</p>
              <p className="text-xs text-slate-600 dark:text-slate-400">Happy Trips</p>
            </div>
          </div>
        </div>
        <div className="relative z-[1] mt-12 flex justify-center lg:mt-0 lg:justify-end">
          <div className="landing-fade-up landing-fade-up-delay-2 landing-mouse-tilt relative w-full max-w-[22rem] sm:max-w-md">
            <div className="landing-aurora absolute -inset-4 rounded-[2rem] bg-gradient-to-br from-primary/20 via-transparent to-secondary/25 blur-2xl dark:from-primary/30 dark:to-secondary/20" aria-hidden />
            <LandingHeroArt />
          </div>
        </div>
      </section>

      <section className="landing-3d-stage mt-10 rounded-[2rem] border border-slate-200/85 bg-white/85 p-5 shadow-[0_20px_60px_-28px_rgba(15,23,42,0.24)] dark:border-slate-700/75 dark:bg-slate-900/60 sm:p-6">
        <div className="grid gap-3 lg:grid-cols-[1.2fr_1fr_0.9fr_auto]">
          <div className="rounded-2xl border border-slate-200/80 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
            <p className="text-xs font-semibold uppercase tracking-[0.11em] text-slate-500 dark:text-slate-400">{t('landingBusiness.searchDestinationLabel')}</p>
            <p className="mt-1 text-sm font-semibold text-slate-800 dark:text-slate-100">{t('landingBusiness.searchDestinationValue')}</p>
          </div>
          <div className="rounded-2xl border border-slate-200/80 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
            <p className="text-xs font-semibold uppercase tracking-[0.11em] text-slate-500 dark:text-slate-400">{t('landingBusiness.searchDateLabel')}</p>
            <p className="mt-1 text-sm font-semibold text-slate-800 dark:text-slate-100">{t('landingBusiness.searchDateValue')}</p>
          </div>
          <div className="rounded-2xl border border-slate-200/80 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
            <p className="text-xs font-semibold uppercase tracking-[0.11em] text-slate-500 dark:text-slate-400">{t('landingBusiness.searchGuestsLabel')}</p>
            <p className="mt-1 text-sm font-semibold text-slate-800 dark:text-slate-100">{t('landingBusiness.searchGuestsValue')}</p>
          </div>
          <a
            href={customerLogin}
            className="inline-flex min-h-[4.5rem] items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-primary/90 px-6 text-sm font-semibold text-white shadow-lg shadow-primary/20 transition hover:brightness-105"
          >
            {pageText('searchCta', t('landingBusiness.searchCta'))}
          </a>
        </div>
      </section>

      <section className="mt-12">
        <div className="flex items-end justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-secondary">Deal shortcuts</p>
            <h2 className="font-display mt-2 text-3xl font-semibold tracking-tight text-slate-900 dark:text-slate-100">
              {pageText('dealsTitle', t('landingBusiness.dealsTitle'))}
            </h2>
          </div>
        </div>
        <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {dealTiles.map((deal) => (
            <article
              key={deal.city}
              className="landing-3d-card rounded-2xl border border-slate-200/85 bg-white/90 p-5 shadow-md shadow-slate-900/[0.05] dark:border-slate-700/70 dark:bg-slate-900/70"
            >
              <p className="text-xs font-semibold uppercase tracking-[0.11em] text-emerald-700 dark:text-emerald-400">{deal.diff}</p>
              <h3 className="mt-2 text-lg font-semibold text-slate-900 dark:text-slate-100">{deal.city}</h3>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{deal.site}</p>
              <div className="mt-5 flex items-end justify-between">
                <p className="text-sm text-slate-500 line-through dark:text-slate-500">{deal.oldPrice}</p>
                <p className="text-2xl font-bold text-primary">{deal.newPrice}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="mt-12 rounded-[2rem] border border-slate-200/85 bg-gradient-to-br from-white via-slate-50 to-secondary/[0.1] p-7 dark:border-slate-700/70 dark:from-slate-900/80 dark:via-slate-900 dark:to-secondary/[0.14] sm:p-10">
        <p className="text-xs font-semibold uppercase tracking-[0.14em] text-secondary">Popular searches</p>
        <h2 className="font-display mt-2 text-3xl font-semibold tracking-tight text-slate-900 dark:text-slate-100">
          {pageText('popularTitle', t('landingBusiness.popularTitle'))}
        </h2>
        <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {cityTiles.map((city) => (
            <div
              key={city}
              className="landing-3d-card rounded-2xl border border-slate-200/85 bg-white/85 px-4 py-3 text-sm font-semibold text-slate-800 dark:border-slate-700 dark:bg-slate-900/75 dark:text-slate-100"
            >
              {city}
            </div>
          ))}
        </div>
      </section>

      <section className="mt-20 md:mt-24">
        <p className="text-xs font-semibold uppercase tracking-[0.14em] text-secondary">{t('landingBusiness.trustEyebrow')}</p>
        <h2 className="font-display mt-3 max-w-2xl text-3xl font-semibold tracking-tight text-slate-900 dark:text-slate-100">
          {t('landingBusiness.trustTitle')}
        </h2>
        <div className="mt-8 grid gap-6 md:grid-cols-3">
          {trustBlocks.map((point, i) => (
            <Card key={point.title} className="landing-3d-card pt-7">
              <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-primary/12 to-secondary/15 ring-1 ring-primary/15 dark:from-primary/25 dark:to-secondary/20 dark:ring-white/10">
                {TRUST_ICONS[i] ?? TRUST_ICONS[0]}
              </div>
              <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-50">{point.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-slate-600 dark:text-slate-400">{point.body}</p>
            </Card>
          ))}
        </div>
      </section>

      <section className="landing-3d-stage relative mt-20 overflow-hidden rounded-[2rem] border border-primary/20 bg-gradient-to-br from-primary/[0.11] via-white to-secondary/[0.14] p-8 shadow-xl shadow-slate-900/[0.08] dark:border-primary/25 dark:from-primary/[0.2] dark:via-slate-900/80 dark:to-secondary/[0.17] dark:shadow-black/40 sm:p-10 md:mt-24">
        <div
          className="landing-aurora pointer-events-none absolute -right-16 top-0 h-64 w-64 rounded-full bg-secondary/25 blur-3xl dark:bg-secondary/15"
          aria-hidden
        />
        <div className="relative z-[1]">
          <h2 className="font-display max-w-2xl text-2xl font-semibold tracking-tight text-slate-900 dark:text-slate-100 md:text-3xl">
            {t('landingBusiness.partnerTitle')}
          </h2>
          <p className="mt-4 max-w-3xl text-base leading-relaxed text-slate-700 dark:text-slate-300">
            {t('landingBusiness.partnerBody')}
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <a
              href={customerLogin}
              className="inline-flex rounded-2xl bg-gradient-to-br from-primary to-primary/90 px-6 py-3.5 text-sm font-semibold text-white shadow-lg shadow-primary/25 ring-1 ring-white/15 transition hover:brightness-105 active:scale-[0.98]"
            >
              {t('landingBusiness.partnerCustomerCta')}
            </a>
            <Link
              to={partnerContact}
              className="inline-flex rounded-2xl border border-slate-300/90 bg-white/90 px-6 py-3.5 text-sm font-semibold text-slate-800 shadow-sm backdrop-blur-sm transition hover:border-primary/35 hover:bg-white dark:border-slate-600 dark:bg-slate-800/85 dark:text-slate-100 dark:hover:border-primary/45"
            >
              {t('landingBusiness.partnerProviderCta')}
            </Link>
          </div>
        </div>
      </section>

      <p className="mt-16 text-center text-sm text-slate-600 dark:text-slate-500">{t('landingBusiness.footerHint')}</p>
      <p className="mt-3 text-center text-xs text-slate-500 dark:text-slate-600">{t('landingBusiness.footerNote')}</p>
    </>
  )
}
