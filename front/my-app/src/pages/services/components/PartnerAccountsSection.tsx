import { useState, useMemo } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useLanguage } from '../../../context/LanguageContext'
import { usePermission } from '../../../hooks/usePermission'
import type { ServiceProviderDto } from '../../../types/api'
import type { ServiceCategorySlug, ServiceEntity } from '../serviceModel'

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

const ChevronIcon = ({ open }: { open: boolean }) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="14"
    height="14"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.5"
    strokeLinecap="round"
    strokeLinejoin="round"
    style={{ transform: open ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 200ms ease' }}
    aria-hidden
  >
    <path d="m6 9 6 6 6-6" />
  </svg>
)

export function PartnerAccountsSection({
  partners,
  services = [],
  category,
}: {
  partners: ServiceProviderDto[]
  services?: ServiceEntity[]
  category?: ServiceCategorySlug
}) {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const canEdit = usePermission('providers:write')
  const showProfit = usePermission('payments:read')
  const [expanded, setExpanded] = useState<string | null>(null)

  const servicesByPartner = useMemo(() => {
    const map = new Map<string, ServiceEntity[]>()
    for (const svc of services) {
      if (svc.providerId) {
        const list = map.get(svc.providerId) ?? []
        list.push(svc)
        map.set(svc.providerId, list)
      }
    }
    return map
  }, [services])

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
        {partners.map((p) => {
          const partnerServices = servicesByPartner.get(p.id) ?? []
          const isOpen = expanded === p.id

          return (
            <div
              key={p.id}
              className="flex flex-col rounded-2xl border border-slate-200 bg-white shadow-sm transition-shadow hover:shadow-md dark:border-white/[0.06] dark:bg-[#0d1117]"
            >
              {/* ── Always-visible card body ─────────────────── */}
              <div className="flex flex-col p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <h3 className="truncate font-semibold text-slate-900 dark:text-slate-100">{p.name}</h3>
                    {p.type && (
                      <p className="mt-0.5 text-xs font-medium uppercase tracking-wide text-slate-400 dark:text-slate-500">
                        {p.type}
                      </p>
                    )}
                  </div>
                  <StatusBadge status={p.status ?? 'UNKNOWN'} />
                </div>

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
                  {showProfit && p.profitMargin != null && (
                    <p className="flex items-center gap-1.5">
                      <span className="shrink-0 text-slate-400">%</span>
                      {t('servicesPage.profitMarginLabel')}:{' '}
                      <span className="font-semibold text-slate-800 dark:text-slate-200">{Number(p.profitMargin)}%</span>
                    </p>
                  )}
                  {p.rating != null && (
                    <p className="flex items-center gap-1">
                      {Array.from({ length: 5 }, (_, i) => (
                        <span key={i} className={i < Math.round(Number(p.rating)) ? 'text-amber-400' : 'text-slate-300 dark:text-slate-600'} aria-hidden>
                          ★
                        </span>
                      ))}
                      <span className="ml-1 text-xs text-slate-500">{Number(p.rating).toFixed(1)}</span>
                    </p>
                  )}
                </div>

                {/* Actions row */}
                <div className="mt-4 flex items-center gap-3 border-t border-slate-100 pt-3.5 dark:border-white/[0.05]">
                  {canEdit ? (
                    <Link to={`/management/providers/${p.id}`} className="text-sm font-medium text-primary hover:underline">
                      {t('servicesPage.editAccount')}
                    </Link>
                  ) : (
                    <Link to={`/management/providers/${p.id}`} className="text-sm font-medium text-primary hover:underline">
                      {t('servicesPage.viewAccount')}
                    </Link>
                  )}

                  {/* Listings toggle */}
                  <button
                    type="button"
                    onClick={() => setExpanded(isOpen ? null : p.id)}
                    className="ms-auto flex items-center gap-1.5 text-sm font-medium text-slate-500 transition-colors hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
                  >
                    <span>
                      {partnerServices.length}{' '}
                      {partnerServices.length === 1 ? t('servicesPage.listingSingular') : t('servicesPage.listingPlural')}
                    </span>
                    <ChevronIcon open={isOpen} />
                  </button>
                </div>
              </div>

              {/* ── Expandable listings ──────────────────────── */}
              {isOpen && (
                <div className="border-t border-slate-100 dark:border-white/[0.05]">
                  {partnerServices.length === 0 ? (
                    <p className="px-5 py-4 text-sm text-slate-400 dark:text-slate-500">
                      {t('servicesPage.noListings')}
                    </p>
                  ) : (
                    <ul className="divide-y divide-slate-100 dark:divide-white/[0.04]">
                      {partnerServices.map((svc) => (
                        <li key={svc.id}>
                          <button
                            type="button"
                            onClick={() => category && navigate(`/${category}/${svc.id}`)}
                            className="flex w-full items-center justify-between gap-3 px-5 py-3 text-start text-sm transition-colors hover:bg-slate-50 dark:hover:bg-white/[0.03]"
                          >
                            <span className="truncate font-medium text-slate-700 dark:text-slate-200">{svc.name}</span>
                            {svc.price != null && (
                              <span className="shrink-0 text-xs text-slate-500 dark:text-slate-400">
                                {svc.currency} {svc.price}
                              </span>
                            )}
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
