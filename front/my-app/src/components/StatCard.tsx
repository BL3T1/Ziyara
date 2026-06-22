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
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
    <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
    <circle cx="12" cy="7" r="4" />
  </svg>
)

const ActivityIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
    <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
  </svg>
)

const ServerIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
    <rect width="20" height="8" x="2" y="2" rx="2" ry="2" />
    <rect width="20" height="8" x="2" y="14" rx="2" ry="2" />
    <path d="M6 6h.01M6 18h.01" />
  </svg>
)

const BellIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
    <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
    <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
  </svg>
)

const TicketIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
    <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z" />
    <path d="M13 5v2" /><path d="M13 17v2" /><path d="M13 11v2" />
  </svg>
)

const TrendUpIcon = () => (
  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="22 7 13.5 15.5 8.5 10.5 2 17" />
    <polyline points="16 7 22 7 22 13" />
  </svg>
)

const TrendDownIcon = () => (
  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="22 17 13.5 8.5 8.5 13.5 2 7" />
    <polyline points="16 17 22 17 22 11" />
  </svg>
)

export const StatCardIcons = { UserIcon, ActivityIcon, ServerIcon, BellIcon, TicketIcon }

export function StatCard({ icon, label, value, trend, trendPositive = true }: StatCardProps) {
  return (
    <Card>
      <div className="flex flex-col gap-4 [container-type:inline-size]">
        {icon && (
          <div className="relative flex h-11 w-11 shrink-0 items-center justify-center">
            {/* Glow behind icon */}
            <div className="absolute inset-0 rounded-xl bg-[#1e4d6b]/20 blur-md dark:bg-[#1e4d6b]/35" aria-hidden />
            <div className="relative flex h-11 w-11 items-center justify-center rounded-xl border border-[#1e4d6b]/20 bg-gradient-to-br from-[#1e4d6b]/15 to-[#1e4d6b]/[0.07] text-[#1e4d6b] ring-1 ring-inset ring-white/10 transition-all duration-300 group-hover:border-[#1e4d6b]/35 dark:border-[#1e4d6b]/30 dark:from-[#1e4d6b]/25 dark:to-[#1e4d6b]/10 dark:text-[#90caff] dark:group-hover:border-[#1e4d6b]/50">
              {icon}
            </div>
          </div>
        )}

        <div className="flex flex-col gap-1">
          <p className="text-xs font-semibold uppercase tracking-[0.1em] text-slate-400 dark:text-slate-500">{label}</p>
          <p className="font-bold tabular-nums leading-none tracking-tight text-slate-900 [font-size:clamp(1.5rem,18cqw,2.25rem)] [overflow-wrap:anywhere] dark:text-white">
            {value}
          </p>
        </div>

        {trend && (
          <div
            className={`flex items-center gap-1.5 text-xs font-semibold ${
              trendPositive
                ? 'text-emerald-600 dark:text-emerald-400'
                : 'text-red-600 dark:text-red-400'
            }`}
          >
            <span aria-hidden>
              {trendPositive ? <TrendUpIcon /> : <TrendDownIcon />}
            </span>
            <span>{trend}</span>
          </div>
        )}
      </div>
    </Card>
  )
}
