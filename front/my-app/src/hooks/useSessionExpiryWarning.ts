import { useEffect, useRef } from 'react'
import { getStoredToken } from '../context/AuthContext'

function getTokenExpiry(token: string): number | null {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return typeof payload.exp === 'number' ? payload.exp * 1000 : null
  } catch {
    return null
  }
}

/**
 * Calls onWarning ~30 s before the JWT expires, and onExpired when it does.
 * Safe to call from any layout; clears timers on unmount or token change.
 */
export function useSessionExpiryWarning(
  onWarning: () => void,
  onExpired: () => void,
) {
  const warningRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const expiredRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    const token = getStoredToken()
    if (!token) return

    const expiry = getTokenExpiry(token)
    if (!expiry) return

    const now = Date.now()
    const msUntilExpiry = expiry - now
    if (msUntilExpiry <= 0) return

    const WARNING_BEFORE_MS = 30_000
    const warningIn = msUntilExpiry - WARNING_BEFORE_MS

    if (warningIn > 0) {
      warningRef.current = setTimeout(onWarning, warningIn)
    } else {
      onWarning()
    }

    expiredRef.current = setTimeout(onExpired, msUntilExpiry)

    return () => {
      if (warningRef.current) clearTimeout(warningRef.current)
      if (expiredRef.current) clearTimeout(expiredRef.current)
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [getStoredToken()])
}
