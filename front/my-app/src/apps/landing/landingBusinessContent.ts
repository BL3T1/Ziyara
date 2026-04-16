export interface LandingBusinessItem {
  titleKey: string
  bodyKey: string
  imageSrc?: string
}

export const SERVICES: LandingBusinessItem[] = [
  {
    titleKey: 'landingBusiness.serviceHotelsTitle',
    bodyKey: 'landingBusiness.serviceHotelsBody',
    imageSrc:
      'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80',
  },
  {
    titleKey: 'landingBusiness.serviceRestaurantsTitle',
    bodyKey: 'landingBusiness.serviceRestaurantsBody',
    imageSrc:
      'https://images.unsplash.com/photo-1559339352-11d035aa65de?auto=format&fit=crop&w=1200&q=80',
  },
  {
    titleKey: 'landingBusiness.serviceTripsTitle',
    bodyKey: 'landingBusiness.serviceTripsBody',
    imageSrc:
      'https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1200&q=80',
  },
]

export const TRUST_POINTS: LandingBusinessItem[] = [
  { titleKey: 'landingBusiness.trustOneTitle', bodyKey: 'landingBusiness.trustOneBody' },
  { titleKey: 'landingBusiness.trustTwoTitle', bodyKey: 'landingBusiness.trustTwoBody' },
  { titleKey: 'landingBusiness.trustThreeTitle', bodyKey: 'landingBusiness.trustThreeBody' },
]

export interface LandingBusinessFaqItem {
  questionKey: string
  answerKey: string
}

export const FAQS: LandingBusinessFaqItem[] = [
  { questionKey: 'landingBusiness.faqOneQ', answerKey: 'landingBusiness.faqOneA' },
  { questionKey: 'landingBusiness.faqTwoQ', answerKey: 'landingBusiness.faqTwoA' },
  { questionKey: 'landingBusiness.faqThreeQ', answerKey: 'landingBusiness.faqThreeA' },
]
