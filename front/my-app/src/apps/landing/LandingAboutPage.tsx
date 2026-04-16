import { useLanguage } from '../../context/LanguageContext'
import { useLandingLiveData } from './useLandingLiveData'
import { useLandingPageContent } from './useLandingPageContent'

export function LandingAboutPage() {
  const { t } = useLanguage()
  const { totalCities, totalServices, averageBasePrice } = useLandingLiveData()
  const { readString: pageText } = useLandingPageContent('about')

  return (
    <section className="landing-3d-stage landing-scroll-parallax relative overflow-hidden rounded-3xl border border-slate-200/80 bg-gradient-to-br from-white via-white to-primary/[0.08] p-8 shadow-[0_24px_70px_-30px_rgba(15,23,42,0.28)] dark:border-slate-700 dark:from-slate-900 dark:via-slate-900 dark:to-primary/[0.16] sm:p-10">
      <div className="landing-aurora landing-parallax-soft pointer-events-none absolute -right-28 top-0 h-72 w-72 rounded-full bg-primary/20 blur-3xl dark:bg-primary/30" aria-hidden />
      <div className="relative z-[1]">
        <p className="landing-fade-up text-xs font-semibold uppercase tracking-[0.12em] text-secondary">
          {pageText('eyebrow', t('landingBusiness.aboutEyebrow'))}
        </p>
        <h1 className="landing-fade-up landing-fade-up-delay-1 font-display mt-3 text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100 md:text-4xl">
          {pageText('title', t('landingBusiness.aboutTitle'))}
        </h1>
        <p className="landing-fade-up landing-fade-up-delay-2 mt-4 max-w-4xl text-base leading-relaxed text-slate-600 dark:text-slate-400">
          {pageText('body', t('landingBusiness.aboutBody'))}
        </p>
        <div className="mt-8 grid gap-4 sm:grid-cols-3">
          <div className="landing-3d-card rounded-2xl border border-slate-200/80 bg-white/85 p-4 dark:border-slate-700/70 dark:bg-slate-800/70">
            <p className="text-xl font-bold text-primary">{totalCities || 0}+</p>
            <p className="text-sm text-slate-600 dark:text-slate-400">Active destination cities</p>
          </div>
          <div className="landing-3d-card rounded-2xl border border-slate-200/80 bg-white/85 p-4 dark:border-slate-700/70 dark:bg-slate-800/70">
            <p className="text-xl font-bold text-primary">{totalServices || 0}+</p>
            <p className="text-sm text-slate-600 dark:text-slate-400">Live listings in the platform</p>
          </div>
          <div className="landing-3d-card rounded-2xl border border-slate-200/80 bg-white/85 p-4 dark:border-slate-700/70 dark:bg-slate-800/70">
            <p className="text-xl font-bold text-primary">{averageBasePrice ? `$${averageBasePrice}` : 'Top Rated'}</p>
            <p className="text-sm text-slate-600 dark:text-slate-400">Average starting base price</p>
          </div>
        </div>
      </div>
    </section>
  )
}
