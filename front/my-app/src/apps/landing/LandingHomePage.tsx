import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
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
  const { t } = useLanguage()
  const { readString: pageText } = useLandingPageContent('home')
  const servicesBrowse = '/services'
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
        {
          city: t('landingBusiness.dealCityOne'),
          site: t('landingBusiness.dealSiteOne'),
          oldPrice: '$210',
          newPrice: '$168',
          diff: '20% lower',
        },
        {
          city: t('landingBusiness.dealCityTwo'),
          site: t('landingBusiness.dealSiteTwo'),
          oldPrice: '$190',
          newPrice: '$149',
          diff: '22% lower',
        },
      ]

  return (
    <>
      <section className="landing-scroll-parallax lp-ziyara-hero" aria-labelledby="hero-heading">
        <div className="lp-ziyara-hero__layout">
          <div className="landing-fade-up lp-ziyara-hero__copy">
            <p className="lp-eyebrow lp-eyebrow--tight">{pageText('badge', t('landingTraveler.badge'))}</p>
            <h1 id="hero-heading" className="lp-hero-title">
              {pageText('heroTitle', t('landingTraveler.heroTitle'))}
            </h1>
            <p className="lp-hero-lede">{pageText('heroBody', t('landingTraveler.heroBody'))}</p>
            <div className="landing-fade-up landing-fade-up-delay-1 lp-cta">
              <Link to="/login" className="lp-btn lp-btn-primary">
                {t('landingTraveler.ctaSignIn')}
              </Link>
              <Link to={servicesBrowse} className="lp-btn lp-btn-outline">
                {t('landingTraveler.ctaBrowse')}
              </Link>
            </div>
          </div>
          <div className="landing-fade-up landing-fade-up-delay-1 lp-ziyara-hero__visual">
            <ZiyaraHeroComposition />
          </div>
        </div>
      </section>

      <section className="landing-fade-up landing-fade-up-delay-2 lp-search-strip">
        <div className="lp-search-cell">
          <p className="lp-label">{t('landingBusiness.searchDestinationLabel')}</p>
          <p className="lp-value">{t('landingBusiness.searchDestinationValue')}</p>
        </div>
        <div className="lp-search-cell">
          <p className="lp-label">{t('landingBusiness.searchDateLabel')}</p>
          <p className="lp-value">{t('landingBusiness.searchDateValue')}</p>
        </div>
        <div className="lp-search-cell">
          <p className="lp-label">{t('landingBusiness.searchGuestsLabel')}</p>
          <p className="lp-value">{t('landingBusiness.searchGuestsValue')}</p>
        </div>
        <Link to="/login" className="lp-btn lp-btn-primary lp-btn-sm flex items-center justify-center self-stretch">
          {pageText('searchCta', t('landingBusiness.searchCta'))}
        </Link>
      </section>

      <section className="lp-section lp-animate">
        <p className="lp-eyebrow lp-eyebrow--tight">{t('landingBusiness.dealsEyebrow')}</p>
        <h2 className="lp-hero-title" style={{ fontSize: 'clamp(1.5rem, 2.2vw, 2rem)', marginBottom: 0 }}>
          {pageText('dealsTitle', t('landingBusiness.dealsTitle'))}
        </h2>
        <div className="lp-deal-grid" style={{ marginTop: 20 }}>
          {dealTiles.map((deal, i) => (
            <article key={deal.city} className={`lp-solution-card lp-animate lp-animate--d${Math.min(i + 1, 6)}`}>
              <div className="lp-solution-icon">
                <IconGrid />
              </div>
              <div className="lp-solution-text" style={{ alignItems: 'flex-start', textAlign: 'left' }}>
                <span className="lp-solution-line" style={{ color: 'var(--accent-teal)', fontSize: 12, letterSpacing: '0.12em', textTransform: 'uppercase' }}>
                  {deal.diff}
                </span>
                <span className="lp-solution-line" style={{ fontSize: 18 }}>
                  {deal.city}
                </span>
                <span className="lp-solution-line" style={{ fontWeight: 500, color: '#62748e', fontSize: 13 }}>
                  {deal.site}
                </span>
                <span className="lp-solution-line" style={{ marginTop: 8, fontSize: 22, color: 'var(--accent-tan-mid)' }}>
                  {deal.newPrice}{' '}
                  <span style={{ textDecoration: 'line-through', fontSize: 14, color: '#62748e' }}>{deal.oldPrice}</span>
                </span>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="lp-sheet lp-section lp-animate" style={{ marginTop: 36 }}>
        <p className="lp-eyebrow lp-eyebrow--tight">{t('landingBusiness.popularEyebrow')}</p>
        <h2 className="lp-h1" style={{ marginTop: 8 }}>
          {pageText('popularTitle', t('landingBusiness.popularTitle'))}
        </h2>
        <div className="lp-deal-grid" style={{ marginTop: 20 }}>
          {cityTiles.map((city) => (
            <div key={city} className="lp-city-chip">
              {city}
            </div>
          ))}
        </div>
      </section>

      <section className="lp-section lp-animate">
        <p className="lp-eyebrow lp-eyebrow--tight">{t('landingBusiness.trustEyebrow')}</p>
        <h2 className="lp-hero-title" style={{ fontSize: 'clamp(1.5rem, 2.2vw, 2rem)', marginBottom: 0 }}>
          {t('landingBusiness.trustTitle')}
        </h2>
        <div className="lp-pillars" style={{ marginTop: 20 }}>
          {trustBlocks.map((point, i) => (
            <article key={point.title} className={`lp-pillar lp-animate lp-animate--d${i + 1}`}>
              <div className="lp-pillar-icon">{TRUST_ICONS[i] ?? TRUST_ICONS[0]}</div>
              <h3>{point.title}</h3>
              <p>{point.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="lp-partner-band lp-section lp-animate">
        <h2 className="lp-hero-title" style={{ fontSize: 'clamp(1.35rem, 2vw, 1.75rem)', marginBottom: 12 }}>
          {t('landingBusiness.partnerTitle')}
        </h2>
        <p className="lp-hero-lede" style={{ marginBottom: 24 }}>
          {t('landingBusiness.partnerBody')}
        </p>
        <div className="lp-cta">
          <Link to="/login" className="lp-btn lp-btn-primary">
            {t('landingBusiness.partnerCustomerCta')}
          </Link>
          <Link to={partnerContact} className="lp-btn lp-btn-outline">
            {t('landingBusiness.partnerProviderCta')}
          </Link>
        </div>
      </section>

      <p className="lp-section lp-muted" style={{ textAlign: 'center', marginTop: 32 }}>
        {t('landingBusiness.footerHint')}
      </p>
      <p className="lp-muted" style={{ textAlign: 'center', marginTop: 8 }}>
        {t('landingBusiness.footerNote')}
      </p>
    </>
  )
}
