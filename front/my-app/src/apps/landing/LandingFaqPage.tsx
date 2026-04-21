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
    <section className="landing-scroll-parallax lp-sheet">
      <p className="landing-fade-up lp-eyebrow lp-eyebrow--tight">{pageText('eyebrow', t('landingBusiness.faqEyebrow'))}</p>
      <h1 className="landing-fade-up landing-fade-up-delay-1 lp-h1" style={{ marginTop: 12 }}>
        {pageText('title', t('landingBusiness.faqTitle'))}
      </h1>
      <div className="mt-6 grid gap-4">
        {faqItems.map((item) => (
          <Card key={item.question} surface="landing" className="!p-5">
            <h2 className="text-base font-semibold" style={{ color: 'var(--ink-heading)' }}>
              {item.question}
            </h2>
            <p className="mt-2 text-sm leading-relaxed" style={{ color: 'var(--ink-muted)' }}>
              {item.answer}
            </p>
          </Card>
        ))}
      </div>
    </section>
  )
}
