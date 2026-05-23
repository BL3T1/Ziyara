interface BulkActionBarProps {
  selectedCount: number
  actions: Array<{
    label: string
    onClick: () => void
    variant?: 'danger' | 'default'
    disabled?: boolean
  }>
  onClearSelection: () => void
}

export function BulkActionBar({ selectedCount, actions, onClearSelection }: BulkActionBarProps) {
  if (selectedCount === 0) return null
  return (
    <div className="sticky top-[4.25rem] z-30 flex flex-wrap items-center gap-3 rounded-xl border border-primary/20 bg-primary/5 px-4 py-2.5 shadow-sm dark:border-primary/30 dark:bg-primary/10">
      <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">
        {selectedCount} selected
      </span>
      <div className="flex flex-wrap gap-2">
        {actions.map((action) => (
          <button
            key={action.label}
            type="button"
            disabled={action.disabled}
            onClick={action.onClick}
            className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors disabled:opacity-50 ${
              action.variant === 'danger'
                ? 'bg-red-100 text-red-700 hover:bg-red-200 dark:bg-red-900/30 dark:text-red-300 dark:hover:bg-red-900/50'
                : 'bg-primary/10 text-primary hover:bg-primary/20 dark:hover:bg-primary/30'
            }`}
          >
            {action.label}
          </button>
        ))}
      </div>
      <button
        type="button"
        onClick={onClearSelection}
        className="ms-auto text-xs text-slate-500 underline hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
      >
        Clear
      </button>
    </div>
  )
}
