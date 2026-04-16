import { Card } from '../../components/Card'
import { useLanguage } from '../../context/LanguageContext'
import { useLandingLiveData } from './useLandingLiveData'
import { useLandingPageContent } from './useLandingPageContent'

export function LandingFaqPage() {
  const { t } = useLanguage()
  const { totalServices, totalCities, averageBasePrice } = useLandingLiveData()
  const { readString: pageText } = useLandingPageContent('faq')
  const faqItems = [
    {
      question: t('landingBusiness.faqOneQ'),
      answer: `${t('landingBusiness.faqOneA')} ${totalServices ? `Currently ${totalServices} listings are available.` : ''}`.trim(),
    },
    {
      question: t('landingBusiness.faqTwoQ'),
      answer: `${t('landingBusiness.faqTwoA')} ${totalCities ? `Coverage spans ${totalCities} cities.` : ''}`.trim(),
    },
    {
      question: t('landingBusiness.faqThreeQ'),
      answer: `${t('landingBusiness.faqThreeA')} ${
        averageBasePrice ? `Average starting price is around $${averageBasePrice}.` : ''
      }`.trim(),
    },
  ]

  return (
    <section className="landing-3d-stage landing-scroll-parallax relative overflow-hidden rounded-[2rem] border border-slate-200/85 bg-white/80 p-7 shadow-[0_20px_70px_-34px_rgba(15,23,42,0.3)] ring-1 ring-slate-900/[0.03] dark:border-slate-700/75 dark:bg-slate-900/55 dark:ring-white/[0.05] sm:p-10">
      <div className="landing-aurora landing-parallax-soft pointer-events-none absolute -left-24 top-0 h-64 w-64 rounded-full bg-primary/10 blur-3xl dark:bg-primary/18" aria-hidden />
      <div className="landing-aurora landing-parallax-strong pointer-events-none absolute -right-24 bottom-0 h-64 w-64 rounded-full bg-secondary/18 blur-3xl dark:bg-secondary/12" aria-hidden />
      <div className="relative z-[1]">
      <p className="landing-fade-up text-xs font-semibold uppercase tracking-[0.12em] text-secondary">
        {pageText('eyebrow', t('landingBusiness.faqEyebrow'))}
      </p>
      <h1 className="landing-fade-up landing-fade-up-delay-1 font-display mt-3 text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100 md:text-4xl">
        {pageText('title', t('landingBusiness.faqTitle'))}
      </h1>
      <div className="mt-6 grid gap-4">
        {faqItems.map((item) => (
          <Card key={item.question} className="landing-3d-card">
            <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">{item.question}</h2>
            <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">{item.answer}</p>
          </Card>
        ))}
      </div>
      </div>
    </section>
  )
}
