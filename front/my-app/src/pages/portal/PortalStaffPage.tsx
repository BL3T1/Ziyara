/**
 * Provider portal > Staff: signed-in account, provider org, and team list (GET /portal/staff).
 */

import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { usePermission } from '../../hooks/usePermission'
import { usersAPI, providersAPI, portalStaffAPI, getApiErrorMessage } from '../../services/api'
import { Card } from '../../components/Card'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'
import { PasswordInput } from '../../components/PasswordInput'
import { SearchableSelect, type SelectOption } from '../../components/SearchableSelect'
import type { PortalStaffMemberDto, ServiceProviderDto } from '../../types/api'

type MeUser = {
  id?: string
  email?: string
  phone?: string
  firstName?: string
  lastName?: string
  role?: string
  status?: string
}

function displayName(u: MeUser) {
  const n = [u.firstName, u.lastName].filter(Boolean).join(' ').trim()
  return n || u.email || '—'
}

export function PortalStaffPage() {
  const { t } = useLanguage()
  const canManage = usePermission('portal:manage')
  const [user, setUser] = useState<MeUser | null>(null)
  const [provider, setProvider] = useState<ServiceProviderDto | null>(null)
  const [team, setTeam] = useState<PortalStaffMemberDto[]>([])
  const [loading, setLoading] = useState(true)
  const [teamLoading, setTeamLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [addUser, setAddUser] = useState<SelectOption | null>(null)
  const [addTitle, setAddTitle] = useState('')
  const [adding, setAdding] = useState(false)
  const [newEmail, setNewEmail] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [newPhone, setNewPhone] = useState('')
  const [assignableRoles, setAssignableRoles] = useState<{ id: string; code: string; name: string }[]>([])
  const [newRoleId, setNewRoleId] = useState('')
  const [newTitle, setNewTitle] = useState('')
  const [creatingUser, setCreatingUser] = useState(false)
  const [editUserId, setEditUserId] = useState<string | null>(null)
  const [editTitle, setEditTitle] = useState('')
  const [editSaving, setEditSaving] = useState(false)
  const [removingId, setRemovingId] = useState<string | null>(null)
  const [resetPwUserId, setResetPwUserId] = useState<string | null>(null)
  const [resetPwValue, setResetPwValue] = useState('')
  const [resetPwSaving, setResetPwSaving] = useState(false)
  const [resetPwSuccess, setResetPwSuccess] = useState(false)

  const loadTeam = useCallback(() => {
    setTeamLoading(true)
    portalStaffAPI
      .list()
      .then((r) => setTeam(Array.isArray(r.data) ? r.data : []))
      .catch(() => setTeam([]))
      .finally(() => setTeamLoading(false))
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    Promise.all([usersAPI.getMe(), providersAPI.getMe()])
      .then(([ur, pr]) => {
        if (cancelled) return
        setUser((ur.data as MeUser) ?? null)
        setProvider((pr.data as ServiceProviderDto) ?? null)
      })
      .catch((e) => {
        if (!cancelled) {
          setError(getApiErrorMessage(e))
          setUser(null)
          setProvider(null)
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!loading && !error) loadTeam()
  }, [loading, error, loadTeam])

  useEffect(() => {
    if (!canManage) return
    portalStaffAPI
      .listAssignableRoles()
      .then((r) => {
        const roles = Array.isArray(r.data) ? r.data : []
        setAssignableRoles(roles)
        setNewRoleId((prev) => prev || roles[0]?.id || '')
      })
      .catch(() => setAssignableRoles([]))
  }, [canManage])

  const row = (label: string, value: string) => (
    <div className="flex flex-wrap justify-between gap-2 border-b border-slate-100 py-2.5 last:border-0 dark:border-slate-700/80">
      <span className="text-sm text-slate-500 dark:text-slate-400">{label}</span>
      <span className="text-sm font-medium text-slate-900 dark:text-slate-100">{value}</span>
    </div>
  )

  const fetchUserOptions = useCallback(async (query: string) => {
    try {
      const res = await usersAPI.list({ size: 50 })
      const items = (Array.isArray(res.data) ? res.data : ((res.data as { content?: unknown[] })?.content ?? [])) as Array<{ id?: string; firstName?: string; lastName?: string; email?: string }>
      const q = query.toLowerCase()
      return items
        .filter((u) => {
          const name = `${u.firstName ?? ''} ${u.lastName ?? ''}`.toLowerCase()
          return !q || name.includes(q) || (u.email ?? '').toLowerCase().includes(q)
        })
        .map((u) => ({
          value: u.id ?? '',
          label: `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || (u.email ?? ''),
          sublabel: u.email,
        }))
    } catch {
      return []
    }
  }, [])

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault()
    const uid = addUser?.value
    if (!uid) return
    setAdding(true)
    setError(null)
    try {
      const body: { userId: string; title?: string } = { userId: uid }
      const t = addTitle.trim()
      if (t) body.title = t
      await portalStaffAPI.add(body)
      setAddUser(null)
      setAddTitle('')
      loadTeam()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setAdding(false)
    }
  }

  const handleSaveEdit = async () => {
    if (!editUserId) return
    setEditSaving(true)
    setError(null)
    try {
      await portalStaffAPI.update(editUserId, { title: editTitle.trim() })
      setEditUserId(null)
      setEditTitle('')
      loadTeam()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setEditSaving(false)
    }
  }

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newRoleId) {
      setError(t('portalStaffPage.errNoRoles'))
      return
    }
    setCreatingUser(true)
    setError(null)
    try {
      const body: { email: string; password: string; roleId: string; phone?: string; title?: string } = {
        email: newEmail.trim(),
        password: newPassword,
        roleId: newRoleId,
      }
      const p = newPhone.trim()
      if (p) body.phone = p
      const t = newTitle.trim()
      if (t) body.title = t
      await portalStaffAPI.createUser(body)
      setNewEmail('')
      setNewPassword('')
      setNewPhone('')
      setNewTitle('')
      loadTeam()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setCreatingUser(false)
    }
  }

  const handleRemove = async (member: PortalStaffMemberDto) => {
    if (member.owner) return
    if (!window.confirm(t('portalStaffPage.confirmRemove'))) return
    setRemovingId(member.userId)
    setError(null)
    try {
      await portalStaffAPI.remove(member.userId)
      loadTeam()
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setRemovingId(null)
    }
  }

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!resetPwUserId || !resetPwValue.trim()) return
    setResetPwSaving(true)
    setResetPwSuccess(false)
    setError(null)
    try {
      await portalStaffAPI.resetPassword(resetPwUserId, { newPassword: resetPwValue })
      setResetPwSuccess(true)
      setResetPwValue('')
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setResetPwSaving(false)
    }
  }

  return (
    <>
      <h1 className="app-page-title">{t('title.staff')}</h1>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      {loading ? (
        <p className="mt-8 text-slate-500 dark:text-slate-400">{t('ui.loading')}</p>
      ) : (
        <div className="mt-8 grid gap-6 lg:grid-cols-2">
          <Card>
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('portalStaffPage.accountTitle')}</h2>
            <div className="mt-2">
              {row(t('portalStaffPage.labelName'), displayName(user ?? {}))}
              {row(t('portalStaffPage.labelEmail'), user?.email ?? '—')}
              {row(t('portalStaffPage.labelPhone'), user?.phone ?? '—')}
              {row(t('portalStaffPage.labelRole'), user?.role ?? '—')}
              {row(t('portalStaffPage.labelUserStatus'), user?.status ?? '—')}
            </div>
          </Card>
          <Card>
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('portalStaffPage.providerTitle')}</h2>
            <div className="mt-2">
              {row(t('portalPages.fieldLegalName'), provider?.name ?? '—')}
              {row(t('portalPages.fieldEmail'), provider?.email ?? '—')}
              {row(t('portalPages.fieldPhone'), provider?.phone ?? '—')}
              {row(t('portalStaffPage.labelProviderStatus'), provider?.status ?? '—')}
              {row(t('portalPages.profileType'), provider?.type ?? '—')}
            </div>
            <Link
              to="/portal/profile"
              className="mt-4 inline-flex text-sm font-medium text-primary hover:underline"
            >
              {t('portalStaffPage.editProfileLink')}
            </Link>
          </Card>
        </div>
      )}

      <Card className="mt-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('portalStaffPage.teamTitle')}</h2>
          {teamLoading && <span className="text-sm text-slate-500 dark:text-slate-400">{t('ui.loading')}</span>}
        </div>
        <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">{t('portalStaffPage.teamBody')}</p>

        <div className="mt-6 table-shell overflow-x-auto">
          <table className="min-w-full">
            <thead>
              <tr>
                <th className="px-4 py-3.5 text-left">{t('portalStaffPage.colEmail')}</th>
                <th className="px-4 py-3.5 text-left">{t('portalStaffPage.colRole')}</th>
                <th className="px-4 py-3.5 text-left">{t('portalStaffPage.colTitle')}</th>
                <th className="px-4 py-3.5 text-left">{t('portalStaffPage.colOwner')}</th>
                <th className="px-4 py-3.5 text-left">{t('portalStaffPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {team.length === 0 && !teamLoading ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-slate-500 dark:text-slate-400">
                    {t('portalStaffPage.teamEmpty')}
                  </td>
                </tr>
              ) : (
                team.map((m) => (
                  <tr key={m.userId}>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-slate-900 dark:text-slate-100">
                      {m.email?.trim() || t('ui.emDash')}
                    </td>
                    <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{m.role ?? '—'}</td>
                    <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-300">{m.title ?? '—'}</td>
                    <td className="px-4 py-3 text-sm">{m.owner ? t('portalStaffPage.badgeOwner') : '—'}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm">
                      {!m.owner && canManage && (
                        <>
                          <button
                            type="button"
                            onClick={() => {
                              setEditUserId(m.userId)
                              setEditTitle(m.title ?? '')
                            }}
                            className="text-primary hover:underline"
                          >
                            {t('portalStaffPage.editTitle')}
                          </button>
                          <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                          <button
                            type="button"
                            onClick={() => {
                              setResetPwUserId(m.userId)
                              setResetPwValue('')
                              setResetPwSuccess(false)
                            }}
                            className="text-amber-600 hover:underline dark:text-amber-400"
                          >
                            {t('portalStaffPage.resetPassword')}
                          </button>
                          <span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
                          <button
                            type="button"
                            disabled={removingId === m.userId}
                            onClick={() => handleRemove(m)}
                            className="text-red-600 hover:underline dark:text-red-400"
                          >
                            {t('portalStaffPage.remove')}
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {canManage && (
          <form onSubmit={handleAdd} className="mt-8 max-w-xl space-y-3 rounded-xl border border-slate-200 p-4 dark:border-slate-700">
            <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{t('portalStaffPage.addMemberTitle')}</h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">{t('portalStaffPage.addMemberHint')}</p>
            <SearchableSelect
              selectedOption={addUser}
              onSelect={setAddUser}
              fetchOptions={fetchUserOptions}
              placeholder={t('portalStaffPage.searchUserPlaceholder')}
              clearable
            />
            <input
              type="text"
              placeholder={t('portalStaffPage.titlePlaceholder')}
              value={addTitle}
              onChange={(e) => setAddTitle(e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            />
            <button
              type="submit"
              disabled={adding || !addUser?.value}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {t('portalStaffPage.addMember')}
            </button>
          </form>
        )}

        {canManage && (
          <form onSubmit={handleCreateUser} className="mt-4 max-w-xl space-y-3 rounded-xl border border-slate-200 p-4 dark:border-slate-700">
            <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{t('portalStaffPage.createMemberTitle')}</h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">{t('portalStaffPage.createMemberHint')}</p>
            <input
              type="email"
              required
              placeholder={t('portalStaffPage.createEmailPlaceholder')}
              value={newEmail}
              onChange={(e) => setNewEmail(e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            />
            <PasswordInput
              required
              minLength={6}
              placeholder={t('portalStaffPage.createPasswordPlaceholder')}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            />
            <input
              type="text"
              placeholder={t('portalStaffPage.createPhonePlaceholder')}
              value={newPhone}
              onChange={(e) => setNewPhone(e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            />
            <select
              value={newRoleId}
              onChange={(e) => setNewRoleId(e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            >
              {assignableRoles.length === 0 && <option value="">{t('portalStaffPage.errNoRoles')}</option>}
              {assignableRoles.map((r) => (
                <option key={r.id} value={r.id}>{r.name}</option>
              ))}
            </select>
            <input
              type="text"
              placeholder={t('portalStaffPage.titlePlaceholder')}
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
            />
            <button
              type="submit"
              disabled={creatingUser || !newRoleId}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {t('portalStaffPage.createMember')}
            </button>
          </form>
        )}

        <Link
          to="/portal/support"
          className="mt-6 inline-flex rounded-xl bg-primary/10 px-4 py-2 text-sm font-medium text-primary hover:bg-primary/20"
        >
          {t('portalStaffPage.contactSupportLink')}
        </Link>
      </Card>

      <Modal
        open={!!editUserId}
        onClose={() => { setEditUserId(null); setEditTitle('') }}
        title={t('portalStaffPage.editTitleModal')}
        size="sm"
        footer={
          <>
            <button
              type="button"
              onClick={() => { setEditUserId(null); setEditTitle('') }}
              disabled={editSaving}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="staff-edit-title-form"
              disabled={editSaving}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {t('portalStaffPage.saveTitle')}
            </button>
          </>
        }
      >
        <form id="staff-edit-title-form" onSubmit={(e) => { e.preventDefault(); handleSaveEdit() }}>
          <FormField label={t('portalStaffPage.colTitle')}>
            <input
              type="text"
              placeholder={t('portalStaffPage.titlePlaceholder')}
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              className="modal-input"
            />
          </FormField>
        </form>
      </Modal>

      <Modal
        open={!!resetPwUserId}
        onClose={() => { setResetPwUserId(null); setResetPwValue(''); setResetPwSuccess(false) }}
        title={t('portalStaffPage.resetPasswordTitle')}
        size="sm"
        footer={
          <>
            <button
              type="button"
              onClick={() => { setResetPwUserId(null); setResetPwValue(''); setResetPwSuccess(false) }}
              disabled={resetPwSaving}
              className="dashboard-btn-secondary"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              form="staff-reset-pw-form"
              disabled={resetPwSaving || !resetPwValue.trim()}
              className="dashboard-btn-primary disabled:opacity-50"
            >
              {resetPwSaving ? t('ui.saving') : t('portalStaffPage.resetPassword')}
            </button>
          </>
        }
      >
        <form id="staff-reset-pw-form" onSubmit={handleResetPassword} className="space-y-3">
          {resetPwSuccess && (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800 dark:border-emerald-800/50 dark:bg-emerald-900/20 dark:text-emerald-300">
              {t('portalStaffPage.resetPasswordSuccess')}
            </div>
          )}
          <FormField label={t('portalStaffPage.newPassword')}>
            <PasswordInput
              required
              minLength={6}
              placeholder={t('portalStaffPage.newPasswordPlaceholder')}
              value={resetPwValue}
              onChange={(e) => setResetPwValue(e.target.value)}
              className="modal-input"
            />
          </FormField>
        </form>
      </Modal>
    </>
  )
}
