import { Languages } from 'lucide-react'
import { useLanguage } from '../context/LanguageContext'

export function LanguageToggleButton({ className = '', ariaLabel }: { className?: string; ariaLabel?: string }) {
  const { locale, toggleLocale, t } = useLanguage()
  const label = ariaLabel ?? t('common.changeLanguage')
  const targetLabel = locale === 'en' ? 'عربي' : 'EN'

  return (
    <button
      type="button"
      onClick={toggleLocale}
      className={`inline-flex items-center justify-center gap-1 ${className}`}
      aria-label={label}
    >
      <Languages className="h-4 w-4 shrink-0 opacity-90" strokeWidth={2} aria-hidden />
      <span className="min-w-[1.75rem] text-center text-[0.7rem] font-bold leading-none tracking-wide">{targetLabel}</span>
    </button>
  )
}
