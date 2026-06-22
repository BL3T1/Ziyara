import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { usePermission } from '../../hooks/usePermission'
import { rolesAPI, usersAPI, getApiErrorMessage } from '../../services/api'
import { Modal } from '../../components/Modal'
import type { PageDto, UserDto } from '../../types/api'

export function GroupMembersPage() {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const { groupId } = useParams<{ groupId: string }>()
  const location = useLocation()
  const state = location.state as { groupName?: string; groupCode?: string } | undefined
  const canView = usePermission('users:read')
  const canWrite = usePermission('users:write')

  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<UserDto[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editingUser, setEditingUser] = useState<UserDto | null>(null)
  const [successMsg, setSuccessMsg] = useState('')

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

  if (!canView) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-900/20">
        <h2 className="text-xl font-semibold text-amber-800 dark:text-amber-200">{t('access.restrictedTitle')}</h2>
        <p className="mt-2 text-amber-700 dark:text-amber-300">{t('ui.accessDenied')}</p>
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
      {successMsg && (
        <div className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800 dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-200">
          {successMsg}
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
                    <div className="inline-flex items-center gap-3">
                      {canWrite && (
                        <>
                          <button
                            type="button"
                            onClick={() => {
                              setSuccessMsg('')
                              setEditingUser(u)
                            }}
                            className="font-medium text-primary hover:underline"
                          >
                            {t('groupMembersPage.editInfo')}
                          </button>
                          <span className="text-slate-300 dark:text-slate-600">|</span>
                        </>
                      )}
                      <Link
                        to={`/management/staff/${u.id}`}
                        state={{ fromGroup: { id: groupId, name: groupName, code: groupCode } }}
                        className="text-slate-500 hover:underline dark:text-slate-400"
                      >
                        {t('groupMembersPage.viewDetails')}
                      </Link>
                    </div>
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

      {editingUser && (
        <EditStaffModal
          user={editingUser}
          onClose={() => setEditingUser(null)}
          onSuccess={(msg) => {
            setEditingUser(null)
            setSuccessMsg(msg)
            load()
          }}
        />
      )}
    </>
  )
}

function EditStaffModal({
  user,
  onClose,
  onSuccess,
}: {
  user: UserDto
  onClose: () => void
  onSuccess: (msg: string) => void
}) {
  const { t } = useLanguage()
  const [email, setEmail] = useState(user.email ?? '')
  const [phone, setPhone] = useState(user.phone ?? '')
  const [firstName, setFirstName] = useState(user.firstName ?? '')
  const [lastName, setLastName] = useState(user.lastName ?? '')
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!email.trim()) {
      setLocalError(`${t('staffUserPage.labelEmail')} is required.`)
      return
    }
    setSubmitting(true)
    setLocalError('')
    try {
      await usersAPI.update(user.id, {
        email: email.trim(),
        phone: phone.trim() || undefined,
        firstName: firstName.trim() || undefined,
        lastName: lastName.trim() || undefined,
      })
      onSuccess(t('staffUserPage.editProfileSuccess'))
    } catch (err) {
      setLocalError(getApiErrorMessage(err, t('staffUserPage.editProfileError')))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t('staffUserPage.editProfileTitle', { name: user.email })}
      size="sm"
      footer={
        <>
          <button type="button" onClick={onClose} disabled={submitting} className="dashboard-btn-secondary">
            {t('ui.cancel')}
          </button>
          <button type="submit" form="edit-staff-form" disabled={submitting} className="dashboard-btn-primary disabled:opacity-70">
            {t('staffUserPage.saveProfile')}
          </button>
        </>
      }
    >
      {localError && (
        <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
          {localError}
        </div>
      )}
      <form id="edit-staff-form" onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('staffUserPage.labelEmail')}
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="modal-input mt-1.5 w-full"
            autoFocus
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
            {t('staffUserPage.labelPhone')}
          </label>
          <input
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            className="modal-input mt-1.5 w-full"
          />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t('staffUserPage.labelFirstName')}
            </label>
            <input
              type="text"
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              className="modal-input mt-1.5 w-full"
              maxLength={100}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
              {t('staffUserPage.labelLastName')}
            </label>
            <input
              type="text"
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              className="modal-input mt-1.5 w-full"
              maxLength={100}
            />
          </div>
        </div>
      </form>
    </Modal>
  )
}
