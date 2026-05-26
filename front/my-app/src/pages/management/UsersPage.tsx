/**
 * Management > Groups: Super Admin sees org groups; HR sees staff directory.
 */

import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { rolesAPI, usersAPI, getApiErrorMessage } from '../../services/api'
import type { GroupDto, GroupSummaryDto, PageDto, RoleDto, StaffDirectoryRoleOptionDto, UserDto } from '../../types/api'
import { Modal } from '../../components/Modal'
import { FormField } from '../../components/FormField'
import {
  UNGROUPED_GROUP_ID,
  buildFallbackStaffRoleOptions,
  findStaffRoleOption,
  mergeStaffRoleOptionsFromApi,
  staffRoleOptionKey,
} from '../../utils/staffRoleOptions'

function groupSortKey(code?: string): number {
  if (!code) return 999
  if (code === 'UNGROUPED') return 10000
  const m = /^G(\d+)$/i.exec(code.trim())
  if (m) return parseInt(m[1], 10)
  return 500
}

function compareGroupSummaries(a: GroupSummaryDto, b: GroupSummaryDto): number {
  const oa = groupSortKey(a.code)
  const ob = groupSortKey(b.code)
  if (oa !== ob) return oa - ob
  return a.name.localeCompare(b.name)
}

function mergeGroupSummaries(summaries: GroupSummaryDto[], allGroups: GroupDto[]): GroupSummaryDto[] {
  const byId = new Map(summaries.map((s) => [s.id, s]))
  const merged: GroupSummaryDto[] = [...summaries]
  for (const g of allGroups) {
    if (!byId.has(g.id)) {
      const row: GroupSummaryDto = {
        id: g.id,
        name: g.name,
        code: g.code,
        description: g.description,
        roleCount: 0,
        userCount: 0,
      }
      merged.push(row)
      byId.set(g.id, row)
    }
  }
  merged.sort(compareGroupSummaries)
  return merged
}

const CARD_ICON_COLORS = [
  'text-violet-600 dark:text-violet-400',
  'text-blue-600 dark:text-blue-400',
  'text-emerald-600 dark:text-emerald-400',
  'text-sky-600 dark:text-sky-400',
  'text-amber-600 dark:text-amber-400',
  'text-rose-600 dark:text-rose-400',
  'text-indigo-600 dark:text-indigo-400',
]

/** Platform org groups use codes matching C followed by digits (e.g. C1). */
function isReservedPlatformGroupCode(code?: string | null): boolean {
  if (!code) return false
  return /^C[0-9]+$/i.test(code.trim())
}

function PencilIcon({ className }: { className?: string }) {
  return (
    <svg className={className} xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4Z" />
    </svg>
  )
}

function TrashIcon({ className }: { className?: string }) {
  return (
    <svg className={className} xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      <path d="M3 6h18" />
      <path d="M8 6V4h8v2" />
      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
      <path d="M10 11v6M14 11v6" />
    </svg>
  )
}

function GroupIcon({ className }: { className: string }) {
  return (
    <svg
      className={`h-12 w-12 ${className}`}
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  )
}

export function UsersPage() {
  const { t } = useLanguage()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [groups, setGroups] = useState<GroupSummaryDto[]>([])
  const [roles, setRoles] = useState<RoleDto[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [showCreateGroup, setShowCreateGroup] = useState(false)
  const [editingGroup, setEditingGroup] = useState<GroupSummaryDto | null>(null)
  const [error, setError] = useState('')
  const [, setActionLoading] = useState(false)
  const [staffUsers, setStaffUsers] = useState<UserDto[]>([])
  const [staffRoleOptions, setStaffRoleOptions] = useState<StaffDirectoryRoleOptionDto[]>([])

  const buildFallback = useCallback((): StaffDirectoryRoleOptionDto[] => buildFallbackStaffRoleOptions(t), [t])

  const canSeeGroups = user?.role === 'super_admin'
  const isHr = user?.role === 'hr'
  const canAccess = canSeeGroups || isHr
  const canCreateUser = canSeeGroups || isHr
  const needsStaffDirectory = isHr && !canSeeGroups

  const load = useCallback(() => {
    if (!canAccess) return
    setLoading(true)
    setError('')
    const groupPromise = canSeeGroups
      ? Promise.all([
          rolesAPI.getGroupSummaries().then((r) => (Array.isArray(r.data) ? r.data : []) as GroupSummaryDto[]),
          rolesAPI.list().then((r) => (Array.isArray(r.data) ? r.data : []) as RoleDto[]),
          rolesAPI.getGroups().then((r) => (Array.isArray(r.data) ? r.data : []) as GroupDto[]),
        ]).then(([summaries, ro, allGroups]) => {
          setGroups(mergeGroupSummaries(summaries, allGroups))
          setRoles(ro)
        })
      : Promise.resolve().then(() => {
          setGroups([])
          setRoles([])
        })

    const staffListPromise = needsStaffDirectory
      ? usersAPI.list({ page: 0, size: 500 }).then((r) => {
          const p = r.data as PageDto<UserDto>
          return p?.content && Array.isArray(p.content) ? p.content : []
        })
      : Promise.resolve([] as UserDto[])

    const staffRolePromise =
      canCreateUser
        ? usersAPI
            .getStaffRoleOptions()
            .then((r) => {
              const data = r.data as StaffDirectoryRoleOptionDto[]
              const fallback = buildFallback()
              if (!Array.isArray(data) || data.length === 0) {
                return fallback
              }
              return mergeStaffRoleOptionsFromApi(data, fallback)
            })
            .catch(() => buildFallback())
        : Promise.resolve().then(() => [] as StaffDirectoryRoleOptionDto[])

    Promise.all([groupPromise, staffListPromise, staffRolePromise])
      .then(([, staff, staffOpts]) => {
        setStaffUsers(staff)
        setStaffRoleOptions(staffOpts)
      })
      .catch((e) => {
        setError(getApiErrorMessage(e, t('usersPage.failedLoadGroups')))
        setGroups([])
        setRoles([])
        setStaffUsers([])
        setStaffRoleOptions(canCreateUser ? buildFallback() : [])
      })
      .finally(() => setLoading(false))
  }, [canAccess, canSeeGroups, needsStaffDirectory, canCreateUser, buildFallback, t])

  useEffect(() => {
    load()
  }, [load])

  if (!user) return null

  if (!canAccess) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-900/20">
        <h2 className="text-xl font-semibold text-amber-800 dark:text-amber-200">{t('access.restrictedTitle')}</h2>
        <p className="mt-2 text-amber-700 dark:text-amber-300">{t('access.staffPageSuperAdminOrHr', { role: user.role })}</p>
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

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="app-page-title">
            {isHr && !canSeeGroups ? t('usersPage.titleHr') : t('usersPage.title')}
          </h1>
        </div>
        <div className="flex flex-wrap gap-2">
          {canSeeGroups && (
            <Link to="/admin/roles" className="dashboard-btn-secondary">
              {t('usersPage.rolesPermissions')}
            </Link>
          )}
          {canSeeGroups && (
            <button type="button" onClick={() => setShowCreateGroup(true)} className="dashboard-btn-secondary">
              {t('usersPage.createGroupButton')}
            </button>
          )}
          {canCreateUser && (
            <button type="button" onClick={() => setShowCreate(true)} className="dashboard-btn-primary shrink-0">
              {t('usersPage.createUserButton')}
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-300">
          {error}
        </div>
      )}

      {isHr && !canSeeGroups && (
        <p className="mt-6 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600 dark:border-slate-600 dark:bg-slate-800/50 dark:text-slate-300">
          {t('usersPage.groupsSuperAdminOnly')}
        </p>
      )}

      {loading ? (
        <div className="mt-8 p-8 text-center text-slate-500 dark:text-slate-400">{t('usersPage.loadingGroups')}</div>
      ) : canSeeGroups ? (
        <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {groups.map((g, i) => {
            const rolesInGroup =
              g.id === UNGROUPED_GROUP_ID ? roles.filter((r) => !r.groupId) : roles.filter((r) => r.groupId === g.id)
            const iconColor = CARD_ICON_COLORS[i % CARD_ICON_COLORS.length]
            const isSyntheticUngrouped = g.id === UNGROUPED_GROUP_ID
            const platform = isReservedPlatformGroupCode(g.code)
            const canDeleteGroup =
              !isSyntheticUngrouped && (g.roleCount ?? 0) === 0 && (g.userCount ?? 0) === 0
            return (
              <div
                key={g.id}
                className="flex flex-col rounded-xl border border-slate-200 bg-white p-6 text-left shadow-sm ring-1 ring-slate-200 dark:border-slate-700 dark:bg-slate-800 dark:ring-slate-700"
              >
                <div className="flex items-start gap-4">
                  <div className={`flex h-14 w-14 shrink-0 items-center justify-center rounded-xl bg-slate-100 dark:bg-slate-900/60`}>
                    <GroupIcon className={iconColor} />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-2">
                      <h2 className="min-w-0 flex-1 text-lg font-bold leading-snug text-slate-900 dark:text-slate-100">{g.name}</h2>
                      {!isSyntheticUngrouped && (
                        <div className="flex shrink-0 items-center gap-0.5">
                          <button
                            type="button"
                            onClick={() => {
                              setError('')
                              setEditingGroup(g)
                            }}
                            className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-primary dark:text-slate-400 dark:hover:bg-slate-700 dark:hover:text-primary"
                            title={t('usersPage.editGroupAria')}
                            aria-label={t('usersPage.editGroupAria')}
                          >
                            <PencilIcon className="h-5 w-5" />
                          </button>
                          <button
                            type="button"
                            disabled={!canDeleteGroup}
                            onClick={async () => {
                              if (!canDeleteGroup) return
                              const ok = window.confirm(t('usersPage.deleteGroupConfirm', { name: g.name }))
                              if (!ok) return
                              setError('')
                              try {
                                await rolesAPI.deleteGroup(g.id)
                                load()
                              } catch (err) {
                                setError(getApiErrorMessage(err, t('usersPage.deleteGroupFailed')))
                              }
                            }}
                            className="rounded-lg p-2 text-slate-500 hover:bg-red-50 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-slate-500 dark:text-slate-400 dark:hover:bg-red-950/40 dark:hover:text-red-400 dark:disabled:hover:bg-transparent"
                            title={canDeleteGroup ? t('usersPage.deleteGroupAria') : t('usersPage.deleteGroupDisabledInUse')}
                            aria-label={t('usersPage.deleteGroupAria')}
                          >
                            <TrashIcon className="h-5 w-5" />
                          </button>
                        </div>
                      )}
                    </div>
                    {g.code && (
                      <span className="mt-1 inline-block rounded-md bg-slate-100 px-2 py-0.5 font-mono text-xs text-slate-600 dark:bg-slate-900 dark:text-slate-400">
                        {g.code}
                      </span>
                    )}
                  </div>
                </div>
                {g.description && <p className="mt-4 text-sm leading-relaxed text-slate-600 dark:text-slate-300">{g.description}</p>}
                <p className="mt-4 text-sm font-medium text-slate-700 dark:text-slate-200">
                  {t('usersPage.groupStats', { roleCount: g.roleCount, userCount: g.userCount })}
                </p>
                <details className="mt-4 border-t border-slate-100 pt-4 dark:border-slate-700">
                  <summary className="cursor-pointer text-sm font-semibold text-primary hover:underline">{t('usersPage.rolesInGroup')}</summary>
                  {rolesInGroup.length === 0 ? (
                    <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{t('usersPage.noRoles')}</p>
                  ) : (
                    <ul className="mt-2 space-y-1.5 text-sm text-slate-600 dark:text-slate-300">
                      {rolesInGroup.map((r) => (
                        <li key={r.id} className="flex justify-between gap-2">
                          <span className="font-medium text-slate-800 dark:text-slate-200">{r.name}</span>
                          <span className="shrink-0 text-slate-500 dark:text-slate-400">
                            {t('usersPage.userCount', { count: r.userCount ?? 0 })}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                </details>
                <button
                  type="button"
                  onClick={() =>
                    navigate(`/management/groups/${g.id}/members`, {
                      state: { groupName: g.name, groupCode: g.code },
                    })
                  }
                  className="mt-4 w-full rounded-lg border border-slate-200 py-2 text-sm font-medium text-primary hover:bg-slate-50 dark:border-slate-600 dark:hover:bg-slate-700/50"
                >
                  {t('usersPage.viewMembers')}
                </button>
              </div>
            )
          })}
        </div>
      ) : isHr ? (
        <div className="mt-8 table-shell overflow-x-auto">
          <table>
            <thead>
              <tr>
                <th className="px-4 py-3.5 text-start">{t('usersPage.staffColEmail')}</th>
                <th className="px-4 py-3.5 text-start">{t('usersPage.staffColRole')}</th>
                <th className="px-4 py-3.5 text-start">{t('usersPage.staffColStatus')}</th>
                <th className="px-4 py-3.5 text-end">{t('groupMembersPage.colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {staffUsers.map((u) => (
                <tr key={u.id}>
                  <td className="px-4 py-3 text-sm text-slate-800 dark:text-slate-100">{u.email}</td>
                  <td className="px-4 py-3 font-mono text-sm text-slate-600 dark:text-slate-300">{u.role}</td>
                  <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-400">{u.status ?? '—'}</td>
                  <td className="px-4 py-3 text-end text-sm">
                    <Link to={`/management/staff/${u.id}`} className="text-primary hover:underline">
                      {t('groupMembersPage.viewDetails')}
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {staffUsers.length === 0 && (
            <p className="p-4 text-center text-sm text-slate-500 dark:text-slate-400">{t('usersPage.staffEmpty')}</p>
          )}
        </div>
      ) : null}

      {!loading && canSeeGroups && groups.length === 0 && !error && (
        <p className="mt-8 text-center text-slate-500 dark:text-slate-400">{t('usersPage.noGroups')}</p>
      )}

      {showCreate && (
        <CreateUserModal
          onClose={() => setShowCreate(false)}
          onSuccess={() => {
            setShowCreate(false)
            load()
          }}
          roleOptions={staffRoleOptions}
          defaultRoleCode="SALES_MANAGER"
          setError={setError}
          setActionLoading={setActionLoading}
        />
      )}

      {showCreateGroup && (
        <CreateGroupModal
          onClose={() => setShowCreateGroup(false)}
          onSuccess={() => {
            setShowCreateGroup(false)
            setError('')
            load()
          }}
          setError={setError}
        />
      )}

      {editingGroup && (
        <EditGroupModal
          group={editingGroup}
          onClose={() => setEditingGroup(null)}
          onSuccess={() => {
            setEditingGroup(null)
            setError('')
            load()
          }}
          setError={setError}
        />
      )}
    </>
  )
}

function CreateGroupModal({
  onClose,
  onSuccess,
  setError,
}: {
  onClose: () => void
  onSuccess: () => void
  setError: (s: string) => void
}) {
  const { t } = useLanguage()
  const [name, setName] = useState('')
  const [code, setCode] = useState('')
  const [description, setDescription] = useState('')
  const [nameAr, setNameAr] = useState('')
  const [descriptionAr, setDescriptionAr] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLocalError('')
    setError('')
    if (!name.trim()) {
      setLocalError(t('usersPage.errGroupName'))
      return
    }
    setSubmitting(true)
    try {
      await rolesAPI.createGroup({
        name: name.trim(),
        code: code.trim() || undefined,
        description: description.trim() || undefined,
        nameAr: nameAr.trim() || undefined,
        descriptionAr: descriptionAr.trim() || undefined,
      })
      onSuccess()
    } catch (err) {
      const msg = getApiErrorMessage(err)
      setLocalError(msg)
      setError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t('usersPage.createGroupTitle')}
      description={t('usersPage.createGroupHint')}
      size="md"
      footer={
        <>
          <button type="button" onClick={onClose} disabled={submitting} className="dashboard-btn-secondary">
            {t('ui.cancel')}
          </button>
          <button type="submit" form="group-create-form" disabled={submitting} className="dashboard-btn-primary disabled:opacity-70">
            {t('usersPage.createGroupSubmit')}
          </button>
        </>
      }
    >
      {localError && (
        <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
          {localError}
        </div>
      )}
      <form id="group-create-form" onSubmit={handleSubmit} className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label={t('usersPage.groupNameLabel')} required>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="modal-input"
              required
            />
          </FormField>
          <FormField label={t('usersPage.groupCodeLabel')} hint={t('usersPage.groupCodeHint')}>
            <input
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="C8"
              className="modal-input font-mono"
            />
          </FormField>
        </div>
        <FormField label={t('usersPage.groupDescriptionLabel')}>
          <input
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="modal-input"
          />
        </FormField>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label={t('rolesPage.nameArLabel')}>
            <input
              type="text"
              value={nameAr}
              onChange={(e) => setNameAr(e.target.value)}
              className="modal-input"
              dir="rtl"
            />
          </FormField>
          <FormField label={t('rolesPage.descriptionArLabel')}>
            <input
              type="text"
              value={descriptionAr}
              onChange={(e) => setDescriptionAr(e.target.value)}
              className="modal-input"
              dir="rtl"
            />
          </FormField>
        </div>
      </form>
    </Modal>
  )
}

function EditGroupModal({
  group,
  onClose,
  onSuccess,
  setError,
}: {
  group: GroupSummaryDto
  onClose: () => void
  onSuccess: () => void
  setError: (s: string) => void
}) {
  const { t } = useLanguage()
  const platform = isReservedPlatformGroupCode(group.code)
  const [name, setName] = useState(group.name)
  const [code, setCode] = useState(group.code ?? '')
  const [description, setDescription] = useState(group.description ?? '')
  const [nameAr, setNameAr] = useState('')
  const [descriptionAr, setDescriptionAr] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLocalError('')
    setError('')
    if (!name.trim()) {
      setLocalError(t('usersPage.errGroupName'))
      return
    }
    setSubmitting(true)
    try {
      const body: {
        name: string
        description?: string
        nameAr?: string
        descriptionAr?: string
        code?: string
      } = {
        name: name.trim(),
        description: description.trim() || undefined,
        nameAr: nameAr.trim() || undefined,
        descriptionAr: descriptionAr.trim() || undefined,
      }
      if (!platform && code.trim()) {
        body.code = code.trim()
      }
      await rolesAPI.updateGroup(group.id, body)
      onSuccess()
    } catch (err) {
      const msg = getApiErrorMessage(err)
      setLocalError(msg)
      setError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t('usersPage.editGroupTitle')}
      description={t('usersPage.editGroupHint')}
      size="md"
      footer={
        <>
          <button type="button" onClick={onClose} disabled={submitting} className="dashboard-btn-secondary">
            {t('ui.cancel')}
          </button>
          <button type="submit" form="group-edit-form" disabled={submitting} className="dashboard-btn-primary disabled:opacity-70">
            {t('usersPage.editGroupSubmit')}
          </button>
        </>
      }
    >
      {platform && (
        <div className="mb-3 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-600 dark:border-white/[0.06] dark:bg-white/[0.03] dark:text-slate-400">
          {t('usersPage.editGroupCodeLocked')}
        </div>
      )}
      {localError && (
        <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
          {localError}
        </div>
      )}
      <form id="group-edit-form" onSubmit={handleSubmit} className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label={t('usersPage.groupNameLabel')} required>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="modal-input"
              required
            />
          </FormField>
          <FormField label={t('usersPage.groupCodeLabel')} hint={platform ? t('usersPage.editGroupCodeLocked') : undefined}>
            <input
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              disabled={platform}
              placeholder="C8"
              className="modal-input font-mono disabled:cursor-not-allowed disabled:opacity-60"
            />
          </FormField>
        </div>
        <FormField label={t('usersPage.groupDescriptionLabel')}>
          <input
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="modal-input"
          />
        </FormField>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label={t('rolesPage.nameArLabel')}>
            <input
              type="text"
              value={nameAr}
              onChange={(e) => setNameAr(e.target.value)}
              className="modal-input"
              dir="rtl"
              placeholder={t('usersPage.editGroupOptionalAr')}
            />
          </FormField>
          <FormField label={t('rolesPage.descriptionArLabel')}>
            <input
              type="text"
              value={descriptionAr}
              onChange={(e) => setDescriptionAr(e.target.value)}
              className="modal-input"
              dir="rtl"
              placeholder={t('usersPage.editGroupOptionalAr')}
            />
          </FormField>
        </div>
      </form>
    </Modal>
  )
}

function CreateUserModal({
  onClose,
  onSuccess,
  roleOptions,
  defaultRoleCode,
  setError,
  setActionLoading,
}: {
  onClose: () => void
  onSuccess: () => void
  roleOptions: StaffDirectoryRoleOptionDto[]
  defaultRoleCode: string
  setError: (s: string) => void
  setActionLoading: (b: boolean) => void
}) {
  const { t } = useLanguage()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [phone, setPhone] = useState('')
  const [roleSelectionKey, setRoleSelectionKey] = useState(() => `enum:${defaultRoleCode}`)
  const [status, setStatus] = useState('ACTIVE')
  const [localError, setLocalError] = useState('')

  const builtInRoleOptions = roleOptions.filter((o) => o.source !== 'CUSTOM')
  const customRoleOptions = roleOptions.filter((o) => o.source === 'CUSTOM')

  useEffect(() => {
    if (roleOptions.length === 0) return
    if (findStaffRoleOption(roleOptions, roleSelectionKey)) return
    const preferred = roleOptions.find((o) => o.securityUserRole === defaultRoleCode || o.code === defaultRoleCode)
    const next = preferred ?? roleOptions[0]
    if (next) setRoleSelectionKey(staffRoleOptionKey(next))
  }, [roleOptions, defaultRoleCode, roleSelectionKey])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLocalError('')
    setError('')
    if (!email.trim()) {
      setLocalError(t('usersPage.errEmail'))
      return
    }
    if (!password || password.length < 6) {
      setLocalError(t('usersPage.errPassword'))
      return
    }
    if (roleOptions.length === 0 || !roleSelectionKey) {
      setLocalError(t('usersPage.loadingGroups'))
      return
    }
    const sel = findStaffRoleOption(roleOptions, roleSelectionKey)
    if (!sel) {
      setLocalError(t('usersPage.loadingGroups'))
      return
    }
    setActionLoading(true)
    try {
      const base = {
        email: email.trim().toLowerCase(),
        password,
        phone: phone.trim() || undefined,
        status: status || undefined,
      }
      const body =
        sel.rbacRoleId != null && sel.rbacRoleId !== ''
          ? { ...base, primaryRbacRoleId: sel.rbacRoleId }
          : { ...base, role: sel.code }
      await usersAPI.create(body)
      onSuccess()
    } catch (err) {
      const msg = getApiErrorMessage(err)
      setLocalError(msg)
      setError(msg)
    } finally {
      setActionLoading(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t('usersPage.modalTitle')}
      size="md"
      footer={
        <>
          <button type="button" onClick={onClose} className="dashboard-btn-secondary">
            {t('usersPage.cancel')}
          </button>
          <button
            type="submit"
            form="user-create-form"
            disabled={roleOptions.length === 0}
            className="dashboard-btn-primary disabled:opacity-60"
          >
            {t('usersPage.create')}
          </button>
        </>
      }
    >
      {localError && (
        <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
          {localError}
        </div>
      )}
      <form id="user-create-form" onSubmit={handleSubmit} className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label={t('usersPage.emailLabel')} required>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="modal-input"
              required
            />
          </FormField>
          <FormField label={t('usersPage.phoneLabel')}>
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="+966 5x xxx xxxx"
              className="modal-input"
            />
          </FormField>
        </div>
        <FormField label={t('usersPage.passwordLabel')} required>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="modal-input"
            minLength={6}
            autoComplete="new-password"
            required
          />
        </FormField>
        <FormField label={t('usersPage.roleLabel')} required>
          <select
            value={roleSelectionKey}
            onChange={(e) => setRoleSelectionKey(e.target.value)}
            disabled={roleOptions.length === 0}
            className="modal-select disabled:opacity-60"
          >
            {roleOptions.length === 0 ? (
              <option value="">{t('usersPage.loadingGroups')}</option>
            ) : (
              <>
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
              </>
            )}
          </select>
          {roleOptions.length > 0 && (
            <p className="mt-1.5 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
              <span className="font-medium text-slate-600 dark:text-slate-300">{t('usersPage.derivedOrgGroupLabel')}: </span>
              {(() => {
                const sel = findStaffRoleOption(roleOptions, roleSelectionKey)
                if (sel?.groupName && sel.groupCode) {
                  return t('usersPage.derivedOrgGroupWithCode', { name: sel.groupName, code: sel.groupCode })
                }
                if (sel?.groupName) {
                  return t('usersPage.derivedOrgGroupNameOnly', { name: sel.groupName })
                }
                return t('usersPage.derivedOrgGroupPending')
              })()}
            </p>
          )}
        </FormField>
        <FormField label={t('usersPage.statusLabel')}>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            className="modal-select"
          >
            <option value="ACTIVE">{t('usersPage.statusActive')}</option>
            <option value="PENDING_VERIFICATION">{t('usersPage.statusPending')}</option>
          </select>
        </FormField>
      </form>
    </Modal>
  )
}
