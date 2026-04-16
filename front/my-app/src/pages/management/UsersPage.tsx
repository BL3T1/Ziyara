/**
 * Management > Groups: Super Admin sees org groups + RBAC; HR sees staff directory + RBAC assignment.
 */

import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { rolesAPI, usersAPI, getApiErrorMessage } from '../../services/api'
import type {
  GroupDto,
  GroupSummaryDto,
  PageDto,
  RbacRoleOptionDto,
  RoleDto,
  StaffDirectoryRoleOptionDto,
  UserDto,
  UserRbacAssignmentDto,
} from '../../types/api'

/** Loose email check for Super Admin manual RBAC target field */
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Synthetic group id from backend for roles with no group (see RoleManagementService). */
const UNGROUPED_GROUP_ID = '00000000-0000-4000-8000-0000000000ff'

/** Fallback labels if GET /users/staff-role-options fails; codes must stay aligned with backend UserRole. */
const COMPANY_STAFF_ROLE_META: { value: string; labelKey: string }[] = [
  { value: 'SALES_MANAGER', labelKey: 'usersPage.roleSalesManager' },
  { value: 'CEO', labelKey: 'usersPage.roleCeo' },
  { value: 'GENERAL_MANAGER', labelKey: 'usersPage.roleGeneralManager' },
  { value: 'HR_MANAGER', labelKey: 'usersPage.roleHrManager' },
  { value: 'SALES_REPRESENTATIVE', labelKey: 'usersPage.roleSalesRep' },
  { value: 'FINANCE_MANAGER', labelKey: 'usersPage.roleFinanceManager' },
  { value: 'ACCOUNTANT', labelKey: 'usersPage.roleAccountant' },
  { value: 'SUPPORT_MANAGER', labelKey: 'usersPage.roleSupportManager' },
  { value: 'SUPPORT_AGENT', labelKey: 'usersPage.roleSupportAgent' },
]

function groupSortKey(code?: string): number {
  if (!code) return 999
  if (code === 'UNGROUPED') return 10000
  const m = /^G(\d+)$/i.exec(code.trim())
  if (m) return parseInt(m[1], 10)
  return 500
}

function staffRoleOptionKey(o: StaffDirectoryRoleOptionDto): string {
  return o.rbacRoleId ?? `enum:${o.code}`
}

function findStaffRoleOption(
  opts: StaffDirectoryRoleOptionDto[],
  key: string,
): StaffDirectoryRoleOptionDto | undefined {
  return opts.find((o) => staffRoleOptionKey(o) === key)
}

/** Enum code for deduping API rows vs static fallback (backend UserRole name). */
function staffRoleOptionEnumCode(o: StaffDirectoryRoleOptionDto): string {
  return (o.securityUserRole ?? o.code ?? '').toUpperCase()
}

/**
 * API may return only sys_roles rows that exist in DB (e.g. a single SALES_MANAGER). Merge in the full static
 * staff list so Super Admin / HR always see every built-in role; keep custom RBAC rows from the API.
 * When both exist for a code, prefer the API row (has rbacRoleId when DB row exists).
 */
function mergeStaffRoleOptionsFromApi(
  api: StaffDirectoryRoleOptionDto[],
  fallback: StaffDirectoryRoleOptionDto[],
): StaffDirectoryRoleOptionDto[] {
  const custom = api.filter((o) => o.source === 'CUSTOM')
  const systemCandidates = api.filter((o) => o.source !== 'CUSTOM')
  const byEnumCode = new Map<string, StaffDirectoryRoleOptionDto>()

  for (const o of systemCandidates) {
    const c = staffRoleOptionEnumCode(o)
    if (!c) continue
    const prev = byEnumCode.get(c)
    if (!prev || (Boolean(o.rbacRoleId) && !prev.rbacRoleId)) {
      byEnumCode.set(c, { ...o, source: (o.source ?? 'SYSTEM') as StaffDirectoryRoleOptionDto['source'] })
    }
  }

  for (const f of fallback) {
    const c = f.code.toUpperCase()
    if (!byEnumCode.has(c)) {
      byEnumCode.set(c, f)
    }
  }

  const systemMerged = Array.from(byEnumCode.values())
  systemMerged.sort((a, b) =>
    a.displayName.localeCompare(b.displayName, undefined, { sensitivity: 'base' }),
  )
  custom.sort((a, b) => a.displayName.localeCompare(b.displayName, undefined, { sensitivity: 'base' }))
  return [...systemMerged, ...custom]
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
  const [error, setError] = useState('')
  const [, setActionLoading] = useState(false)
  const [rbacPicklist, setRbacPicklist] = useState<RbacRoleOptionDto[]>([])
  const [staffUsers, setStaffUsers] = useState<UserDto[]>([])
  const [rbacStaffId, setRbacStaffId] = useState('')
  const [membersModal, setMembersModal] = useState<{ id: string; name: string } | null>(null)
  const [membersPage, setMembersPage] = useState(0)
  const [membersLoading, setMembersLoading] = useState(false)
  const [membersError, setMembersError] = useState('')
  const [membersRows, setMembersRows] = useState<UserDto[]>([])
  const [membersTotalPages, setMembersTotalPages] = useState(0)
  const [rbacUserEmail, setRbacUserEmail] = useState('')
  const [rbacRoleId, setRbacRoleId] = useState('')
  const [rbacMsg, setRbacMsg] = useState('')
  const [rbacBusy, setRbacBusy] = useState(false)
  const [staffRoleOptions, setStaffRoleOptions] = useState<StaffDirectoryRoleOptionDto[]>([])

  const buildFallbackStaffRoleOptions = useCallback((): StaffDirectoryRoleOptionDto[] => {
    return COMPANY_STAFF_ROLE_META.map((m) => ({
      source: 'SYSTEM' as const,
      rbacRoleId: null,
      securityUserRole: m.value,
      code: m.value,
      displayName: t(m.labelKey),
      groupId: null,
      groupName: null,
      groupCode: null,
    }))
  }, [t])

  const canSeeGroups = user?.role === 'super_admin'
  const isHr = user?.role === 'hr'
  const canAssignRbac = canSeeGroups || isHr
  const canAccess = canAssignRbac
  const canCreateUser = canSeeGroups || isHr

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

    const rbacPromise = canAssignRbac
      ? Promise.all([
          usersAPI.getRbacCustomRoles().then((r) => (Array.isArray(r.data) ? r.data : []) as RbacRoleOptionDto[]),
          usersAPI.list({ page: 0, size: 500 }).then((r) => {
            const p = r.data as PageDto<UserDto>
            return p?.content && Array.isArray(p.content) ? p.content : []
          }),
        ]).then(([opts, staff]) => {
          setRbacPicklist(opts)
          setStaffUsers(staff)
        })
      : Promise.resolve().then(() => {
          setRbacPicklist([])
          setStaffUsers([])
        })

    const staffRolePromise =
      canCreateUser
        ? usersAPI
            .getStaffRoleOptions()
            .then((r) => {
              const data = r.data as StaffDirectoryRoleOptionDto[]
              const fallback = buildFallbackStaffRoleOptions()
              if (!Array.isArray(data) || data.length === 0) {
                return fallback
              }
              return mergeStaffRoleOptionsFromApi(data, fallback)
            })
            .catch(() => buildFallbackStaffRoleOptions())
        : Promise.resolve().then(() => [] as StaffDirectoryRoleOptionDto[])

    Promise.all([groupPromise, rbacPromise, staffRolePromise])
      .then(([, , staffOpts]) => {
        setStaffRoleOptions(staffOpts)
      })
      .catch((e) => {
        setError(getApiErrorMessage(e, t('usersPage.failedLoadGroups')))
        setGroups([])
        setRoles([])
        setRbacPicklist([])
        setStaffUsers([])
        setStaffRoleOptions(canCreateUser ? buildFallbackStaffRoleOptions() : [])
      })
      .finally(() => setLoading(false))
  }, [canAccess, canSeeGroups, canAssignRbac, canCreateUser, buildFallbackStaffRoleOptions, t])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (!membersModal) return
    let cancelled = false
    setMembersLoading(true)
    setMembersError('')
    rolesAPI
      .getGroupMembers(membersModal.id, { page: membersPage, size: 15 })
      .then((res) => {
        if (cancelled) return
        const d = res.data as PageDto<UserDto>
        setMembersRows(Array.isArray(d?.content) ? d.content : [])
        setMembersTotalPages(typeof d?.totalPages === 'number' ? d.totalPages : 0)
      })
      .catch((e) => {
        if (!cancelled) {
          setMembersError(getApiErrorMessage(e, t('usersPage.membersLoadError')))
          setMembersRows([])
          setMembersTotalPages(0)
        }
      })
      .finally(() => {
        if (!cancelled) setMembersLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [membersModal, membersPage, t])

  const effectiveRbacEmail = useMemo(() => {
    if (rbacStaffId) {
      const u = staffUsers.find((x) => x.id === rbacStaffId)
      const e = u?.email?.trim()
      return e ? e.toLowerCase() : ''
    }
    if (canSeeGroups) {
      const e = rbacUserEmail.trim()
      return e && EMAIL_RE.test(e) ? e.toLowerCase() : ''
    }
    return ''
  }, [rbacStaffId, rbacUserEmail, canSeeGroups, staffUsers])

  useEffect(() => {
    if (!effectiveRbacEmail) {
      setRbacRoleId('')
      return
    }
    let cancelled = false
    usersAPI
      .getUserRbacRoleByEmail(effectiveRbacEmail)
      .then((res) => {
        if (cancelled) return
        const d = res.data as UserRbacAssignmentDto
        setRbacRoleId(d?.roleId ? String(d.roleId) : '')
      })
      .catch(() => {
        if (!cancelled) setRbacRoleId('')
      })
    return () => {
      cancelled = true
    }
  }, [effectiveRbacEmail])

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

      {canAssignRbac && (
        <div className="mt-6 rounded-xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-700 dark:bg-slate-800/80">
          <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{t('usersPage.assignRbacTitle')}</h2>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{t('usersPage.assignRbacHint')}</p>
          {rbacMsg && (
            <p className="mt-2 text-sm text-emerald-700 dark:text-emerald-300" role="status">
              {rbacMsg}
            </p>
          )}
          <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-end">
            <div className="min-w-0 flex-1">
              <label className="mb-1 block text-xs font-medium text-slate-600 dark:text-slate-400">
                {t('usersPage.assignRbacStaffLabel')}
              </label>
              <select
                value={rbacStaffId}
                onChange={(e) => setRbacStaffId(e.target.value)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              >
                <option value="">{t('usersPage.assignRbacStaffPlaceholder')}</option>
                {staffUsers.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.email} ({u.role})
                  </option>
                ))}
              </select>
            </div>
            {canSeeGroups && (
              <div className="min-w-0 flex-1">
                <label className="mb-1 block text-xs font-medium text-slate-600 dark:text-slate-400">
                  {t('usersPage.assignRbacUserEmail')}
                </label>
                <input
                  type="email"
                  value={rbacUserEmail}
                  onChange={(e) => setRbacUserEmail(e.target.value)}
                  placeholder="staff@company.com"
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
                />
              </div>
            )}
            <div className="min-w-[12rem] flex-1">
              <label className="mb-1 block text-xs font-medium text-slate-600 dark:text-slate-400">
                {t('usersPage.assignRbacRoleLabel')}
              </label>
              <select
                value={rbacRoleId}
                onChange={(e) => setRbacRoleId(e.target.value)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              >
                <option value="">{t('usersPage.assignRbacRoleNone')}</option>
                {rbacPicklist.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name ?? r.code ?? r.id}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                disabled={rbacBusy || !effectiveRbacEmail}
                onClick={async () => {
                  setRbacMsg('')
                  setError('')
                  if (!effectiveRbacEmail) {
                    if (!rbacStaffId && canSeeGroups && rbacUserEmail.trim()) {
                      setError(t('usersPage.assignRbacErrEmail'))
                    } else {
                      setError(t('usersPage.assignRbacErrStaff'))
                    }
                    return
                  }
                  setRbacBusy(true)
                  try {
                    await usersAPI.assignRbacRoleByEmail(effectiveRbacEmail, {
                      roleId: rbacRoleId || undefined,
                    })
                    setRbacMsg(rbacRoleId ? t('usersPage.assignRbacSaved') : t('usersPage.assignRbacClearedMsg'))
                    load()
                  } catch (e) {
                    setError(getApiErrorMessage(e))
                  } finally {
                    setRbacBusy(false)
                  }
                }}
                className="dashboard-btn-primary disabled:opacity-60"
              >
                {t('usersPage.assignRbacSubmit')}
              </button>
              <button
                type="button"
                disabled={rbacBusy || !effectiveRbacEmail}
                onClick={async () => {
                  setRbacMsg('')
                  setError('')
                  if (!effectiveRbacEmail) {
                    if (!rbacStaffId && canSeeGroups && rbacUserEmail.trim()) {
                      setError(t('usersPage.assignRbacErrEmail'))
                    } else {
                      setError(t('usersPage.assignRbacErrStaff'))
                    }
                    return
                  }
                  setRbacBusy(true)
                  try {
                    await usersAPI.assignRbacRoleByEmail(effectiveRbacEmail, {})
                    setRbacRoleId('')
                    setRbacMsg(t('usersPage.assignRbacClearedMsg'))
                    load()
                  } catch (e) {
                    setError(getApiErrorMessage(e))
                  } finally {
                    setRbacBusy(false)
                  }
                }}
                className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-700"
              >
                {t('usersPage.assignRbacClear')}
              </button>
            </div>
          </div>
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
                    <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100">{g.name}</h2>
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
                  onClick={() => {
                    setMembersPage(0)
                    setMembersModal({ id: g.id, name: g.name })
                  }}
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
              </tr>
            </thead>
            <tbody>
              {staffUsers.map((u) => (
                <tr key={u.id}>
                  <td className="px-4 py-3 text-sm text-slate-800 dark:text-slate-100">{u.email}</td>
                  <td className="px-4 py-3 font-mono text-sm text-slate-600 dark:text-slate-300">{u.role}</td>
                  <td className="px-4 py-3 text-sm text-slate-600 dark:text-slate-400">{u.status ?? '—'}</td>
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

      {membersModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="max-h-[85vh] w-full max-w-2xl overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl dark:border-slate-700 dark:bg-slate-800">
            <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-slate-700">
              <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                {t('usersPage.membersModalTitle', { name: membersModal.name })}
              </h2>
              <button
                type="button"
                onClick={() => setMembersModal(null)}
                className="rounded-lg px-2 py-1 text-sm text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700"
              >
                {t('usersPage.cancel')}
              </button>
            </div>
            <div className="max-h-[calc(85vh-8rem)] overflow-y-auto p-4">
              {membersError && (
                <p className="mb-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900 dark:bg-red-950/40 dark:text-red-200">
                  {membersError}
                </p>
              )}
              {membersLoading ? (
                <p className="py-8 text-center text-slate-500">{t('usersPage.loadingGroups')}</p>
              ) : membersRows.length === 0 ? (
                <p className="py-8 text-center text-slate-500 dark:text-slate-400">{t('usersPage.membersEmpty')}</p>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 text-left dark:border-slate-600">
                      <th className="pb-2 pe-4 font-medium text-slate-700 dark:text-slate-300">{t('usersPage.membersColEmail')}</th>
                      <th className="pb-2 pe-4 font-medium text-slate-700 dark:text-slate-300">{t('usersPage.membersColRole')}</th>
                      <th className="pb-2 font-medium text-slate-700 dark:text-slate-300">{t('usersPage.membersColStatus')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {membersRows.map((u) => (
                      <tr key={u.id} className="border-b border-slate-100 dark:border-slate-700">
                        <td className="py-2 pe-4 text-slate-800 dark:text-slate-200">{u.email}</td>
                        <td className="py-2 pe-4 font-mono text-slate-600 dark:text-slate-400">{u.role ?? '—'}</td>
                        <td className="py-2 text-slate-600 dark:text-slate-400">{u.status ?? '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
            <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-200 px-4 py-3 dark:border-slate-700">
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {t('usersPage.membersPageOf', { page: membersPage + 1, total: Math.max(1, membersTotalPages) })}
              </p>
              <div className="flex gap-2">
                <button
                  type="button"
                  disabled={membersPage <= 0 || membersLoading}
                  onClick={() => setMembersPage((p) => Math.max(0, p - 1))}
                  className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm disabled:opacity-40 dark:border-slate-600"
                >
                  {t('usersPage.membersPrev')}
                </button>
                <button
                  type="button"
                  disabled={membersPage >= membersTotalPages - 1 || membersLoading || membersTotalPages === 0}
                  onClick={() => setMembersPage((p) => p + 1)}
                  className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm disabled:opacity-40 dark:border-slate-600"
                >
                  {t('usersPage.membersNext')}
                </button>
              </div>
            </div>
          </div>
        </div>
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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-700 dark:bg-slate-800"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">{t('usersPage.createGroupTitle')}</h3>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{t('usersPage.createGroupHint')}</p>
        {localError && (
          <div className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
            {localError}
          </div>
        )}
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">
              {t('usersPage.groupNameLabel')}
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              required
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">
              {t('usersPage.groupCodeLabel')}
            </label>
            <input
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="G8"
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 font-mono text-sm text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">
              {t('usersPage.groupDescriptionLabel')}
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">
              {t('rolesPage.nameArLabel')}
            </label>
            <input
              type="text"
              value={nameAr}
              onChange={(e) => setNameAr(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              dir="rtl"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">
              {t('rolesPage.descriptionArLabel')}
            </label>
            <input
              type="text"
              value={descriptionAr}
              onChange={(e) => setDescriptionAr(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              dir="rtl"
            />
          </div>
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-700"
            >
              {t('ui.cancel')}
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="dashboard-btn-primary flex-1 disabled:opacity-70"
            >
              {t('usersPage.createGroupSubmit')}
            </button>
          </div>
        </form>
      </div>
    </div>
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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-700 dark:bg-slate-800"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">{t('usersPage.modalTitle')}</h3>
        {localError && (
          <div className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-900/20 dark:text-red-300">
            {localError}
          </div>
        )}
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('usersPage.emailLabel')}</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              required
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('usersPage.passwordLabel')}</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
              minLength={6}
              required
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('usersPage.phoneLabel')}</label>
            <input
              type="text"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('usersPage.roleLabel')}</label>
            <select
              value={roleSelectionKey}
              onChange={(e) => setRoleSelectionKey(e.target.value)}
              disabled={roleOptions.length === 0}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100 disabled:opacity-60"
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
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-900 dark:text-slate-100">{t('usersPage.statusLabel')}</label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100"
            >
              <option value="ACTIVE">{t('usersPage.statusActive')}</option>
              <option value="PENDING_VERIFICATION">{t('usersPage.statusPending')}</option>
            </select>
          </div>
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-700"
            >
              {t('usersPage.cancel')}
            </button>
            <button
              type="submit"
              disabled={roleOptions.length === 0}
              className="dashboard-btn-primary flex-1 disabled:opacity-60"
            >
              {t('usersPage.create')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
