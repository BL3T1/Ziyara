import { Link, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { TRUST_ICONS } from './trustIcons'
import { useLandingLiveData } from './useLandingLiveData'
import { useLandingPageContent } from './useLandingPageContent'
import { ZiyaraHeroComposition } from './ZiyaraHeroComposition'

function IconGrid() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M4 4h7v7H4V4zm9 0h7v7h-7V4zM4 13h7v7H4v-7zm9 0h7v7h-7v-7z"
        stroke="currentColor"
        strokeWidth="1.5"
      />
    </svg>
  )
}

export function LandingHomePage() {
  useDocumentMeta({ title: 'Ziyara — Discover Lebanon\'s best stays, dining & experiences', description: 'Book hotels, resorts, restaurants, trips and taxis across Lebanon with Ziyara.' })
  const { t } = useLanguage()
  const navigate = useNavigate()
  const { readString: pageText } = useLandingPageContent('home')
  const servicesBrowse = '/services'
  const [searchQuery, setSearchQuery] = useState('')
  const { services, totalServices, totalCities, averageBasePrice, popularCities } = useLandingLiveData()
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
      </section>

      {/* ── Quick search ─────────────────────────────────────────────────── */}
      <section className="lp-section lp-animate" aria-label="Quick search">
        <form
          onSubmit={(e) => {
            e.preventDefault()
            const q = searchQuery.trim()
            if (q) {
              navigate(`/services?q=${encodeURIComponent(q)}`)
            } else {
              navigate('/services')
            }
          }}
          className="flex items-center gap-2 rounded-2xl border px-4 py-3"
          style={{ borderColor: 'rgba(90,122,130,0.25)', background: 'rgba(255,255,255,0.6)', backdropFilter: 'blur(12px)' }}
        >
          <svg className="shrink-0 lp-text-muted" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" aria-hidden>
            <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
          </svg>
          <input
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={t('landingBusiness.searchPlaceholder')}
            className="flex-1 bg-transparent text-sm outline-none"
            style={{ color: 'var(--ink-heading)' }}
          />
          <button
            type="submit"
            className="lp-btn lp-btn-primary lp-btn-sm shrink-0"
          >
            {t('landingBusiness.searchBtn')}
          </button>
        </form>
      </section>

      {/* ── Stats strip (replaces fake search bar) ───────────────────────── */}
      <section className="lp-stats-strip lp-animate" aria-label="Platform statistics">
        <div className="lp-stat-item">
          <span className="lp-stat-num">{totalServices ? `${totalServices}+` : '100+'}</span>
          <span className="lp-stat-label">{t('landingBusiness.statsServicesLabel')}</span>
        </div>
        <div className="lp-stat-item">
          <span className="lp-stat-num">{totalCities ? `${totalCities}+` : '15+'}</span>
          <span className="lp-stat-label">{t('landingBusiness.statsCitiesLabel')}</span>
        </div>
        {averageBasePrice ? (
          <div className="lp-stat-item">
            <span className="lp-stat-num">${averageBasePrice}</span>
            <span className="lp-stat-label">{t('landingBusiness.statsAvgPriceLabel')}</span>
          </div>
        ) : (
          <div className="lp-stat-item">
            <span className="lp-stat-num">4.8★</span>
            <span className="lp-stat-label">{t('landingBusiness.statsRatingLabel')}</span>
          </div>
        )}
        <Link
          to={servicesBrowse}
          className="lp-btn lp-btn-primary lp-btn-sm flex items-center justify-center self-stretch"
        >
          {t('landingBusiness.statsBrowseCta')}
        </Link>
      </section>

      {/* ── Deal shortcuts ────────────────────────────────────────────────── */}
      <section className="lp-section lp-animate">
        <p className="lp-eyebrow lp-eyebrow--tight">{t('landingBusiness.dealsEyebrow')}</p>
        <h2 className="lp-hero-title" style={{ fontSize: 'clamp(1.5rem, 2.2vw, 2rem)', marginBottom: 0 }}>
          {pageText('dealsTitle', t('landingBusiness.dealsTitle'))}
        </h2>
        <div className="lp-deal-grid mt-5">
          {dealTiles.length > 0 ? dealTiles.map((deal) => (
            <Link
              key={deal.key}
              to={deal.href ?? servicesBrowse}
              className="lp-solution-card no-underline cursor-pointer"
            >
              <div className="lp-solution-icon">
                <IconGrid />
              </div>
              <div className="lp-solution-text" style={{ alignItems: 'flex-start', textAlign: 'left' }}>
                <span className="lp-solution-line" style={{ color: 'var(--accent-teal)', fontSize: 12, letterSpacing: '0.12em', textTransform: 'uppercase' }}>
                  {deal.type}
                </span>
                <span className="lp-solution-line" style={{ fontSize: 18 }}>
                  {deal.label}
                </span>
                {deal.city ? (
                  <span className="lp-solution-line" style={{ fontWeight: 500, color: '#62748e', fontSize: 13 }}>
                    {deal.city}
                  </span>
                ) : null}
                <span className="lp-solution-line" style={{ marginTop: 8, fontSize: 22, color: 'var(--accent-tan-mid)' }}>
                  {deal.price}
                </span>
              </div>
            </Link>
          )) : (
            <Link to={servicesBrowse} className="lp-solution-card no-underline cursor-pointer">
              <div className="lp-solution-icon"><IconGrid /></div>
              <div className="lp-solution-text" style={{ alignItems: 'flex-start', textAlign: 'left' }}>
                <span className="lp-solution-line" style={{ fontSize: 16 }}>{t('landingTraveler.ctaBrowse')}</span>
              </div>
            </Link>
          )}
        </div>
      </section>

      {/* ── Popular destinations ──────────────────────────────────────────── */}
      <section className="lp-sheet lp-section lp-animate mt-9">
        <p className="lp-eyebrow lp-eyebrow--tight">{t('landingBusiness.popularEyebrow')}</p>
        <h2 className="lp-h1 mt-2">
          {pageText('popularTitle', t('landingBusiness.popularTitle'))}
        </h2>
        <div className="lp-deal-grid mt-5">
          {cityTiles.map((city) => (
            <button
              key={city}
              type="button"
              onClick={() => navigate(`/services?city=${encodeURIComponent(String(city))}`)}
              className="lp-city-chip"
              style={{ cursor: 'pointer', border: 'none', textAlign: 'start', fontFamily: 'inherit' }}
            >
              {city}
            </button>
          ))}
        </div>
      </section>

      {/* ── Trust pillars ─────────────────────────────────────────────────── */}
      <section className="lp-section lp-animate">
        <p className="lp-eyebrow lp-eyebrow--tight">{t('landingBusiness.trustEyebrow')}</p>
        <h2 className="lp-hero-title" style={{ fontSize: 'clamp(1.5rem, 2.2vw, 2rem)', marginBottom: 0 }}>
          {t('landingBusiness.trustTitle')}
        </h2>
        <div className="lp-pillars mt-5">
          {trustBlocks.map((point, i) => (
            <article key={point.title} className="lp-pillar">
              <div className="lp-pillar-icon">{TRUST_ICONS[i] ?? TRUST_ICONS[0]}</div>
              <h3>{point.title}</h3>
              <p>{point.body}</p>
            </article>
          ))}
        </div>
      </section>

      {/* ── Final CTA ─────────────────────────────────────────────────────── */}
      <section className="lp-partner-band lp-section text-center">
        <h2 className="lp-hero-title" style={{ fontSize: 'clamp(1.35rem, 2vw, 1.75rem)', marginBottom: 12 }}>
          {t('landingBusiness.footerHint')}
        </h2>
        <p className="lp-hero-lede mb-6">
          {t('landingBusiness.footerNote')}
        </p>
        <div className="lp-cta justify-center">
          <Link to="/services" className="lp-btn lp-btn-primary">
            {t('landingTraveler.ctaBrowse')}
          </Link>
          <Link to="/login" className="lp-btn lp-btn-outline">
            {t('landingTraveler.ctaSignIn')}
          </Link>
        </div>
      </section>
    </>
  )
}
