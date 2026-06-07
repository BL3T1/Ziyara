import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useAuth } from './AuthContext'
import { currencyAPI, settingsAPI } from '../services/api'
import type { SystemSettingsDto } from '../types/api'
import { convertAmountWithRates, type ExchangeRateLike } from '../utils/currencyConvert'

interface DisplayCurrencyContextValue {
  /** Platform default from Admin → Settings */
  defaultCurrency: string
  /** User-selected display currency (persisted in localStorage). Defaults to defaultCurrency. */
  selectedCurrency: string
  setSelectedCurrency: (code: string) => void
  /** All currencies available for selection (derived from rate table). */
  availableCurrencies: string[]
  /** Format amount using ISO currency (defaults to {@link defaultCurrency}). No FX conversion. */
  formatMoney: (amount: number, isoCurrencyCode?: string) => string
  /** Convert from {@code sourceCurrency} to {@link defaultCurrency} using rate table, then format. Falls back to literal format if no path. */
  displayInDefault: (amount: number, sourceCurrency?: string | null) => string
  /** Convert amount from sourceCurrency to selectedCurrency and format. */
  displayInSelected: (amount: number, sourceCurrency?: string | null) => string
  /** Refetch settings + rates (e.g. after saving Admin Settings). */
  refreshDisplayCurrency: () => void
}

const DisplayCurrencyContext = createContext<DisplayCurrencyContextValue | null>(null)

function formatMoneyIntl(amount: number, code: string): string {
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: code,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(Number.isFinite(amount) ? amount : 0)
  } catch {
    return `${code} ${amount}`
  }
}

const STORAGE_KEY = 'ziyara-currency'

export function DisplayCurrencyProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [defaultCurrency, setDefaultCurrency] = useState('USD')
  const [rateRows, setRateRows] = useState<ExchangeRateLike[]>([])
  const [refreshKey, setRefreshKey] = useState(0)
  const [selectedCurrency, setSelectedCurrencyState] = useState<string>(
    () => localStorage.getItem(STORAGE_KEY) ?? 'USD',
  )

  const setSelectedCurrency = useCallback((code: string) => {
    const c = code.trim().toUpperCase()
    if (c.length !== 3) return
    localStorage.setItem(STORAGE_KEY, c)
    setSelectedCurrencyState(c)
  }, [])

  const refreshDisplayCurrency = useCallback(() => {
    setRefreshKey((k) => k + 1)
  }, [])

  // Load rates from the public endpoint (no auth required)
  useEffect(() => {
    let cancelled = false
    currencyAPI.listRates()
      .then((r) => { if (!cancelled) setRateRows(Array.isArray(r.data) ? (r.data as ExchangeRateLike[]) : []) })
      .catch(() => {})
    return () => { cancelled = true }
  }, [refreshKey])

  // Load admin default currency only when authenticated
  useEffect(() => {
    if (!user) return
    let cancelled = false
    settingsAPI.get()
      .then((r) => {
        if (cancelled) return
        const d = r.data as SystemSettingsDto
        const c = (d?.defaultCurrency ?? 'USD').toUpperCase()
        if (c.length === 3) setDefaultCurrency(c)
      })
      .catch(() => {})
    return () => { cancelled = true }
  }, [user, refreshKey])

  const formatMoney = useCallback(
    (amount: number, isoCurrencyCode?: string) => {
      const code = (
        isoCurrencyCode && isoCurrencyCode.trim().length === 3 ? isoCurrencyCode.trim() : defaultCurrency
      ).toUpperCase()
      return formatMoneyIntl(amount, code)
    },
    [defaultCurrency],
  )

  const availableCurrencies = useMemo(() => {
    const codes = new Set<string>(['USD', defaultCurrency])
    rateRows.forEach((r) => {
      if (r.toCurrency?.length === 3) codes.add(r.toCurrency.toUpperCase())
    })
    return Array.from(codes).sort()
  }, [rateRows, defaultCurrency])

  const displayInDefault = useCallback(
    (amount: number, sourceCurrency?: string | null) => {
      const { value, converted } = convertAmountWithRates(
        amount,
        sourceCurrency,
        defaultCurrency,
        rateRows,
        'USD',
      )
      if (!converted) {
        const src =
          sourceCurrency && sourceCurrency.trim().length === 3 ? sourceCurrency.trim().toUpperCase() : defaultCurrency
        return formatMoneyIntl(amount, src)
      }
      return formatMoneyIntl(value, defaultCurrency)
    },
    [defaultCurrency, rateRows],
  )

  const displayInSelected = useCallback(
    (amount: number, sourceCurrency?: string | null) => {
      const target = selectedCurrency || defaultCurrency
      const { value, converted } = convertAmountWithRates(amount, sourceCurrency, target, rateRows, 'USD')
      if (!converted) {
        const src = sourceCurrency && sourceCurrency.trim().length === 3 ? sourceCurrency.trim().toUpperCase() : target
        return formatMoneyIntl(amount, src)
      }
      return formatMoneyIntl(value, target)
    },
    [selectedCurrency, defaultCurrency, rateRows],
  )

  const value: DisplayCurrencyContextValue = {
    defaultCurrency,
    selectedCurrency: selectedCurrency || defaultCurrency,
    setSelectedCurrency,
    availableCurrencies,
    formatMoney,
    displayInDefault,
    displayInSelected,
    refreshDisplayCurrency,
  }

  return <DisplayCurrencyContext.Provider value={value}>{children}</DisplayCurrencyContext.Provider>
}

export function useDisplayCurrency(): DisplayCurrencyContextValue {
  const ctx = useContext(DisplayCurrencyContext)
  if (!ctx) {
    const fallbackFmt = (amount: number, iso?: string | null) => {
      const code = (iso && iso.trim().length === 3 ? iso.trim() : 'USD').toUpperCase()
      return formatMoneyIntl(amount, code)
    }
    return {
      defaultCurrency: 'USD',
      selectedCurrency: 'USD',
      setSelectedCurrency: () => {},
      availableCurrencies: ['USD'],
      formatMoney: fallbackFmt,
      displayInDefault: fallbackFmt,
      displayInSelected: fallbackFmt,
      refreshDisplayCurrency: () => {},
    }
  }
  return ctx
}
