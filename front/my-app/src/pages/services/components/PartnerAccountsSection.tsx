import { useNavigate } from 'react-router-dom'
import { Card } from '../../../components/Card'
import { useLanguage } from '../../../context/LanguageContext'
import type { ServiceProviderDto } from '../../../types/api'

export function PartnerAccountsSection({ partners }: { partners: ServiceProviderDto[] }) {
  const { t } = useLanguage()
  const navigate = useNavigate()

  if (partners.length === 0) return null

  return (
    <div className="mt-10">
      <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('servicesPage.partnersHeading')}</h2>
      <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('servicesPage.partnersHint')}</p>
      <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {partners.map((p) => (
          <Card key={p.id} className="p-5">
            <h3 className="font-semibold text-slate-900 dark:text-slate-100">{p.name}</h3>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
              {t('providersPage.colStatus')}: {p.status ?? t('ui.emDash')}
            </p>
            {p.email && (
              <p className="mt-2 line-clamp-2 text-sm text-slate-600 dark:text-slate-300">{p.email}</p>
            )}
            <button
              type="button"
              onClick={() => navigate('/management/providers')}
              className="mt-4 text-sm font-medium text-primary hover:underline"
            >
              {t('servicesPage.viewInProviders')}
            </button>
          </Card>
        ))}
      </div>
    </div>
  )
}
