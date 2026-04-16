import { useParams } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'

interface PortalPlaceholderPageProps {
  /** i18n key for page title, e.g. title.listings */
  titleKey: string
}

export function PortalPlaceholderPage({ titleKey }: PortalPlaceholderPageProps) {
  const { id } = useParams()
  const { t } = useLanguage()
  const title = t(titleKey)
  const displayTitle = id ? `${title} (${id})` : title

  return (
    <>
      <h1 className="app-page-title">{displayTitle}</h1>
    </>
  )
}
