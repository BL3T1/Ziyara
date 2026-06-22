import type { ReactNode } from 'react'
import { usePermission } from '../hooks/usePermission'

interface Props {
  code: string
  children: ReactNode
  fallback?: ReactNode
}

export function PermissionGate({ code, children, fallback = null }: Props) {
  const allowed = usePermission(code)
  return allowed ? <>{children}</> : <>{fallback}</>
}
