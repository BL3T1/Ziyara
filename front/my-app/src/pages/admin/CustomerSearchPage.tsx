/**
 * Super admin > Find customer — search by email, phone, profile name, or exact user id (CUSTOMER role only).
 */

import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { adminSuperAPI, getApiErrorMessage } from '../../services/api'
import type { UserDto } from '../../types/api'

export function CustomerSearchPage() {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isSuperAdmin = user?.role === 'super_admin'
  const [q, setQ] = useState('')
  const [nameFilter, setNameFilter] = useState('')
  const [phoneFilter, setPhoneFilter] = useState('')
  const [emailFilter, setEmailFilter] = useState('')
  const [idFilter, setIdFilter] = useState('')
  const [rows, setRows] = useState<UserDto[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!user) return null

  if (!isSuperAdmin) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-900/20">
        <h2 className="text-xl font-semibold text-amber-800 dark:text-amber-200">{t('access.restrictedTitle')}</h2>
        <p className="mt-2 text-amber-700 dark:text-amber-300">
          {t('access.superAdminCustomersWithRole', { role: user.role })}
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

  const search = () => {
    const term = idFilter.trim() || emailFilter.trim() || phoneFilter.trim() || nameFilter.trim() || q.trim()
    if (!term) {
      setError(t('customerSearchPage.enterQuery'))
      return
    }
    setError(null)
    setLoading(true)
    adminSuperAPI
      .searchCustomers(term)
      .then((res) => {
        const data = res.data
        let results = Array.isArray(data) ? (data as UserDto[]) : []
        if (nameFilter.trim()) results = results.filter((r) => r.fullName?.toLowerCase().includes(nameFilter.toLowerCase()))
        if (phoneFilter.trim()) results = results.filter((r) => r.phone?.includes(phoneFilter.trim()))
        if (emailFilter.trim()) results = results.filter((r) => r.email?.toLowerCase().includes(emailFilter.toLowerCase()))
        if (idFilter.trim()) results = results.filter((r) => r.id === idFilter.trim())
        setRows(results)
      })
      .catch((e) => {
        setError(getApiErrorMessage(e, 'Search failed'))
        setRows([])
      })
      .finally(() => setLoading(false))
  }

  const dash = t('ui.emDash')

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">{t('customerSearchPage.title')}</h1>

      <div className="mt-6 space-y-3 rounded-xl border border-slate-200 bg-slate-50/80 p-4 dark:border-slate-700 dark:bg-slate-800/40">
        <div className="flex flex-wrap gap-2">
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && search()}
            placeholder={t('customerSearchPage.placeholder')}
            className="min-w-[16rem] flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
        </div>
        <div className="flex flex-wrap gap-2">
          <input
            type="text"
            value={nameFilter}
            onChange={(e) => setNameFilter(e.target.value)}
            placeholder="Filter by name"
            className="w-40 rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
          <input
            type="text"
            value={emailFilter}
            onChange={(e) => setEmailFilter(e.target.value)}
            placeholder="Filter by email"
            className="w-44 rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
          <input
            type="text"
            value={phoneFilter}
            onChange={(e) => setPhoneFilter(e.target.value)}
            placeholder="Filter by phone"
            className="w-36 rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
          <input
            type="text"
            value={idFilter}
            onChange={(e) => setIdFilter(e.target.value)}
            placeholder="Exact customer ID"
            className="w-56 rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
          <button
            type="button"
            onClick={search}
            disabled={loading}
            className="dashboard-btn-primary disabled:opacity-50"
          >
            {loading ? t('ui.searching') : t('ui.searchAction')}
          </button>
        </div>
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="mt-6 table-shell">
        {rows.length === 0 && !loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('customerSearchPage.noResults')}</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5">{t('customerSearchPage.colName')}</th>
                <th className="px-4 py-3.5">{t('customerSearchPage.colEmail')}</th>
                <th className="px-4 py-3.5">{t('customerSearchPage.colPhone')}</th>
                <th className="px-4 py-3.5">{t('customerSearchPage.colStatus')}</th>
                <th className="px-4 py-3.5">{t('customerSearchPage.colCreated')}</th>
                <th className="px-4 py-3.5">{t('customerSearchPage.colProfile')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id}>
                  <td className="px-4 py-3 text-sm font-medium text-slate-800 dark:text-slate-200">
                    {r.fullName?.trim() || dash}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-700 dark:text-slate-300">{r.email}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-400">{r.phone ?? dash}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-400">{r.status ?? dash}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-600 dark:text-slate-400">
                    {r.createdAt ? new Date(r.createdAt).toLocaleString() : dash}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-sm">
                    <button
                      type="button"
                      onClick={() => navigate(`/admin/customers/${encodeURIComponent(r.id)}`)}
                      className="text-primary hover:underline"
                    >
                      {t('customerSearchPage.viewProfile')}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
