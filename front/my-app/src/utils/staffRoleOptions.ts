/**
 * Shared staff role option helpers for UsersPage and StaffUserDetailPage.
 */

import type { StaffDirectoryRoleOptionDto } from '../types/api'

export const UNGROUPED_GROUP_ID = '00000000-0000-4000-8000-0000000000ff'

export const COMPANY_STAFF_ROLE_META: { value: string; labelKey: string }[] = [
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

export function staffRoleOptionKey(o: StaffDirectoryRoleOptionDto): string {
  return o.rbacRoleId ?? `enum:${o.code}`
}

export function findStaffRoleOption(
  opts: StaffDirectoryRoleOptionDto[],
  key: string,
): StaffDirectoryRoleOptionDto | undefined {
  return opts.find((o) => staffRoleOptionKey(o) === key)
}

export function staffRoleOptionEnumCode(o: StaffDirectoryRoleOptionDto): string {
  return (o.securityUserRole ?? o.code ?? '').toUpperCase()
}

export function mergeStaffRoleOptionsFromApi(
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

export function buildFallbackStaffRoleOptions(t: (key: string) => string): StaffDirectoryRoleOptionDto[] {
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
}
