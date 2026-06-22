import { useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { useLandingPageContent } from './useLandingPageContent'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'

export function LandingFaqPage() {
  useDocumentMeta({ title: 'FAQ · Ziyara', description: 'Answers to the most common questions about booking with Ziyara.' })
  const { t } = useLanguage()
  const { readString: pageText } = useLandingPageContent('faq')
  const [openIndex, setOpenIndex] = useState<number | null>(0)

  const faqItems = [
    { question: t('landingBusiness.faqOneQ'),   answer: t('landingBusiness.faqOneA') },
    { question: t('landingBusiness.faqTwoQ'),   answer: t('landingBusiness.faqTwoA') },
    { question: t('landingBusiness.faqThreeQ'), answer: t('landingBusiness.faqThreeA') },
    { question: t('landingBusiness.faqFourQ'),  answer: t('landingBusiness.faqFourA') },
    { question: t('landingBusiness.faqFiveQ'),  answer: t('landingBusiness.faqFiveA') },
    { question: t('landingBusiness.faqSixQ'),   answer: t('landingBusiness.faqSixA') },
    { question: t('landingBusiness.faqSevenQ'), answer: t('landingBusiness.faqSevenA') },
  ]

  return (
    <section className="landing-scroll-parallax lp-sheet">
      <p className="landing-fade-up lp-eyebrow lp-eyebrow--tight">{pageText('eyebrow', t('landingBusiness.faqEyebrow'))}</p>
      <h1 className="landing-fade-up landing-fade-up-delay-1 lp-h1 mt-3">
        {pageText('title', t('landingBusiness.faqTitle'))}
      </h1>
      <div className="mt-6 space-y-2 lp-animate">
        {faqItems.map((item, i) => (
          <div key={item.question} className="lp-faq-item">
            <button
              type="button"
              className="lp-faq-trigger"
              onClick={() => setOpenIndex(openIndex === i ? null : i)}
              aria-expanded={openIndex === i}
            >
              <span>{item.question}</span>
              <span className={`lp-faq-chevron${openIndex === i ? ' lp-faq-chevron--open' : ''}`} aria-hidden>›</span>
            </button>
            {openIndex === i && (
              <div className="lp-faq-answer">
                <p>{item.answer}</p>
              </div>
            )}
          </div>
        ))}
      </div>
    </section>
  )
}
