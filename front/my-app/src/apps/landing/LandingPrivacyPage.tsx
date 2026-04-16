import { useLanguage } from '../../context/LanguageContext'
import { useLandingLiveData } from './useLandingLiveData'
import { useLandingPageContent } from './useLandingPageContent'

export function LandingPrivacyPage() {
  const { t } = useLanguage()
  const { services } = useLandingLiveData()
  const { readString: pageText } = useLandingPageContent('privacy')
  const lastUpdated = services
    .map((s) => s.updatedAt || s.createdAt)
    .filter(Boolean)
    .sort()
    .reverse()[0]

  const sections = [
    {
      title: t('landingBusiness.privacyDataTitle'),
      body: t('landingBusiness.privacyDataBody'),
    },
    {
      title: t('landingBusiness.privacyUsageTitle'),
      body: t('landingBusiness.privacyUsageBody'),
    },
    {
      title: t('landingBusiness.privacySharingTitle'),
      body: t('landingBusiness.privacySharingBody'),
    },
    {
      title: t('landingBusiness.privacyRightsTitle'),
      body: t('landingBusiness.privacyRightsBody'),
    },
  ]

  return (
    <section className="landing-3d-stage landing-scroll-parallax relative overflow-hidden rounded-[2rem] border border-slate-200/85 bg-white/85 p-7 shadow-[0_20px_70px_-34px_rgba(15,23,42,0.3)] ring-1 ring-slate-900/[0.03] dark:border-slate-700/75 dark:bg-slate-900/55 dark:ring-white/[0.05] sm:p-10">
      <div className="landing-aurora landing-parallax-strong pointer-events-none absolute -right-24 top-0 h-72 w-72 rounded-full bg-primary/12 blur-3xl dark:bg-primary/18" aria-hidden />
      <div className="relative z-[1]">
        <p className="landing-fade-up text-xs font-semibold uppercase tracking-[0.12em] text-secondary">{t('footer.privacy')}</p>
        <h1 className="landing-fade-up landing-fade-up-delay-1 font-display mt-3 text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100 md:text-4xl">
          {pageText('title', t('landingBusiness.privacyTitle'))}
        </h1>
        <p className="landing-fade-up landing-fade-up-delay-2 mt-4 max-w-4xl text-sm leading-relaxed text-slate-600 dark:text-slate-400">
          {pageText('intro', t('landingBusiness.privacyIntro'))}
        </p>
        <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
          Last data refresh: {lastUpdated ? new Date(lastUpdated).toLocaleDateString() : 'N/A'}
        </p>
        <div className="mt-8 space-y-4">
          {sections.map((section) => (
            <article
              key={section.title}
              className="landing-3d-card rounded-2xl border border-slate-200/80 bg-white/80 p-5 dark:border-slate-700/75 dark:bg-slate-900/70"
            >
              <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{section.title}</h2>
              <p className="mt-2 text-sm leading-relaxed text-slate-600 dark:text-slate-400">{section.body}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
