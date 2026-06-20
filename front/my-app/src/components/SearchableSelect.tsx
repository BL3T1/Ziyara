import { useCallback, useEffect, useId, useRef, useState } from 'react'
import { useLanguage } from '../context/LanguageContext'

export interface SelectOption {
  value: string
  label: string
  sublabel?: string
}

interface SearchableSelectProps {
  selectedOption: SelectOption | null
  onSelect: (opt: SelectOption | null) => void
  fetchOptions: (query: string) => Promise<SelectOption[]>
  placeholder?: string
  disabled?: boolean
  className?: string
  clearable?: boolean
  id?: string
}

export function SearchableSelect({
  selectedOption,
  onSelect,
  fetchOptions,
  placeholder,
  disabled = false,
  className = '',
  clearable = false,
  id,
}: SearchableSelectProps) {
  const { t, locale } = useLanguage()
  const isRtl = locale === 'ar'
  const inputId = useId()
  const listId = useId()
  const containerRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [options, setOptions] = useState<SelectOption[]>([])
  const [loading, setLoading] = useState(false)
  const [activeIndex, setActiveIndex] = useState(-1)

  const ph = placeholder ?? t('searchableSelect.placeholder')

  const runFetch = useCallback(
    (q: string) => {
      setLoading(true)
      fetchOptions(q)
        .then((res) => {
          setOptions(res)
          setActiveIndex(-1)
        })
        .catch(() => setOptions([]))
        .finally(() => setLoading(false))
    },
    [fetchOptions],
  )

  const openDropdown = () => {
    if (disabled) return
    setOpen(true)
    setQuery('')
    runFetch('')
    requestAnimationFrame(() => inputRef.current?.focus())
  }

  const closeDropdown = () => {
    setOpen(false)
    setQuery('')
    setOptions([])
    setActiveIndex(-1)
  }

  const handleQueryChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const q = e.target.value
    setQuery(q)
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => runFetch(q), 280)
  }

  const handleSelect = (opt: SelectOption) => {
    onSelect(opt)
    closeDropdown()
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!open) {
      if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') {
        e.preventDefault()
        openDropdown()
      }
      return
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActiveIndex((i) => Math.min(i + 1, options.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActiveIndex((i) => Math.max(i - 1, -1))
    } else if (e.key === 'Enter' && activeIndex >= 0 && options[activeIndex]) {
      e.preventDefault()
      handleSelect(options[activeIndex])
    } else if (e.key === 'Escape') {
      e.preventDefault()
      closeDropdown()
    }
  }

  // Close on outside click
  useEffect(() => {
    if (!open) return
    const handler = (e: MouseEvent) => {
      if (!containerRef.current?.contains(e.target as Node)) closeDropdown()
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [open])

  // Cleanup debounce on unmount
  useEffect(() => {
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [])

  const displayText = selectedOption?.label ?? ''

  return (
    <div ref={containerRef} className={`relative ${className}`} dir={isRtl ? 'rtl' : undefined}>
      {/* Trigger button */}
      {!open && (
        <button
          type="button"
          id={id ?? inputId}
          role="combobox"
          aria-expanded={false}
          aria-haspopup="listbox"
          aria-controls={listId}
          disabled={disabled}
          onClick={openDropdown}
          onKeyDown={handleKeyDown}
          className="modal-input flex w-full items-center justify-between gap-2 text-left cursor-pointer disabled:cursor-not-allowed disabled:opacity-60"
        >
          <span className={displayText ? 'text-slate-700 dark:text-slate-100' : 'text-slate-400 dark:text-slate-500'}>
            {displayText || ph}
          </span>
          <span className="flex shrink-0 items-center gap-1">
            {clearable && selectedOption && (
              <span
                role="button"
                aria-label={t('searchableSelect.clearSelection')}
                tabIndex={0}
                onClick={(e) => { e.stopPropagation(); onSelect(null) }}
                onKeyDown={(e) => { if (e.key === 'Enter') { e.stopPropagation(); onSelect(null) } }}
                className="rounded p-0.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              >
                <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden>
                  <path d="M9 3L3 9M3 3l6 6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                </svg>
              </span>
            )}
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden>
              <path d="M4 6l4 4 4-4" stroke="#94a3b8" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </span>
        </button>
      )}

      {/* Open state: input + dropdown */}
      {open && (
        <>
          <input
            ref={inputRef}
            id={id ?? inputId}
            role="combobox"
            aria-expanded
            aria-haspopup="listbox"
            aria-controls={listId}
            aria-activedescendant={activeIndex >= 0 ? `${listId}-opt-${activeIndex}` : undefined}
            aria-autocomplete="list"
            autoComplete="off"
            value={query}
            onChange={handleQueryChange}
            onKeyDown={handleKeyDown}
            placeholder={ph}
            className="modal-input w-full"
          />
          <ul
            id={listId}
            role="listbox"
            className="absolute z-50 mt-1 max-h-56 w-full overflow-auto rounded-xl border border-slate-200 bg-white py-1 shadow-lg dark:border-white/[0.1] dark:bg-[#1a2236]"
          >
            {loading && (
              <li className="px-3 py-2 text-sm text-slate-500 dark:text-slate-400">
                {t('searchableSelect.loading')}
              </li>
            )}
            {!loading && options.length === 0 && (
              <li className="px-3 py-2 text-sm text-slate-500 dark:text-slate-400">
                {t('searchableSelect.noResults')}
              </li>
            )}
            {!loading &&
              options.map((opt, i) => (
                <li
                  key={opt.value}
                  id={`${listId}-opt-${i}`}
                  role="option"
                  aria-selected={selectedOption?.value === opt.value}
                  onMouseDown={(e) => { e.preventDefault(); handleSelect(opt) }}
                  onMouseEnter={() => setActiveIndex(i)}
                  className={`flex cursor-pointer flex-col px-3 py-2 text-sm ${
                    i === activeIndex
                      ? 'bg-primary/10 text-primary dark:bg-primary/20 dark:text-[#90caff]'
                      : selectedOption?.value === opt.value
                        ? 'bg-primary/5 text-primary dark:text-[#90caff]'
                        : 'text-slate-700 hover:bg-slate-50 dark:text-slate-200 dark:hover:bg-white/[0.05]'
                  }`}
                >
                  <span className="font-medium">{opt.label}</span>
                  {opt.sublabel && (
                    <span className="text-xs text-slate-500 dark:text-slate-400">{opt.sublabel}</span>
                  )}
                </li>
              ))}
          </ul>
        </>
      )}
    </div>
  )
}
