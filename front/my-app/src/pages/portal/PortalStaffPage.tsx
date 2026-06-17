/**
 * Provider portal > Staff: signed-in account, provider org, and team list (GET /portal/staff).
 */

import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../../context/LanguageContext'
import { usersAPI, providersAPI, portalStaffAPI, getApiErrorMessage } from '../../services/api'
import { Card } from '../../components/Card'
import type { LinkableUserDto, PortalStaffMemberDto, ServiceProviderDto } from '../../types/api'

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
  const [user, setUser] = useState<MeUser | null>(null)
  const [provider, setProvider] = useState<ServiceProviderDto | null>(null)
  const [team, setTeam] = useState<PortalStaffMemberDto[]>([])
  const [loading, setLoading] = useState(true)
  const [teamLoading, setTeamLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [linkableUsers, setLinkableUsers] = useState<LinkableUserDto[]>([])
  const [addUserId, setAddUserId] = useState('')
  const [addTitle, setAddTitle] = useState('')
  const [adding, setAdding] = useState(false)
  const [newEmail, setNewEmail] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [newPhone, setNewPhone] = useState('')
  const [newRole, setNewRole] = useState('PROVIDER_STAFF')
  const [newTitle, setNewTitle] = useState('')
  const [creatingUser, setCreatingUser] = useState(false)
  const [editUserId, setEditUserId] = useState<string | null>(null)
  const [editTitle, setEditTitle] = useState('')
  const [editSaving, setEditSaving] = useState(false)
  const [removingId, setRemovingId] = useState<string | null>(null)

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
    portalStaffAPI
      .listLinkable()
      .then((r) => setLinkableUsers(Array.isArray(r.data) ? r.data : []))
      .catch(() => setLinkableUsers([]))
  }, [])

  const row = (label: string, value: string) => (
    <div className="flex flex-wrap justify-between gap-2 border-b border-slate-100 py-2.5 last:border-0 dark:border-slate-700/80">
      <span className="text-sm text-slate-500 dark:text-slate-400">{label}</span>
      <span className="text-sm font-medium text-slate-900 dark:text-slate-100">{value}</span>
    </div>
  )

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault()
    const uid = addUserId.trim()
    if (!uid) return
    setAdding(true)
    setError(null)
    try {
      const body: { userId: string; title?: string } = { userId: uid }
      const t = addTitle.trim()
      if (t) body.title = t
      await portalStaffAPI.add(body)
      setAddUserId('')
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
    setCreatingUser(true)
    setError(null)
    try {
      const body: { email: string; password: string; roleId: string; phone?: string; title?: string } = {
        email: newEmail.trim(),
        password: newPassword,
        roleId: newRole,
      }
      const p = newPhone.trim()
      if (p) body.phone = p
      const t = newTitle.trim()
      if (t) body.title = t
      await portalStaffAPI.createUser(body)
      setNewEmail('')
      setNewPassword('')
      setNewPhone('')
      setNewRole('PROVIDER_STAFF')
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
                      {!m.owner && (
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

        <form onSubmit={handleAdd} className="mt-8 max-w-xl space-y-3 rounded-xl border border-slate-200 p-4 dark:border-slate-700">
          <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">{t('portalStaffPage.addMemberTitle')}</h3>
          <p className="text-xs text-slate-500 dark:text-slate-400">{t('portalStaffPage.addMemberHint')}</p>
          <select
            required
            value={addUserId}
            onChange={(e) => setAddUserId(e.target.value)}
            className="w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            <option value="">{t('portalStaffPage.searchUserPlaceholder')}</option>
            {linkableUsers.map((u) => (
              <option key={u.id} value={u.id}>
                {u.name || u.email}{u.phone ? ` · ${u.phone}` : ''}
              </option>
            ))}
          </select>
          <input
            type="text"
            placeholder={t('portalStaffPage.titlePlaceholder')}
            value={addTitle}
            onChange={(e) => setAddTitle(e.target.value)}
            className="w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          />
          <button
            type="submit"
            disabled={adding}
            className="dashboard-btn-primary disabled:opacity-50"
          >
            {t('portalStaffPage.addMember')}
          </button>
        </form>

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
          <input
            type="password"
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
            value={newRole}
            onChange={(e) => setNewRole(e.target.value)}
            className="w-full rounded border border-slate-300 px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
          >
            <option value="PROVIDER_STAFF">{t('portalStaffPage.roleProviderStaff')}</option>
            <option value="PROVIDER_FINANCE">{t('portalStaffPage.roleProviderFinance')}</option>
            <option value="TAXI_OPERATOR">{t('portalStaffPage.roleTaxiOperator')}</option>
            <option value="PROVIDER_MANAGER">{t('portalStaffPage.roleProviderManager')}</option>
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
            disabled={creatingUser}
            className="dashboard-btn-primary disabled:opacity-50"
          >
            {t('portalStaffPage.createMember')}
          </button>
        </form>

        <Link
          to="/portal/support"
          className="mt-6 inline-flex rounded-xl bg-primary/10 px-4 py-2 text-sm font-medium text-primary hover:bg-primary/20"
        >
          {t('portalStaffPage.contactSupportLink')}
        </Link>
      </Card>

      {editUserId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-800">
            <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('portalStaffPage.editTitleModal')}</h2>
            <input
              type="text"
              placeholder={t('portalStaffPage.titlePlaceholder')}
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              className="mt-3 w-full rounded border border-slate-300 px-3 py-2 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
            <div className="mt-4 flex gap-2">
              <button
                type="button"
                onClick={handleSaveEdit}
                disabled={editSaving}
                className="dashboard-btn-primary disabled:opacity-50"
              >
                {t('portalStaffPage.saveTitle')}
              </button>
              <button
                type="button"
                onClick={() => { setEditUserId(null); setEditTitle(''); }}
                className="rounded-xl border border-slate-300 px-4 py-2 text-sm dark:border-slate-600 dark:text-slate-200"
              >
                {t('ui.cancel')}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
