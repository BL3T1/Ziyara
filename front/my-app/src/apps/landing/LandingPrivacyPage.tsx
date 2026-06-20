import { useLanguage } from '../../context/LanguageContext'
import { useLandingPageContent } from './useLandingPageContent'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'

export function LandingPrivacyPage() {
  useDocumentMeta({ title: 'Privacy Policy · Ziyara', description: 'Read the Ziyara privacy policy and learn how we protect your data.' })
  const { t } = useLanguage()
  const { readString: pageText } = useLandingPageContent('privacy')

  const lastUpdated = new Date('2026-05-31').toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })

  const sections = [
    { id: 'data',     title: t('landingBusiness.privacyDataTitle'),    body: t('landingBusiness.privacyDataBody') },
    { id: 'usage',    title: t('landingBusiness.privacyUsageTitle'),   body: t('landingBusiness.privacyUsageBody') },
    { id: 'sharing',  title: t('landingBusiness.privacySharingTitle'), body: t('landingBusiness.privacySharingBody') },
    { id: 'rights',   title: t('landingBusiness.privacyRightsTitle'),  body: t('landingBusiness.privacyRightsBody') },
  ]

  return (
    <section className="landing-scroll-parallax lp-sheet">
      <p className="landing-fade-up lp-eyebrow lp-eyebrow--tight">{t('footer.privacy')}</p>
      <h1 className="landing-fade-up landing-fade-up-delay-1 lp-h1 mt-3">
        {pageText('title', t('landingBusiness.privacyTitle'))}
      </h1>
      <p className="landing-fade-up landing-fade-up-delay-2 lp-body mt-4 max-w-[56rem]">
        {pageText('intro', t('landingBusiness.privacyIntro'))}
      </p>
      <p className="lp-muted text-xs mt-2">
        {t('landingBusiness.lastUpdated', { date: lastUpdated })}
      </p>

      <div className="mt-6 grid gap-4 lg:grid-cols-[200px_1fr] lg:items-start">
        {/* Table of contents — sticky on desktop */}
        <nav className="lp-legal-toc" aria-label="Table of contents">
          {sections.map((s) => (
            <a key={s.id} href={`#privacy-${s.id}`} className="lp-legal-toc__link">
              {s.title}
            </a>
          ))}
        </nav>

        {/* Sections */}
        <div className="space-y-4">
          {sections.map((section) => (
            <article key={section.id} id={`privacy-${section.id}`} className="lp-legal-section">
              <h2>{section.title}</h2>
              <p>{section.body}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
