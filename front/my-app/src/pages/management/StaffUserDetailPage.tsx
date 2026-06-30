/**
 * Company staff user detail: profile, RBAC assignment, freeze/delete, login + audit logs.
 */

import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { Modal } from '../../components/Modal'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { PasswordInput } from '../../components/PasswordInput'
import { auditLogsAPI, usersAPI, getApiErrorMessage } from '../../services/api'
import { usePermissions } from '../../context/PermissionsContext'
import type { AuditLogDto, StaffDirectoryRoleOptionDto, UserDto, UserRbacAssignmentDto } from '../../types/api'
import {
  buildFallbackStaffRoleOptions,
  findStaffRoleOption,
  mergeStaffRoleOptionsFromApi,
  staffRoleOptionKey,
} from '../../utils/staffRoleOptions'

type LocationState = {
  fromGroup?: { id: string; name?: string; code?: string }
}

export function StaffUserDetailPage() {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const { userId } = useParams<{ userId: string }>()
  const location = useLocation()
  const fromGroup = (location.state as LocationState | undefined)?.fromGroup
  const { user: authUser } = useAuth()

  const [profile, setProfile] = useState<UserDto | null>(null)
  const [rbac, setRbac] = useState<UserRbacAssignmentDto | null>(null)
  const [loginRows, setLoginRows] = useState<{ loginAt?: string; ipAddress?: string }[]>([])
  const [auditRows, setAuditRows] = useState<AuditLogDto[]>([])
  const [staffRoleOptions, setStaffRoleOptions] = useState<StaffDirectoryRoleOptionDto[]>([])
  const [roleSelectionKey, setRoleSelectionKey] = useState('')
  const [loading, setLoading] = useState(true)
  const [savingRbac, setSavingRbac] = useState(false)
  const [actionBusy, setActionBusy] = useState(false)
  const [showResetModal, setShowResetModal] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const [confirmFreeze, setConfirmFreeze] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')

  const { has: hasPerm, loading: permsLoading } = usePermissions()
  const canManage = hasPerm('users:write')
  const isSuperAdmin = authUser?.role === 'super_admin'

  const buildFallback = useCallback(() => buildFallbackStaffRoleOptions(t), [t])

  const load = useCallback(() => {
    if (!userId || !canManage) return
    setLoading(true)
    setError('')
    Promise.all([
      usersAPI.get(userId).then((r) => r.data as UserDto),
      usersAPI.getUserRbacRole(userId).then((r) => r.data as UserRbacAssignmentDto).catch(() => null),
      usersAPI.getLoginHistory(userId).then((r) => (Array.isArray(r.data) ? r.data : []) as { loginAt?: string; ipAddress?: string }[]),
      auditLogsAPI.getForUser(userId).then((r) => (Array.isArray(r.data) ? r.data : []) as AuditLogDto[]),
      usersAPI
        .getStaffRoleOptions()
        .then((r) => {
          const data = r.data as StaffDirectoryRoleOptionDto[]
          const fallback = buildFallback()
          if (!Array.isArray(data) || data.length === 0) return fallback
          return mergeStaffRoleOptionsFromApi(data, fallback)
        })
        .catch(() => buildFallback()),
    ])
      .then(([u, rbacData, logins, audits, opts]) => {
        setProfile(u)
        setRbac(rbacData)
        setLoginRows(logins ?? [])
        setAuditRows(audits ?? [])
        setStaffRoleOptions(opts)
        if (rbacData?.roleId) {
          const match = opts.find((o) => o.rbacRoleId === rbacData.roleId)
          setRoleSelectionKey(match ? staffRoleOptionKey(match) : `rbac:${rbacData.roleId}`)
        } else if (u?.role) {
          const match = findStaffRoleOption(opts, `enum:${u.role}`)
          setRoleSelectionKey(match ? staffRoleOptionKey(match) : `enum:${u.role}`)
        } else {
          setRoleSelectionKey(opts[0] ? staffRoleOptionKey(opts[0]) : '')
        }
      })
      .catch((e) => {
        setError(getApiErrorMessage(e, t('staffUserPage.loadError')))
        setProfile(null)
      })
      .finally(() => setLoading(false))
  }, [userId, canManage, buildFallback, t])

  useEffect(() => {
    load()
  }, [load])

  const handleSaveRbac = async () => {
    if (!userId || !roleSelectionKey) return
    const sel = findStaffRoleOption(staffRoleOptions, roleSelectionKey)
    if (!sel) return
    setSavingRbac(true)
    setMsg('')
    setError('')
    try {
      if (!sel.rbacRoleId) {
        setError('Role has no ID — please refresh and try again.')
        return
      }
      await usersAPI.assignRbacRole(userId, { roleId: sel.rbacRoleId })
      setMsg(t('staffUserPage.rbacSaved'))
      load()
    } catch (e) {
      setError(getApiErrorMessage(e, t('staffUserPage.rbacError')))
    } finally {
      setSavingRbac(false)
    }
  }

  const handleFreezeToggle = () => {
    if (!userId || !profile) return
    setConfirmFreeze(true)
  }

  const doFreezeToggle = async () => {
    if (!userId || !profile) return
    const frozen = profile.status === 'FROZEN'
    setActionBusy(true)
    setError('')
    setMsg('')
    try {
      if (frozen) await usersAPI.unfreeze(userId)
      else await usersAPI.freeze(userId)
      setMsg(frozen ? t('staffUserPage.unfrozen') : t('staffUserPage.frozen'))
      load()
    } catch (e) {
      setError(getApiErrorMessage(e, t('staffUserPage.actionError')))
    } finally {
      setActionBusy(false)
    }
  }

  const handleDelete = () => {
    if (!userId) return
    setConfirmDelete(true)
  }

  const doDelete = async () => {
    if (!userId) return
    setActionBusy(true)
    setError('')
    setMsg('')
    try {
      await usersAPI.delete(userId)
      if (fromGroup?.id) {
        navigate(`/management/groups/${fromGroup.id}/members`, {
          replace: true,
          state: { groupName: fromGroup.name, groupCode: fromGroup.code },
        })
      } else {
        navigate('/management/users', { replace: true })
      }
    } catch (e) {
      setError(getApiErrorMessage(e, t('staffUserPage.actionError')))
    } finally {
      setActionBusy(false)
    }
  }

  if (!authUser) return null

  if (!canManage && !permsLoading) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-900/20">
        <h2 className="text-xl font-semibold text-amber-800 dark:text-amber-200">{t('access.restrictedTitle')}</h2>
        <p className="mt-2 text-amber-700 dark:text-amber-300">{t('staffUserPage.restricted')}</p>
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

  if (!userId) {
    return <p className="text-slate-600 dark:text-slate-400">{t('staffUserPage.invalidUser')}</p>
  }

  const rbacEditUrl =
    rbac?.roleId && isSuperAdmin ? `/admin/roles?roleId=${encodeURIComponent(rbac.roleId)}` : '/admin/roles'

  const builtInRoleOptions = staffRoleOptions.filter((o) => o.source !== 'CUSTOM')
  const customRoleOptions = staffRoleOptions.filter((o) => o.source === 'CUSTOM')

  return (
    <>
      <div className="mb-4">
        {fromGroup?.id ? (
          <button
            type="button"
            onClick={() =>
              navigate(`/management/groups/${fromGroup.id}/members`, {
                state: { groupName: fromGroup.name, groupCode: fromGroup.code },
              })
            }
            className="text-sm font-medium text-primary hover:underline"
          >
            ← {t('staffUserPage.backToMembers')}
          </button>
        ) : (
          <button
            type="button"
            onClick={() => navigate('/management/users')}
            className="text-sm font-medium text-primary hover:underline"
          >
            ← {t('staffUserPage.backToUsers')}
          </button>
        )}
      </div>

      <h1 className="app-page-title">{t('staffUserPage.title')}</h1>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}
      {msg && (
        <div className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800 dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-200">
          {msg}
        </div>
      )}

      {loading ? (
        <div className="mt-8 p-8 text-center text-slate-500">{t('ui.loading')}</div>
      ) : !profile ? (
        <p className="mt-8 text-slate-500">{t('staffUserPage.notFound')}</p>
      ) : (
        <div className="mt-8 space-y-10">
          <section className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-800">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('staffUserPage.sectionProfile')}</h2>
            <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-slate-500 dark:text-slate-400">{t('usersPage.membersColEmail')}</dt>
                <dd className="font-medium text-slate-900 dark:text-slate-100">{profile.email}</dd>
              </div>
              <div>
                <dt className="text-slate-500 dark:text-slate-400">{t('staffUserPage.phone')}</dt>
                <dd className="text-slate-800 dark:text-slate-200">{profile.phone ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500 dark:text-slate-400">{t('usersPage.membersColRole')}</dt>
                <dd className="font-mono text-slate-800 dark:text-slate-200">{rbac?.roleName ?? profile.role ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500 dark:text-slate-400">{t('usersPage.membersColStatus')}</dt>
                <dd className="text-slate-800 dark:text-slate-200">{profile.status ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500 dark:text-slate-400">{t('staffUserPage.emailVerified')}</dt>
                <dd className="text-slate-800 dark:text-slate-200">{profile.emailVerified ? t('ui.yes') : t('ui.no')}</dd>
              </div>
              <div>
                <dt className="text-slate-500 dark:text-slate-400">{t('staffUserPage.phoneVerified')}</dt>
                <dd className="text-slate-800 dark:text-slate-200">{profile.phoneVerified ? t('ui.yes') : t('ui.no')}</dd>
              </div>
              <div>
                <dt className="text-slate-500 dark:text-slate-400">{t('staffUserPage.lastLogin')}</dt>
                <dd className="text-slate-800 dark:text-slate-200">{profile.lastLoginAt ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-slate-500 dark:text-slate-400">{t('staffUserPage.createdAt')}</dt>
                <dd className="text-slate-800 dark:text-slate-200">{profile.createdAt ?? '—'}</dd>
              </div>
            </dl>

            <div className="mt-6 flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => setShowEditModal(true)}
                className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary/90"
              >
                {t('staffUserPage.editProfile')}
              </button>
              <button
                type="button"
                disabled={actionBusy}
                onClick={handleFreezeToggle}
                className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium dark:border-slate-600"
              >
                {profile.status === 'FROZEN' ? t('staffUserPage.unfreeze') : t('staffUserPage.freeze')}
              </button>
              <button
                type="button"
                disabled={actionBusy}
                onClick={() => setShowResetModal(true)}
                className="rounded-lg border border-amber-300 px-4 py-2 text-sm font-medium text-amber-700 dark:border-amber-700 dark:text-amber-300"
              >
                {t('staffUserPage.resetPassword')}
              </button>
              <button
                type="button"
                disabled={actionBusy}
                onClick={handleDelete}
                className="rounded-lg border border-red-300 px-4 py-2 text-sm font-medium text-red-700 dark:border-red-800 dark:text-red-300"
              >
                {t('staffUserPage.deleteUser')}
              </button>
            </div>
          </section>

          <section className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-800">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('staffUserPage.sectionRbac')}</h2>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('staffUserPage.rbacHint')}</p>
            {rbac && (
              <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">
                {t('staffUserPage.currentRbac', {
                  name: rbac.roleName ?? '—',
                  code: rbac.roleCode ?? '',
                })}
              </p>
            )}
            <div className="mt-4 max-w-md space-y-3">
              <label className="block text-sm font-medium text-slate-800 dark:text-slate-200">
                {t('staffUserPage.assignRbacLabel')}
              </label>
              <select
                value={roleSelectionKey}
                onChange={(e) => setRoleSelectionKey(e.target.value)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              >
                <option value="">{t('staffUserPage.rbacPlaceholder')}</option>
                {builtInRoleOptions.length > 0 && (
                  <optgroup label={t('usersPage.roleGroupBuiltIn')}>
                    {builtInRoleOptions.map((opt) => (
                      <option key={staffRoleOptionKey(opt)} value={staffRoleOptionKey(opt)}>
                        {opt.displayName}
                      </option>
                    ))}
                  </optgroup>
                )}
                {customRoleOptions.length > 0 && (
                  <optgroup label={t('usersPage.roleGroupCustom')}>
                    {customRoleOptions.map((opt) => (
                      <option key={staffRoleOptionKey(opt)} value={staffRoleOptionKey(opt)}>
                        {opt.displayName}
                      </option>
                    ))}
                  </optgroup>
                )}
              </select>
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  disabled={savingRbac || !roleSelectionKey}
                  onClick={handleSaveRbac}
                  className="dashboard-btn-primary text-sm disabled:opacity-60"
                >
                  {t('staffUserPage.saveRbac')}
                </button>
                {isSuperAdmin && (
                  <Link to={rbacEditUrl} className="dashboard-btn-secondary inline-flex items-center text-sm">
                    {t('staffUserPage.editRoleDefinition')}
                  </Link>
                )}
              </div>
            </div>
          </section>

          <section className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-800">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('staffUserPage.sectionLogin')}</h2>
            {loginRows.length === 0 ? (
              <p className="mt-4 text-sm text-slate-500">{t('staffUserPage.noLoginHistory')}</p>
            ) : (
              <ul className="mt-4 list-inside list-disc text-sm text-slate-700 dark:text-slate-300">
                {loginRows.map((row, i) => (
                  <li key={i}>
                    {row.loginAt ?? '—'}
                    {row.ipAddress ? ` · ${row.ipAddress}` : ''}
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-800">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('staffUserPage.sectionAudit')}</h2>
            {auditRows.length === 0 ? (
              <p className="mt-4 text-sm text-slate-500">{t('auditLogsPage.noEntries')}</p>
            ) : (
              <div className="mt-4 overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 text-left dark:border-slate-600">
                      <th className="pb-2 pe-4 font-medium text-slate-700 dark:text-slate-300">{t('auditLogsPage.colTime')}</th>
                      <th className="pb-2 pe-4 font-medium text-slate-700 dark:text-slate-300">{t('auditLogsPage.colAction')}</th>
                      <th className="pb-2 pe-4 font-medium text-slate-700 dark:text-slate-300">{t('auditLogsPage.colEntity')}</th>
                      <th className="pb-2 font-medium text-slate-700 dark:text-slate-300">{t('auditLogsPage.colOldNew')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditRows.map((log) => (
                      <tr key={log.id} className="border-b border-slate-100 dark:border-slate-700">
                        <td className="py-2 pe-4 whitespace-nowrap text-slate-600 dark:text-slate-300">{log.createdAt}</td>
                        <td className="py-2 pe-4 text-slate-900 dark:text-slate-100">{log.action}</td>
                        <td className="py-2 pe-4 text-slate-600 dark:text-slate-300">
                          {(log as AuditLogDto & { resource?: string }).resource ??
                            ([log.entityType, log.entityId].filter(Boolean).join('/') || '—')}
                        </td>
                        <td className="max-w-xs py-2 text-slate-600 dark:text-slate-300">
                          {log.oldValue != null || log.newValue != null
                            ? `${String(log.oldValue ?? '—').slice(0, 24)} → ${String(log.newValue ?? '—').slice(0, 24)}`
                            : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      )}

      {showResetModal && userId && (
        <ResetPasswordModal
          userId={userId}
          userName={profile?.email ?? userId}
          onClose={() => setShowResetModal(false)}
          onSuccess={(message) => {
            setShowResetModal(false)
            setMsg(message)
          }}
          onError={setError}
        />
      )}

      {showEditModal && userId && profile && (
        <EditProfileModal
          userId={userId}
          profile={profile}
          onClose={() => setShowEditModal(false)}
          onSuccess={(message) => {
            setShowEditModal(false)
            setMsg(message)
            load()
          }}
          onError={setError}
        />
      )}
      {profile && (
        <ConfirmDialog
          open={confirmFreeze}
          onClose={() => setConfirmFreeze(false)}
          title={profile.status === 'FROZEN' ? t('staffUserPage.confirmUnfreeze') : t('staffUserPage.confirmFreeze')}
          variant="danger"
          onConfirm={doFreezeToggle}
        />
      )}
      <ConfirmDialog
        open={confirmDelete}
        onClose={() => setConfirmDelete(false)}
        title={t('staffUserPage.confirmDelete')}
        variant="danger"
        onConfirm={doDelete}
      />
    </>
  )
}

function ResetPasswordModal({
  userId,
  userName,
  onClose,
  onSuccess,
  onError,
}: {
  userId: string
  userName: string
  onClose: () => void
  onSuccess: (msg: string) => void
  onError: (msg: string) => void
}) {
  const { t } = useLanguage()
  const [newPassword, setNewPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newPassword.trim()) {
      setLocalError(t('staffUserPage.resetPasswordLabel') + ' is required.')
      return
    }
    setSubmitting(true)
    setLocalError('')
    try {
      await usersAPI.resetPassword(userId, { newPassword })
      onSuccess(t('staffUserPage.resetPasswordSuccess'))
    } catch (err) {
      const msg = getApiErrorMessage(err, t('staffUserPage.resetPasswordError'))
      setLocalError(msg)
      onError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t('staffUserPage.resetPasswordTitle', { name: userName })}
      size="sm"
      footer={
        <>
          <button type="button" onClick={onClose} disabled={submitting} className="dashboard-btn-secondary">
            {t('ui.cancel')}
          </button>
          <button type="submit" form="reset-pw-form" disabled={submitting} className="dashboard-btn-primary disabled:opacity-70">
            {t('staffUserPage.resetPasswordSubmit')}
          </button>
        </>
      }
    >
      {localError && (
        <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
          {localError}
        </div>
      )}
      <form id="reset-pw-form" onSubmit={handleSubmit}>
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
          {t('staffUserPage.resetPasswordLabel')}
        </label>
        <PasswordInput
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          className="modal-input mt-1.5 w-full"
          autoFocus
          required
          minLength={6}
        />
      </form>
    </Modal>
  )
}

function EditProfileModal({
  userId,
  profile,
  onClose,
  onSuccess,
  onError,
}: {
  userId: string
  profile: UserDto
  onClose: () => void
  onSuccess: (msg: string) => void
  onError: (msg: string) => void
}) {
  const { t } = useLanguage()
  const [email, setEmail] = useState(profile.email ?? '')
  const [username, setUsername] = useState(profile.username ?? '')
  const [phone, setPhone] = useState(profile.phone ?? '')
  const [firstName, setFirstName] = useState(profile.firstName ?? '')
  const [lastName, setLastName] = useState(profile.lastName ?? '')
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
      await usersAPI.update(userId, {
        email: email.trim(),
        username: username.trim() || undefined,
        phone: phone.trim() || undefined,
        firstName: firstName.trim() || undefined,
        lastName: lastName.trim() || undefined,
      })
      onSuccess(t('staffUserPage.editProfileSuccess'))
    } catch (err) {
      const msg = getApiErrorMessage(err, t('staffUserPage.editProfileError'))
      setLocalError(msg)
      onError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t('staffUserPage.editProfileTitle', { name: profile.email })}
      size="sm"
      footer={
        <>
          <button type="button" onClick={onClose} disabled={submitting} className="dashboard-btn-secondary">
            {t('ui.cancel')}
          </button>
          <button type="submit" form="edit-profile-form" disabled={submitting} className="dashboard-btn-primary disabled:opacity-70">
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
      <form id="edit-profile-form" onSubmit={handleSubmit} className="space-y-4">
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
            {t('staffUserPage.labelUsername')}
          </label>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="modal-input mt-1.5 w-full"
            maxLength={50}
            autoComplete="off"
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
