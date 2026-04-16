import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import { translate, type Locale as LocaleType } from '../i18n/translations'

const LANG_KEY = 'ziyara-lang'
export type Locale = 'en' | 'ar'

function loadLocale(): Locale {
  if (typeof window === 'undefined') return 'en'
  const stored = localStorage.getItem(LANG_KEY) as Locale | null
  if (stored === 'en' || stored === 'ar') return stored
  try {
    const nav = (navigator.language || '').toLowerCase()
    if (nav.startsWith('ar')) return 'ar'
  } catch {
    /* ignore */
  }
  return 'en'
}

function applyLocale(locale: Locale) {
  document.documentElement.lang = locale === 'ar' ? 'ar' : 'en'
  document.documentElement.dir = locale === 'ar' ? 'rtl' : 'ltr'
}

interface LanguageContextValue {
  locale: Locale
  setLocale: (l: Locale) => void
  toggleLocale: () => void
  /** Translate a key (e.g. 'common.logOut'). Use `{name}` placeholders in JSON and pass params. */
  t: (key: string, params?: Record<string, string | number>) => string
}

const LanguageContext = createContext<LanguageContextValue | null>(null)

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(loadLocale)

  useEffect(() => {
    applyLocale(locale)
    localStorage.setItem(LANG_KEY, locale)
  }, [locale])

  const setLocale = useCallback((l: Locale) => setLocaleState(l), [])
  const toggleLocale = useCallback(() => setLocaleState((l) => (l === 'en' ? 'ar' : 'en')), [])
  const t = useCallback(
    (key: string, params?: Record<string, string | number>) => translate(locale as LocaleType, key, params),
    [locale]
  )

  const value: LanguageContextValue = {
    locale,
    setLocale,
    toggleLocale,
    t,
  }

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
}

export function useLanguage() {
  const ctx = useContext(LanguageContext)
  if (!ctx) throw new Error('useLanguage must be used within LanguageProvider')
  return ctx
}
