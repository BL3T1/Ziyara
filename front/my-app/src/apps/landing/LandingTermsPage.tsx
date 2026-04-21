import { useLanguage } from '../../context/LanguageContext'
import { useLandingLiveData } from './useLandingLiveData'
import { useLandingPageContent } from './useLandingPageContent'

export function LandingTermsPage() {
  const { t } = useLanguage()
  const { services } = useLandingLiveData()
  const { readString: pageText } = useLandingPageContent('terms')
  const lastUpdated = services
    .map((s) => s.updatedAt || s.createdAt)
    .filter(Boolean)
    .sort()
    .reverse()[0]

  const sections = [
    {
      title: t('landingBusiness.termsUseTitle'),
      body: t('landingBusiness.termsUseBody'),
    },
    {
      title: t('landingBusiness.termsBookingTitle'),
      body: t('landingBusiness.termsBookingBody'),
    },
    {
      title: t('landingBusiness.termsPartnerTitle'),
      body: t('landingBusiness.termsPartnerBody'),
    },
    {
      title: t('landingBusiness.termsLiabilityTitle'),
      body: t('landingBusiness.termsLiabilityBody'),
    },
  ]

  return (
    <section className="landing-scroll-parallax lp-sheet">
      <p className="landing-fade-up lp-eyebrow lp-eyebrow--tight">{t('footer.terms')}</p>
      <h1 className="landing-fade-up landing-fade-up-delay-1 lp-h1" style={{ marginTop: 12 }}>
        {pageText('title', t('landingBusiness.termsTitle'))}
      </h1>
      <p className="landing-fade-up landing-fade-up-delay-2 lp-body" style={{ marginTop: 16, maxWidth: '56rem' }}>
        {pageText('intro', t('landingBusiness.termsIntro'))}
      </p>
      <p className="lp-muted" style={{ marginTop: 8 }}>
        Last data refresh: {lastUpdated ? new Date(lastUpdated).toLocaleDateString() : 'N/A'}
      </p>
      <div className="mt-8 space-y-4">
        {sections.map((section) => (
          <article key={section.title} className="lp-city-chip" style={{ textAlign: 'start' }}>
            <h2 className="text-lg font-semibold" style={{ color: 'var(--ink-heading)', margin: 0 }}>
              {section.title}
            </h2>
            <p className="mt-2 text-sm leading-relaxed" style={{ color: 'var(--ink-muted)', margin: 0 }}>
              {section.body}
            </p>
          </article>
        ))}
      </div>
    </section>
  )
}
