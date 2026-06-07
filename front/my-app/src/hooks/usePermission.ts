import { usePermissions } from '../context/PermissionsContext'

/** Returns true if the current user has the given permission code. Super admin always returns true. */
export function usePermission(code: string): boolean {
  return usePermissions().has(code)
}
