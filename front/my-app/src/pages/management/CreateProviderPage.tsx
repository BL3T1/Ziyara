/**
 * Full-page create provider (Super Admin / CEO / GM): edit partner details and manager login on a dedicated screen.
 */

import { useMemo, useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { CreateProviderForm } from '../../components/CreateProviderForm'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { usePermission } from '../../hooks/usePermission'
import { PARTNER_SERVICE_TYPE_VALUES, type ServiceTypeDto } from '../../types/api'

function parsePresetType(raw: string | null): ServiceTypeDto | undefined {
  if (!raw) return undefined
  const u = raw.trim().toUpperCase()
  return (PARTNER_SERVICE_TYPE_VALUES as readonly string[]).includes(u) ? (u as ServiceTypeDto) : undefined
}

export function CreateProviderPage() {
  const { t } = useLanguage()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [finished, setFinished] = useState(false)
  const canCreate = usePermission('providers:write')

  const preset = useMemo(
    () => parsePresetType(searchParams.get('type')),
    [searchParams],
  )

  if (!user || !canCreate) {
    return <Navigate to="/management/providers" replace />
  }

  if (finished) {
    return (
      <div className="mx-auto max-w-xl">
        <Link
          to="/management/providers"
          className="text-sm font-medium text-primary hover:underline"
        >
          ← {t('createProviderPage.backToList')}
        </Link>
        <div className="mt-6 rounded-2xl border border-emerald-200 bg-emerald-50 p-6 dark:border-emerald-800 dark:bg-emerald-900/20">
          <h1 className="text-xl font-bold text-emerald-900 dark:text-emerald-100">
            {t('createProviderPage.doneTitle')}
          </h1>
          <p className="mt-2 text-sm text-emerald-800 dark:text-emerald-200">
            {t('createProviderModal.successActive')}
          </p>
          <Link to="/management/providers" className="mt-6 inline-flex dashboard-btn-primary">
            {t('createProviderPage.backToList')}
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl">
      <Link
        to="/management/providers"
        className="text-sm font-medium text-primary hover:underline"
      >
        ← {t('createProviderPage.backToList')}
      </Link>
      <h1 className="mt-4 text-2xl font-bold text-slate-900 dark:text-slate-100">
        {t('createProviderPage.title')}
      </h1>
      <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">{t('createProviderPage.intro')}</p>

      <div className="mt-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <CreateProviderForm
          variant="management"
          presetServiceType={preset}
          onCancel={() => navigate('/management/providers')}
          onCreated={() => setFinished(true)}
        />
      </div>
    </div>
  )
}
