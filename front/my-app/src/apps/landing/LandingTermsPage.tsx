import { useLanguage } from '../../context/LanguageContext'
import { useLandingPageContent } from './useLandingPageContent'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'

export function LandingTermsPage() {
  useDocumentMeta({ title: 'Terms of Service · Ziyara', description: 'Ziyara terms and conditions of use.' })
  const { t } = useLanguage()
  const { readString: pageText } = useLandingPageContent('terms')

  const lastUpdated = new Date('2026-05-31').toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })

  const sections = [
    { id: 'use',       title: t('landingBusiness.termsUseTitle'),       body: t('landingBusiness.termsUseBody') },
    { id: 'booking',   title: t('landingBusiness.termsBookingTitle'),   body: t('landingBusiness.termsBookingBody') },
    { id: 'provider',  title: t('landingBusiness.termsPartnerTitle'),   body: t('landingBusiness.termsPartnerBody') },
    { id: 'liability', title: t('landingBusiness.termsLiabilityTitle'), body: t('landingBusiness.termsLiabilityBody') },
  ]

  return (
    <section className="landing-scroll-parallax lp-sheet">
      <p className="landing-fade-up lp-eyebrow lp-eyebrow--tight">{t('footer.terms')}</p>
      <h1 className="landing-fade-up landing-fade-up-delay-1 lp-h1 mt-3">
        {pageText('title', t('landingBusiness.termsTitle'))}
      </h1>
      <p className="landing-fade-up landing-fade-up-delay-2 lp-body mt-4 max-w-[56rem]">
        {pageText('intro', t('landingBusiness.termsIntro'))}
      </p>
      <p className="lp-muted text-xs mt-2">
        {t('landingBusiness.lastUpdated', { date: lastUpdated })}
      </p>

      <div className="mt-6 grid gap-4 lg:grid-cols-[200px_1fr] lg:items-start">
        <nav className="lp-legal-toc" aria-label="Table of contents">
          {sections.map((s) => (
            <a key={s.id} href={`#terms-${s.id}`} className="lp-legal-toc__link">
              {s.title}
            </a>
          ))}
        </nav>

        <div className="space-y-4">
          {sections.map((section) => (
            <article key={section.id} id={`terms-${section.id}`} className="lp-legal-section">
              <h2>{section.title}</h2>
              <p>{section.body}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
