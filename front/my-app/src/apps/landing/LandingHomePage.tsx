import { Link, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { TRUST_ICONS } from './trustIcons'
import { useLandingLiveData } from './useLandingLiveData'
import { useLandingPageContent } from './useLandingPageContent'
import { ZiyaraHeroComposition } from './ZiyaraHeroComposition'

export function LandingHomePage() {
  useDocumentMeta({ title: 'Ziyara — Discover Syria\'s best stays, dining & experiences', description: 'Book hotels, resorts, restaurants, trips and taxis across Syria with Ziyara.' })
  const { t } = useLanguage()
  const navigate = useNavigate()
  const { readString: pageText } = useLandingPageContent('home')
  const servicesBrowse = '/services'
  const [searchQuery, setSearchQuery] = useState('')
  const { services, popularCities } = useLandingLiveData()
  const TYPE_TO_SLUG: Record<string, string> = {
    HOTEL: 'hotels', RESORT: 'resorts', RESTAURANT: 'restaurants', TAXI: 'taxis', TRIP: 'trips',
  }
  const priceDeals = services
    .filter((item) => typeof item.basePrice === 'number' && item.basePrice > 0)
    .slice(0, 4)
    .map((item) => {
      const slug = TYPE_TO_SLUG[item.type ?? ''] ?? 'hotels'
      return {
        key: item.id,
        label: item.name,
        city: item.city || '',
        type: item.type ?? '',
        price: `${item.currency ?? 'USD'} ${Number(item.basePrice).toLocaleString()}`,
        href: `/${slug}/${item.id}`,
      }
    })
  const trustBlocks = [
    {
      title: t('landingBusiness.trustOneTitle'),
      body: t('landingBusiness.trustOneBody'),
    },
    {
      title: t('landingBusiness.trustTwoTitle'),
      body: t('landingBusiness.trustTwoBody'),
    },
    {
      title: t('landingBusiness.trustThreeTitle'),
      body: t('landingBusiness.trustThreeBody'),
    },
  ]
  const cityTiles = popularCities.length
    ? popularCities
    : [t('landingBusiness.cityOne'), t('landingBusiness.cityTwo'), t('landingBusiness.cityThree')]
  const dealTiles = priceDeals

  return (
    <>
      {/* ── Hero ─────────────────────────────────────────────────────────── */}
      <section className="landing-scroll-parallax lp-ziyara-hero" aria-labelledby="hero-heading">
        <div className="lp-ziyara-hero__layout">
          <div className="lp-ziyara-hero__copy">
            <p className="lp-eyebrow lp-eyebrow--tight">{pageText('badge', t('landingTraveler.badge'))}</p>
            <h1 id="hero-heading" className="lp-hero-title">
              {pageText('heroTitle', t('landingTraveler.heroTitle'))}
            </h1>
            <p className="lp-hero-lede">{pageText('heroBody', t('landingTraveler.heroBody'))}</p>
            <div className="lp-cta">
              <Link to="/login" className="lp-btn lp-btn-primary">
                {t('landingTraveler.ctaSignIn')}
              </Link>
              <Link to={servicesBrowse} className="lp-btn lp-btn-outline">
                {t('landingTraveler.ctaBrowse')}
              </Link>
            </div>
          </div>
          <div className="lp-ziyara-hero__visual">
            <ZiyaraHeroComposition />
          </div>
        </div>

        {/* ── Stitch glass-panel search bar inside hero ──────────────────── */}
        <div className="relative z-10 mx-auto max-w-[860px] px-4 -mb-7 mt-8 md:mt-12">
          <form
            onSubmit={(e) => {
              e.preventDefault()
              const q = searchQuery.trim()
              navigate(q ? `/services?q=${encodeURIComponent(q)}` : '/services')
            }}
            className="glass-panel rounded-2xl flex flex-col md:flex-row items-stretch md:items-center gap-0 overflow-hidden shadow-xl"
          >
            {/* Destination */}
            <div className="flex items-center gap-3 flex-1 px-5 py-3.5 border-b md:border-b-0 md:border-r border-outline-variant/20">
              <svg className="text-outline shrink-0" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
              </svg>
              <input
                type="search"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={t('landingBusiness.searchPlaceholder')}
                aria-label={t('landingBusiness.searchPlaceholder')}
                className="flex-1 bg-transparent border-none outline-none font-body-md text-body-md text-on-surface placeholder:text-outline min-w-0"
              />
            </div>
            {/* City */}
            <div className="flex items-center gap-3 flex-1 px-5 py-3.5 border-b md:border-b-0 md:border-r border-outline-variant/20">
              <svg className="text-outline shrink-0" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/><circle cx="12" cy="9" r="2.5"/>
              </svg>
              <input type="text" placeholder="City or region" aria-label="City or region"
                className="flex-1 bg-transparent border-none outline-none font-body-md text-body-md text-on-surface placeholder:text-outline min-w-0" />
            </div>
            {/* Dates */}
            <div className="flex items-center gap-3 flex-1 px-5 py-3.5 border-b md:border-b-0 md:border-r border-outline-variant/20">
              <svg className="text-outline shrink-0" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/>
              </svg>
              <input type="text" placeholder="Any dates" aria-label="Any dates"
                className="flex-1 bg-transparent border-none outline-none font-body-md text-body-md text-on-surface placeholder:text-outline min-w-0" />
            </div>
            {/* Submit */}
            <button type="submit"
              className="m-2 px-6 py-3.5 bg-stitch-primary text-on-primary rounded-xl font-label-md text-label-md font-bold hover:opacity-90 transition-opacity whitespace-nowrap shadow-md">
              {t('landingBusiness.searchBtn')}
            </button>
          </form>
        </div>
      </section>

      {/* ── Stitch Stats Strip ───────────────────────────────────────────── */}
      <section className="lp-section lp-animate px-4">
        <div
          className="w-full bg-surface-container-lowest rounded-3xl p-8 md:p-12 border border-outline-variant/20 flex flex-col md:flex-row justify-between items-center gap-8 relative overflow-hidden"
          style={{ boxShadow: '0 8px 32px -12px rgba(163,95,32,0.1)' }}
        >
          {/* decorative glow */}
          <div
            className="absolute top-0 right-0 w-72 h-72 rounded-full -translate-y-1/2 translate-x-1/2"
            style={{ background: 'radial-gradient(circle, rgba(135,82,0,0.06), transparent)' }}
            aria-hidden
          />
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 md:gap-12 w-full flex-grow z-10">
            {[
              { value: '120+', label: t('landingBusiness.catPremiumStays') || 'Premium Stays' },
              { value: '45+',  label: t('landingBusiness.catDining')       || 'Curated Dining' },
              { value: '14',   label: 'Major Cities' },
              { value: '24/7', label: t('landingBusiness.catConcierge')    || 'Concierge' },
            ].map(({ value, label }) => (
              <div key={value} className="text-left">
                <span className="text-gradient-gold font-headline-lg text-[42px] md:text-[48px] font-extrabold block leading-tight">
                  {value}
                </span>
                <span className="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider text-xs mt-1 block">
                  {label}
                </span>
              </div>
            ))}
          </div>
          <button
            type="button"
            onClick={() => navigate('/services')}
            className="shrink-0 z-10 px-6 py-3 rounded-xl font-label-md text-label-md font-semibold bg-surface-variant text-on-surface border border-outline-variant/30 hover:bg-primary-container hover:text-white hover:border-primary-container transition-all whitespace-nowrap"
          >
            {t('landingBusiness.browsePartners') || 'Browse All Partners'}
          </button>
        </div>
      </section>

      {/* ── Featured listings ─────────────────────────────────────────────── */}
      {dealTiles.length > 0 && (
        <section className="lp-section lp-animate">
          <p className="font-eyebrow text-eyebrow text-stitch-primary uppercase tracking-[0.18em] mb-2">
            {t('landingBusiness.dealsEyebrow')}
          </p>
          <h2 className="font-headline-lg text-headline-lg text-on-surface mb-6">
            {pageText('dealsTitle', t('landingBusiness.dealsTitle'))}
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-gutter">
            {dealTiles.map((deal) => (
              <Link
                key={deal.key}
                to={deal.href ?? servicesBrowse}
                className="no-underline flex flex-col gap-3 bg-surface-container-lowest rounded-xl border border-outline-variant/20 p-5 hover:shadow-lg hover:border-primary-container/30 transition-all group"
              >
                <span className="font-eyebrow text-eyebrow text-on-surface-variant uppercase tracking-[0.14em] text-[11px]">
                  {deal.type}
                </span>
                <div>
                  <p className="font-headline-md text-headline-md text-on-surface group-hover:text-stitch-primary transition-colors line-clamp-2">
                    {deal.label}
                  </p>
                  {deal.city && (
                    <p className="font-body-md text-body-md text-on-surface-variant text-sm mt-1">{deal.city}</p>
                  )}
                </div>
                <p className="font-label-md text-label-md text-primary-container font-bold text-lg mt-auto">
                  {deal.price}
                </p>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* ── Popular destinations ──────────────────────────────────────────── */}
      <section className="lp-section lp-animate bg-surface-container-low rounded-3xl px-8 md:px-12 py-10">
        <p className="font-eyebrow text-eyebrow text-stitch-primary uppercase tracking-[0.18em] mb-2">
          {t('landingBusiness.popularEyebrow')}
        </p>
        <h2 className="font-headline-lg text-headline-lg text-on-surface mb-6">
          {pageText('popularTitle', t('landingBusiness.popularTitle'))}
        </h2>
        <div className="flex flex-wrap gap-3">
          {cityTiles.map((city) => (
            <button
              key={city}
              type="button"
              onClick={() => navigate(`/services?city=${encodeURIComponent(String(city))}`)}
              className="px-5 py-2.5 rounded-full bg-surface-container-lowest border border-outline-variant/40 font-body-md text-body-md text-on-surface hover:bg-stitch-primary hover:text-white hover:border-stitch-primary transition-all text-sm font-medium"
            >
              {city}
            </button>
          ))}
        </div>
      </section>

      {/* ── Why Ziyara (trust pillars) ────────────────────────────────────── */}
      <section className="lp-section lp-animate">
        <p className="font-eyebrow text-eyebrow text-stitch-primary uppercase tracking-[0.18em] mb-2">
          {t('landingBusiness.trustEyebrow')}
        </p>
        <h2 className="font-headline-lg text-headline-lg text-on-surface mb-6">
          {t('landingBusiness.trustTitle')}
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-gutter">
          {trustBlocks.map((point, i) => (
            <article
              key={point.title}
              className="bg-surface-container-lowest rounded-xl border border-outline-variant/20 p-6 flex flex-col gap-4"
            >
              <div className="w-10 h-10 rounded-xl flex items-center justify-center text-primary-container bg-primary-container/10">
                {TRUST_ICONS[i] ?? TRUST_ICONS[0]}
              </div>
              <h3 className="font-headline-md text-headline-md text-on-surface">{point.title}</h3>
              <p className="font-body-md text-body-md text-on-surface-variant">{point.body}</p>
            </article>
          ))}
        </div>
      </section>

      {/* ── Final CTA band ────────────────────────────────────────────────── */}
      <section className="lp-section lp-animate px-4">
        <div
          className="w-full rounded-3xl p-10 md:p-16 text-center"
          style={{ background: 'linear-gradient(135deg, #875200 0%, #C8893A 100%)' }}
        >
          <h2 className="font-hero-h1 text-hero-h1 text-white mb-4">
            {t('landingBusiness.footerHint')}
          </h2>
          <p className="font-body-lg text-body-lg text-white/80 max-w-xl mx-auto mb-8">
            {t('landingBusiness.footerNote')}
          </p>
          <div className="flex items-center justify-center gap-4 flex-wrap">
            <Link
              to="/services"
              className="no-underline bg-white text-stitch-primary font-label-md text-label-md px-8 py-3 rounded-xl hover:bg-surface transition-colors font-bold shadow-md"
            >
              {t('landingTraveler.ctaBrowse')}
            </Link>
            <Link
              to="/login"
              className="no-underline text-white border-2 border-white/60 font-label-md text-label-md px-8 py-3 rounded-xl hover:bg-white/10 transition-colors"
            >
              {t('landingTraveler.ctaSignIn')}
            </Link>
          </div>
        </div>
      </section>
    </>
  )
}
