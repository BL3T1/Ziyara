/**
 * Super Admin: paginated members of an organizational group.
 */

import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { rolesAPI, getApiErrorMessage } from '../../services/api'
import type { PageDto, UserDto } from '../../types/api'

export function GroupMembersPage() {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const { groupId } = useParams<{ groupId: string }>()
  const location = useLocation()
  const state = location.state as { groupName?: string; groupCode?: string } | undefined
  const { user } = useAuth()

  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<UserDto[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const groupName = state?.groupName ?? t('groupMembersPage.unknownGroup')
  const groupCode = state?.groupCode

  const load = useCallback(() => {
    if (!groupId) return
    setLoading(true)
    setError('')
    rolesAPI
      .getGroupMembers(groupId, { page, size: 15 })
      .then((res) => {
        const d = res.data as PageDto<UserDto>
        setRows(Array.isArray(d?.content) ? d.content : [])
        setTotalPages(typeof d?.totalPages === 'number' ? d.totalPages : 0)
      })
      .catch((e) => {
        setError(getApiErrorMessage(e, t('usersPage.membersLoadError')))
        setRows([])
        setTotalPages(0)
      })
      .finally(() => setLoading(false))
  }, [groupId, page, t])

  useEffect(() => {
    load()
  }, [load])

  if (!user) return null

  if (user.role !== 'super_admin') {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-900/20">
        <h2 className="text-xl font-semibold text-amber-800 dark:text-amber-200">{t('access.restrictedTitle')}</h2>
        <p className="mt-2 text-amber-700 dark:text-amber-300">{t('groupMembersPage.superAdminOnly')}</p>
        <button
          type="button"
          onClick={() => navigate('/management/users')}
          className="mt-4 rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700"
        >
          {t('groupMembersPage.backToGroups')}
        </button>
      </div>
    )
  }

  if (!groupId) {
    return (
      <p className="text-slate-600 dark:text-slate-400">{t('groupMembersPage.invalidLink')}</p>
    )
  }

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <button
            type="button"
            onClick={() => navigate('/management/users')}
            className="mb-2 text-sm font-medium text-primary hover:underline"
          >
            ← {t('groupMembersPage.backToGroups')}
          </button>
          <h1 className="app-page-title">{t('groupMembersPage.title', { name: groupName })}</h1>
          {groupCode && (
            <p className="mt-1 font-mono text-sm text-slate-500 dark:text-slate-400">{groupCode}</p>
          )}
        </div>
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      <div className="mt-8 table-shell overflow-x-auto">
        {loading ? (
          <div className="p-8 text-center text-slate-500 dark:text-slate-400">{t('usersPage.loadingGroups')}</div>
        ) : rows.length === 0 ? (
          <p className="p-8 text-center text-slate-500 dark:text-slate-400">{t('usersPage.membersEmpty')}</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5 text-start">{t('usersPage.membersColEmail')}</th>
                <th className="px-4 py-3.5 text-start">{t('usersPage.membersColRole')}</th>
                <th className="px-4 py-3.5 text-start">{t('usersPage.membersColStatus')}</th>
                <th className="px-4 py-3.5 text-end">{t('groupMembersPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((u) => (
                <tr key={u.id}>
                  <td className="px-4 py-3 text-sm text-slate-800 dark:text-slate-100">{u.email}</td>
                  <td className="px-4 py-3 font-mono text-sm text-slate-600 dark:text-slate-300">{u.role ?? '—'}</td>
                  <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-400">{u.status ?? '—'}</td>
                  <td className="px-4 py-3 text-end text-sm">
                    <Link
                      to={`/management/staff/${u.id}`}
                      state={{ fromGroup: { id: groupId, name: groupName, code: groupCode } }}
                      className="text-primary hover:underline"
                    >
                      {t('groupMembersPage.viewDetails')}
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {!loading && rows.length > 0 && (
        <div className="mt-4 flex flex-wrap items-center justify-between gap-2">
          <p className="text-xs text-slate-500 dark:text-slate-400">
            {t('usersPage.membersPageOf', { page: page + 1, total: Math.max(1, totalPages) })}
          </p>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={page <= 0 || loading}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm disabled:opacity-40 dark:border-slate-600"
            >
              {t('usersPage.membersPrev')}
            </button>
            <button
              type="button"
              disabled={page >= totalPages - 1 || loading || totalPages === 0}
              onClick={() => setPage((p) => p + 1)}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm disabled:opacity-40 dark:border-slate-600"
            >
              {t('usersPage.membersNext')}
            </button>
          </div>
        </div>
      )}
    </>
  )
}
