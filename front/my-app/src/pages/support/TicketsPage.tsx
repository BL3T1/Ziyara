/**
 * Support > Tickets – group-first by priority or status.
 */

import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { ticketsAPI } from '../../services/api'
import type { TicketDto } from '../../types/api'

export function TicketsPage() {
  const { t } = useLanguage()
  const PRIORITY_CARDS = useMemo(
    () => [
      { id: 'LOW', label: t('ticketsPage.priLow') },
      { id: 'MEDIUM', label: t('ticketsPage.priMedium') },
      { id: 'HIGH', label: t('ticketsPage.priHigh') },
      { id: 'URGENT', label: t('ticketsPage.priUrgent') },
    ],
    [t],
  )
  const STATUS_CARDS = useMemo(
    () => [
      { id: 'OPEN', label: t('ticketsPage.stOpen') },
      { id: 'IN_PROGRESS', label: t('ticketsPage.stInProgress') },
      { id: 'RESOLVED', label: t('ticketsPage.stResolved') },
      { id: 'CLOSED', label: t('ticketsPage.stClosed') },
    ],
    [t],
  )

  const [tickets, setTickets] = useState<TicketDto[]>([])
  const [filterType, setFilterType] = useState<'priority' | 'status'>('status')
  const [filterValue, setFilterValue] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    const params = filterValue ? (filterType === 'priority' ? { priority: filterValue } : { status: filterValue }) : undefined
    ticketsAPI
      .list(params)
      .then((res) => setTickets(Array.isArray(res.data) ? (res.data as TicketDto[]) : []))
      .catch(() => setTickets([]))
      .finally(() => setLoading(false))
  }, [filterType, filterValue])

  const cards = filterType === 'priority' ? PRIORITY_CARDS : STATUS_CARDS

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('title.tickets')}</h1>
      <div className="mt-4 flex gap-4">
        <button
          type="button"
          onClick={() => {
            setFilterType('priority')
            setFilterValue(null)
          }}
          className={`text-sm font-medium ${filterType === 'priority' ? 'text-primary' : 'text-slate-500'}`}
        >
          {t('ticketsPage.byPriority')}
        </button>
        <button
          type="button"
          onClick={() => {
            setFilterType('status')
            setFilterValue(null)
          }}
          className={`text-sm font-medium ${filterType === 'status' ? 'text-primary' : 'text-slate-500'}`}
        >
          {t('ticketsPage.byStatus')}
        </button>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {cards.map((card) => (
          <button
            key={card.id}
            type="button"
            onClick={() => setFilterValue(card.id)}
            className={filterValue === card.id ? 'dashboard-pill dashboard-pill--active' : 'dashboard-pill'}
          >
            {card.label}
          </button>
        ))}
      </div>

      <div className="mt-6 table-shell">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
        ) : tickets.length === 0 ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('ticketsPage.noTickets')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('ticketsPage.colNumber')}</th>
                <th className="px-4 py-3.5">{t('ticketsPage.colSubject')}</th>
                <th className="px-4 py-3.5">{t('ticketsPage.colPriority')}</th>
                <th className="px-4 py-3.5">{t('ticketsPage.colStatus')}</th>
                <th className="px-4 py-3.5">{t('ticketsPage.colCreated')}</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((tk) => (
                <tr key={tk.id}>
                  <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-slate-900 dark:text-slate-100">
                    <Link to={`/support/tickets/${tk.id}`} className="text-primary hover:underline">
                      {tk.ticketNumber?.trim() || t('ticketsPage.unnumbered')}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{tk.subject}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{tk.priority}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{tk.status}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{tk.createdAt ?? t('ui.emDash')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
