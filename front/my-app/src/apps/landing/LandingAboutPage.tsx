import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { useLandingLiveData } from './useLandingLiveData'
import { useLandingPageContent } from './useLandingPageContent'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'

const StepIcons = [
  <svg key="search" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>,
  <svg key="check" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden><polyline points="20 6 9 17 4 12"/></svg>,
  <svg key="star" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>,
]

export function LandingAboutPage() {
  useDocumentMeta({ title: 'About Us · Ziyara', description: "Learn about Ziyara and our mission to connect travellers with Lebanon's best experiences." })
  const { t } = useLanguage()
  const { totalCities, totalServices, averageBasePrice, loading } = useLandingLiveData()
  const { readString: pageText } = useLandingPageContent('about')

  const stats = [
    { label: t('landingAbout.statCitiesLabel'),   value: loading ? '…' : (totalCities   ? `${totalCities}+`   : '—'), desc: t('landingAbout.statCitiesDesc') },
    { label: t('landingAbout.statListingsLabel'), value: loading ? '…' : (totalServices ? `${totalServices}+` : '—'), desc: t('landingAbout.statListingsDesc') },
    { label: t('landingAbout.statPriceLabel'),    value: loading ? '…' : (averageBasePrice ? `$${averageBasePrice}` : '—'), desc: t('landingAbout.statPriceDesc') },
  ]

  const steps = [
    { title: t('landingAbout.step1Title'), body: t('landingAbout.step1Body') },
    { title: t('landingAbout.step2Title'), body: t('landingAbout.step2Body') },
    { title: t('landingAbout.step3Title'), body: t('landingAbout.step3Body') },
  ]

  return (
    <section className="landing-scroll-parallax lp-sheet">
      <p className="landing-fade-up lp-eyebrow lp-eyebrow--tight">{pageText('eyebrow', t('landingBusiness.aboutEyebrow'))}</p>
      <h1 className="landing-fade-up landing-fade-up-delay-1 lp-h1 mt-3">
        {pageText('title', t('landingBusiness.aboutTitle'))}
      </h1>
      <p className="landing-fade-up landing-fade-up-delay-2 lp-body mt-4 max-w-[48rem]">
        {pageText('body', t('landingBusiness.aboutBody'))}
      </p>

      {/* Platform stats */}
      <div className="lp-pillars lp-animate mt-8">
        {stats.map((stat) => (
          <div key={stat.label} className="lp-pillar">
            <p className="lp-label mb-3">{stat.label}</p>
            <p style={{ margin: 0, fontSize: 'clamp(1.6rem, 3vw, 2.2rem)', fontWeight: 700, letterSpacing: '-0.03em', color: 'var(--ink-heading)' }}>
              {stat.value}
            </p>
            <p className="lp-muted mt-2">{stat.desc}</p>
          </div>
        ))}
      </div>

      {/* How it works */}
      <div className="lp-section lp-animate">
        <p className="lp-eyebrow lp-eyebrow--tight">{t('landingAbout.howEyebrow')}</p>
        <h2 className="lp-h1 mt-2">{t('landingAbout.howTitle')}</h2>
        <div className="lp-pillars mt-5">
          {steps.map((step, i) => (
            <div key={step.title} className="lp-pillar">
              <div className="lp-pillar-icon lp-text-accent">
                {StepIcons[i]}
              </div>
              <h3>{step.title}</h3>
              <p>{step.body}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Closing CTA */}
      <div className="lp-animate" style={{ marginTop: 40, textAlign: 'center' }}>
        <Link to="/services" className="lp-btn lp-btn-primary">
          {t('landingTraveler.ctaBrowse')}
        </Link>
      </div>
    </section>
  )
}
