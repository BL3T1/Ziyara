import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'

export function LandingPublicFooter() {
  const { t, locale, toggleLocale } = useLanguage()

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
        <button
          type="button"
          onClick={toggleLocale}
          className="lang-toggle"
          aria-label={t('common.changeLanguage')}
        >
          <span className={locale === 'en' ? 'active' : ''}>{t('common.english').slice(0, 2).toUpperCase()}</span>
          <span className={locale === 'ar' ? 'active' : ''}>{t('common.arabic').slice(0, 2).toUpperCase()}</span>
        </button>
      </div>
    </footer>
  )
}
