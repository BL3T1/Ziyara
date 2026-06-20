const BADGE_STYLES: Record<string, string> = {
  ACTIVE:           'bg-green-100  text-green-800  dark:bg-green-900/40  dark:text-green-300',
  APPROVED:         'bg-green-100  text-green-800  dark:bg-green-900/40  dark:text-green-300',
  PENDING:          'bg-amber-100  text-amber-800  dark:bg-amber-900/40  dark:text-amber-300',
  PENDING_APPROVAL: 'bg-amber-100  text-amber-800  dark:bg-amber-900/40  dark:text-amber-300',
  INACTIVE:         'bg-slate-100  text-slate-600  dark:bg-slate-700     dark:text-slate-300',
  SUSPENDED:        'bg-red-100    text-red-800    dark:bg-red-900/40    dark:text-red-300',
  REJECTED:         'bg-red-100    text-red-800    dark:bg-red-900/40    dark:text-red-300',
  EXPIRED:          'bg-red-100    text-red-700    dark:bg-red-900/40    dark:text-red-300',
  FULLY_REDEEMED:   'bg-blue-100   text-blue-800   dark:bg-blue-900/40   dark:text-blue-300',
  COMPLETED:        'bg-blue-100   text-blue-800   dark:bg-blue-900/40   dark:text-blue-300',
  CANCELLED:        'bg-slate-100  text-slate-600  dark:bg-slate-700     dark:text-slate-300',
  DELIVERED:        'bg-green-100  text-green-800  dark:bg-green-900/40  dark:text-green-300',
  FAILED:           'bg-red-100    text-red-800    dark:bg-red-900/40    dark:text-red-300',
}

const DOT_STYLES: Record<string, string> = {
  ACTIVE: 'text-green-500', APPROVED: 'text-green-500', DELIVERED: 'text-green-500', COMPLETED: 'text-blue-500',
  FULLY_REDEEMED: 'text-blue-500', PENDING: 'text-amber-500', PENDING_APPROVAL: 'text-amber-500',
}

interface StatusBadgeProps {
  status?: string | null
  label?: string
  size?: 'sm' | 'md'
}

export function StatusBadge({ status, label, size = 'sm' }: StatusBadgeProps) {
  const key = (status ?? '').toUpperCase()
  const classes = BADGE_STYLES[key] ?? 'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-300'
  const dotClass = DOT_STYLES[key] ?? 'text-slate-400'
  const sizeClass = size === 'md' ? 'px-2.5 py-1 text-sm' : 'px-2 py-0.5 text-xs'
  const display = label ?? (status ?? '—')

  return (
    <span className={`inline-flex items-center gap-1 rounded-full font-medium ${sizeClass} ${classes}`}>
      <span className={`text-[0.5rem] leading-none ${dotClass}`} aria-hidden="true">●</span>
      <span>{display}</span>
    </span>
  )
}
