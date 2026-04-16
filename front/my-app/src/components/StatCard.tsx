import type { ReactNode } from 'react'
import { Card } from './Card'

interface StatCardProps {
  icon?: ReactNode
  label: string
  value: string
  trend?: string
  trendPositive?: boolean
}

const UserIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
    <circle cx="12" cy="7" r="4" />
  </svg>
)

const ActivityIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
  </svg>
)

const ServerIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect width="20" height="8" x="2" y="2" rx="2" ry="2" />
    <rect width="20" height="8" x="2" y="14" rx="2" ry="2" />
    <path d="M6 6h.01M6 18h.01M6 10h.01M6 14h.01M18 6h.01M18 18h.01M18 10h.01M18 14h.01" />
  </svg>
)

const BellIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
    <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
  </svg>
)

const TicketIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z" />
    <path d="M13 5v2" /><path d="M13 17v2" /><path d="M13 11v2" />
  </svg>
)

export const StatCardIcons = { UserIcon, ActivityIcon, ServerIcon, BellIcon, TicketIcon }

export function StatCard({ icon, label, value, trend, trendPositive = true }: StatCardProps) {
  return (
    <Card>
      <div className="flex flex-col gap-3 [container-type:inline-size]">
        {icon && (
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-primary/12 to-primary/[0.06] text-primary shadow-inner shadow-primary/10 ring-1 ring-primary/15 transition-all duration-300 group-hover:from-primary/[0.16] group-hover:to-primary/10 group-hover:ring-primary/25 dark:from-primary/20 dark:to-primary/10 dark:ring-primary/25 dark:group-hover:ring-primary/35">
            {icon}
          </div>
        )}
        <p className="text-sm font-semibold text-slate-500 dark:text-slate-400">{label}</p>
        <p className="font-bold tabular-nums leading-tight tracking-tight text-slate-900 [font-size:clamp(1.125rem,16cqw,1.875rem)] [overflow-wrap:anywhere] dark:text-slate-50">
          {value}
        </p>
        {trend && (
          <p
            className={`inline-flex items-center gap-1 text-sm font-medium ${
              trendPositive ? 'text-emerald-600' : 'text-red-600'
            }`}
          >
            <span
              className={`inline-block ${trendPositive ? '' : 'rotate-180'}`}
              aria-hidden
            >
              ↑
            </span>
            {trend}
          </p>
        )}
      </div>
    </Card>
  )
}
