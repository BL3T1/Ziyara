/**
 * Admin > Permission Matrix — visual role × module × action grid with toggle controls.
 */

import { useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { permissionsAPI, getApiErrorMessage } from '../../services/api'

interface PermEntry {
  id: string
  role_id: string
  module: string
  action: string
  granted: boolean
  updated_at: string
}

const MODULES = ['BOOKINGS', 'PAYMENTS', 'REPORTS', 'USERS', 'PROVIDERS', 'DISCOUNTS', 'SETTINGS', 'ADMIN']
const ACTIONS = ['VIEW', 'CREATE', 'EDIT', 'DELETE', 'EXPORT', 'APPROVE']

export function PermissionMatrixPage() {
  const { t } = useLanguage()
  const [entries, setEntries] = useState<PermEntry[]>([])
  const [roles, setRoles] = useState<{ id: string; name: string }[]>([])
  const [selectedRole, setSelectedRole] = useState<string>('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    permissionsAPI
      .getMatrix()
      .then((res) => {
        const data = res.data as PermEntry[]
        setEntries(Array.isArray(data) ? data : [])
        const roleMap = new Map<string, string>()
        data.forEach((e) => {
          if (!roleMap.has(e.role_id)) roleMap.set(e.role_id, e.role_id)
        })
        setRoles(Array.from(roleMap.entries()).map(([id]) => ({ id, name: id })))
        if (roleMap.size > 0) setSelectedRole(Array.from(roleMap.keys())[0])
      })
      .catch((e) => setError(getApiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  const getGranted = (module: string, action: string): boolean => {
    const entry = entries.find((e) => e.role_id === selectedRole && e.module === module && e.action === action)
    return entry?.granted ?? false
  }

  const toggle = async (module: string, action: string) => {
    if (!selectedRole) return
    const current = getGranted(module, action)
    const key = `${module}:${action}`
    setSaving(key)
    setError(null)
    try {
      await permissionsAPI.upsert({ roleId: selectedRole, module, action, granted: !current })
      setEntries((prev) => {
        const idx = prev.findIndex((e) => e.role_id === selectedRole && e.module === module && e.action === action)
        if (idx >= 0) {
          const next = [...prev]
          next[idx] = { ...next[idx], granted: !current }
          return next
        }
        return [...prev, { id: '', role_id: selectedRole, module, action, granted: !current, updated_at: '' }]
      })
      setSuccess(`${module}:${action} → ${!current ? 'granted' : 'denied'}`)
      setTimeout(() => setSuccess(null), 2000)
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setSaving(null)
    }
  }

  return (
    <>
      <h1 className="text-2xl font-bold text-slate-800 dark:text-slate-100">Permission Matrix</h1>
      <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
        Toggle module × action permissions per role. Changes apply immediately.
      </p>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}
      {success && (
        <div className="mt-4 rounded-lg border border-green-200 bg-green-50 p-3 text-sm text-green-700 dark:border-green-800 dark:bg-green-900/20 dark:text-green-300">
          {success}
        </div>
      )}

      {loading ? (
        <div className="mt-8 text-center text-slate-500 dark:text-slate-400">{t('ui.loading')}</div>
      ) : (
        <>
          <div className="mt-6 flex flex-wrap items-center gap-3">
            <label className="text-sm font-medium text-slate-700 dark:text-slate-200">Role:</label>
            <select
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
              className="rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            >
              {roles.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </select>
          </div>

          <div className="mt-6 overflow-x-auto rounded-xl border border-slate-200 dark:border-slate-700">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-50 dark:bg-slate-800">
                <tr>
                  <th className="px-4 py-3 text-left font-medium text-slate-600 dark:text-slate-300">Module</th>
                  {ACTIONS.map((a) => (
                    <th key={a} className="px-4 py-3 text-center font-medium text-slate-600 dark:text-slate-300">
                      {a}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
                {MODULES.map((mod) => (
                  <tr key={mod} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40">
                    <td className="px-4 py-3 font-medium text-slate-800 dark:text-slate-200">{mod}</td>
                    {ACTIONS.map((action) => {
                      const granted = getGranted(mod, action)
                      const key = `${mod}:${action}`
                      const busy = saving === key
                      return (
                        <td key={action} className="px-4 py-3 text-center">
                          <button
                            type="button"
                            disabled={busy}
                            onClick={() => toggle(mod, action)}
                            aria-label={`${mod} ${action} ${granted ? 'granted' : 'denied'}`}
                            className={`h-6 w-6 rounded-full border-2 transition-colors ${
                              busy
                                ? 'border-slate-300 bg-slate-100 dark:border-slate-600 dark:bg-slate-700'
                                : granted
                                ? 'border-green-500 bg-green-500 hover:bg-green-600'
                                : 'border-slate-300 bg-white hover:border-red-400 dark:border-slate-600 dark:bg-slate-800'
                            }`}
                          >
                            {granted && (
                              <svg className="mx-auto h-3 w-3 text-white" fill="currentColor" viewBox="0 0 12 12">
                                <path d="M10 3L5 8 2 5" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round" />
                              </svg>
                            )}
                          </button>
                        </td>
                      )
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </>
  )
}
