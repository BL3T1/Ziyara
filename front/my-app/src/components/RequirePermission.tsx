import type { ReactNode } from 'react'
import { usePermissions } from '../context/PermissionsContext'
import { useLanguage } from '../context/LanguageContext'
import { useNavigate } from 'react-router-dom'

interface Props {
  code: string
  children: ReactNode
}

function AccessDenied({ code }: { code: string }) {
  const { t } = useLanguage()
  const navigate = useNavigate()
  return (
    <div className="flex min-h-[50vh] flex-col items-center justify-center rounded-xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-900/20">
      <h2 className="text-xl font-semibold text-amber-800 dark:text-amber-200">
        {t('access.restrictedTitle')}
      </h2>
      <p className="mt-2 text-sm text-amber-700 dark:text-amber-300">
        {t('access.needPermission', { permission: code })}
      </p>
      <button
        type="button"
        onClick={() => navigate('/dashboard')}
        className="mt-4 rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700"
      >
        {t('access.backToDashboard')}
      </button>
    </div>
  )
}

export function RequirePermission({ code, children }: Props) {
  const { has, loading } = usePermissions()
  if (loading) return null
  if (!has(code)) return <AccessDenied code={code} />
  return <>{children}</>
}
