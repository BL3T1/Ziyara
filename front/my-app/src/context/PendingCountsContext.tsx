import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { dashboardAPI, adminMediaAPI, reviewsAPI } from '../services/api'
import { useAuth } from './AuthContext'

interface PendingCounts {
  complaints: number
  reviews: number
  media_approvals: number
}

const EMPTY: PendingCounts = { complaints: 0, reviews: 0, media_approvals: 0 }

const PendingCountsContext = createContext<PendingCounts>(EMPTY)

const POLL_INTERVAL_MS = 60_000

export function PendingCountsProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [counts, setCounts] = useState<PendingCounts>(EMPTY)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const fetch = async () => {
    if (!user) return
    try {
      const [kpisRes, mediaRes, reviewsRes] = await Promise.allSettled([
        dashboardAPI.getKpis(),
        adminMediaAPI.list(),
        reviewsAPI.listAdmin({ page: 0, size: 1, status: 'PENDING' }),
      ])

      const kpis = kpisRes.status === 'fulfilled'
        ? (kpisRes.value.data as Record<string, unknown>)
        : null

      const media = mediaRes.status === 'fulfilled'
        ? (Array.isArray(mediaRes.value.data) ? mediaRes.value.data as unknown[] : [])
        : []

      const reviewsData = reviewsRes.status === 'fulfilled'
        ? (reviewsRes.value.data as { totalElements?: number; content?: unknown[] } | null)
        : null

      setCounts({
        complaints: typeof kpis?.pendingComplaints === 'number' ? kpis.pendingComplaints : 0,
        reviews: typeof reviewsData?.totalElements === 'number' ? reviewsData.totalElements : 0,
        media_approvals: media.filter((m) => (m as { status?: string }).status === 'PENDING').length,
      })
    } catch {
      // silently ignore — badges are best-effort
    }
  }

  useEffect(() => {
    if (!user) {
      setCounts(EMPTY)
      return
    }
    void fetch()
    timerRef.current = setInterval(() => void fetch(), POLL_INTERVAL_MS)
    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [user])

  return (
    <PendingCountsContext.Provider value={counts}>
      {children}
    </PendingCountsContext.Provider>
  )
}

export function usePendingCounts() {
  return useContext(PendingCountsContext)
}
