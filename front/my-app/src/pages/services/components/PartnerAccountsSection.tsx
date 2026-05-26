import { Link } from 'react-router-dom'
import { useLanguage } from '../../../context/LanguageContext'
import { useAuth } from '../../../context/AuthContext'
import { canViewProviderCommission, isCompanyStaffRole } from '../../../types/auth'
import type { ServiceProviderDto } from '../../../types/api'

function StatusBadge({ status }: { status: string }) {
  const s = status.toUpperCase()
  const cls =
    s === 'ACTIVE'
      ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-300'
      : s === 'SUSPENDED'
        ? 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300'
        : s.startsWith('PENDING')
          ? 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300'
          : 'bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-300'
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${cls}`}>
      {status}
    </span>
  )
}

export function PartnerAccountsSection({ partners }: { partners: ServiceProviderDto[] }) {
  const { t } = useLanguage()
  const { user } = useAuth()
  const canEdit = user?.role ? isCompanyStaffRole(user.role) : false
  const showProfit = user?.role ? canViewProviderCommission(user.role) : false

  if (partners.length === 0) return null

  return (
    <div className="mt-10">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('servicesPage.partnersHeading')}</h2>
          <p className="mt-0.5 text-sm text-slate-500 dark:text-slate-400">{t('servicesPage.partnersHint')}</p>
        </div>
        <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600 dark:bg-slate-700 dark:text-slate-300">
          {partners.length}
        </span>
      </div>

      <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {partners.map((p) => (
          <div
            key={p.id}
            className="flex flex-col rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition-shadow hover:shadow-md dark:border-white/[0.06] dark:bg-[#0d1117]"
          >
            {/* Header row */}
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                <h3 className="truncate font-semibold text-slate-900 dark:text-slate-100">{p.name}</h3>
                {p.type && (
                  <p className="mt-0.5 text-xs font-medium text-slate-400 dark:text-slate-500 uppercase tracking-wide">
                    {p.type}
                  </p>
                )}
              </div>
              <StatusBadge status={p.status ?? 'UNKNOWN'} />
            </div>

            {/* Details */}
            <div className="mt-3 space-y-1.5 text-sm text-slate-600 dark:text-slate-400">
              {p.email && (
                <p className="flex items-center gap-1.5 truncate">
                  <span className="shrink-0 text-slate-400">✉</span>
                  {p.email}
                </p>
              )}
              {p.phone && (
                <p className="flex items-center gap-1.5">
                  <span className="shrink-0 text-slate-400">☎</span>
                  {p.phone}
                </p>
              )}
              {showProfit && p.commissionRate != null && (
                <p className="flex items-center gap-1.5">
                  <span className="shrink-0 text-slate-400">%</span>
                  {t('servicesPage.profitMarginLabel')}: <span className="font-semibold text-slate-800 dark:text-slate-200">{Number(p.commissionRate)}%</span>
                </p>
              )}
              {p.rating != null && (
                <p className="flex items-center gap-1.5">
                  <span className="shrink-0 text-slate-400">★</span>
                  {Number(p.rating).toFixed(1)} / 5
                </p>
              )}
            </div>

            {/* Actions */}
            <div className="mt-4 flex items-center gap-3 border-t border-slate-100 pt-3.5 dark:border-white/[0.05]">
              {canEdit ? (
                <Link
                  to={`/management/providers/${p.id}`}
                  className="text-sm font-medium text-primary hover:underline"
                >
                  {t('servicesPage.editAccount')}
                </Link>
              ) : (
                <Link
                  to={`/management/providers/${p.id}`}
                  className="text-sm font-medium text-primary hover:underline"
                >
                  {t('servicesPage.viewAccount')}
                </Link>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
