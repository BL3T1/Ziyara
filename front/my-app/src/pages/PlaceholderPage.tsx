import { useLanguage } from '../context/LanguageContext'

interface PlaceholderPageProps {
  /** Prefer i18n title key, e.g. `title.chat` */
  titleKey?: string
  title?: string
}

export function PlaceholderPage({ titleKey, title }: PlaceholderPageProps) {
  const { t } = useLanguage()
  const displayTitle = titleKey ? t(titleKey) : (title ?? '')
  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800">{displayTitle}</h1>
    </>
  )
}
