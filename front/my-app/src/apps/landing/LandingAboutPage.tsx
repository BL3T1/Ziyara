import { useLanguage } from '../../context/LanguageContext'
import { useLandingLiveData } from './useLandingLiveData'
import { useLandingPageContent } from './useLandingPageContent'

export function LandingAboutPage() {
  const { t } = useLanguage()
  const { totalCities, totalServices, averageBasePrice } = useLandingLiveData()
  const { readString: pageText } = useLandingPageContent('about')

  return (
    <section className="landing-scroll-parallax lp-sheet">
      <p className="landing-fade-up lp-eyebrow lp-eyebrow--tight">{pageText('eyebrow', t('landingBusiness.aboutEyebrow'))}</p>
      <h1 className="landing-fade-up landing-fade-up-delay-1 lp-h1" style={{ marginTop: 12 }}>
        {pageText('title', t('landingBusiness.aboutTitle'))}
      </h1>
      <p className="landing-fade-up landing-fade-up-delay-2 lp-body" style={{ marginTop: 16, maxWidth: '48rem' }}>
        {pageText('body', t('landingBusiness.aboutBody'))}
      </p>
      <div className="lp-deal-grid" style={{ marginTop: 28 }}>
        <div className="lp-city-chip">
          <p className="lp-label" style={{ margin: 0 }}>
            Cities
          </p>
          <p className="lp-value" style={{ marginTop: 8, fontSize: 22 }}>
            {totalCities || 0}+
          </p>
          <p className="lp-muted" style={{ marginTop: 6 }}>
            Active destination cities
          </p>
        </div>
        <div className="lp-city-chip">
          <p className="lp-label" style={{ margin: 0 }}>
            Listings
          </p>
          <p className="lp-value" style={{ marginTop: 8, fontSize: 22 }}>
            {totalServices || 0}+
          </p>
          <p className="lp-muted" style={{ marginTop: 6 }}>
            Live listings in the platform
          </p>
        </div>
        <div className="lp-city-chip">
          <p className="lp-label" style={{ margin: 0 }}>
            Pricing
          </p>
          <p className="lp-value" style={{ marginTop: 8, fontSize: 22 }}>
            {averageBasePrice ? `$${averageBasePrice}` : '—'}
          </p>
          <p className="lp-muted" style={{ marginTop: 6 }}>
            Average starting base price
          </p>
        </div>
      </div>
    </section>
  )
}
