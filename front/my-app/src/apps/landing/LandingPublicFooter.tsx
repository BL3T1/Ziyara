import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'

export function LandingPublicFooter() {
  const { t, locale } = useLanguage()

  return (
    <footer className="lp-footer">
      <div className="lp-footer-cols">
        <div>
          <h4>{t('landingPublicFooter.product')}</h4>
          <Link to="/services">{t('landingPublicFooter.features')}</Link>
          <Link to="/faq">{t('landingPublicFooter.pricing')}</Link>
          <Link to="/about">{t('landingPublicFooter.caseStudies')}</Link>
        </div>
        <div>
          <h4>{t('landingPublicFooter.company')}</h4>
          <Link to="/about">{t('landingPublicFooter.about')}</Link>
          <Link to="/contact">{t('landingPublicFooter.careers')}</Link>
          <Link to="/contact">{t('landingPublicFooter.contact')}</Link>
        </div>
        <div>
          <h4>{t('landingPublicFooter.legal')}</h4>
          <Link to="/privacy">{t('footer.privacy')}</Link>
          <Link to="/terms">{t('footer.terms')}</Link>
        </div>
      </div>
      <div className="lp-footer-meta">
        <span className="lp-muted">© {new Date().getFullYear()} Ziyara. {t('footer.allRightsReserved')}</span>
        <div className="lang-toggle" aria-hidden>
          <span className={locale === 'en' ? 'active' : ''}>EN</span>
          <span className={locale === 'ar' ? 'active' : ''}>AR</span>
        </div>
      </div>
    </footer>
  )
}
