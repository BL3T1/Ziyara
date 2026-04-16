import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { useAuth } from './AuthContext'
import { currencyAPI, settingsAPI } from '../services/api'
import type { SystemSettingsDto } from '../types/api'
import { convertAmountWithRates, type ExchangeRateLike } from '../utils/currencyConvert'

interface DisplayCurrencyContextValue {
  /** Platform default from Admin → Settings */
  defaultCurrency: string
  /** Format amount using ISO currency (defaults to {@link defaultCurrency}). No FX conversion. */
  formatMoney: (amount: number, isoCurrencyCode?: string) => string
  /** Convert from {@code sourceCurrency} to {@link defaultCurrency} using rate table, then format. Falls back to literal format if no path. */
  displayInDefault: (amount: number, sourceCurrency?: string | null) => string
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

export function DisplayCurrencyProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [defaultCurrency, setDefaultCurrency] = useState('USD')
  const [rateRows, setRateRows] = useState<ExchangeRateLike[]>([])
  const [refreshKey, setRefreshKey] = useState(0)

  const refreshDisplayCurrency = useCallback(() => {
    setRefreshKey((k) => k + 1)
  }, [])

  useEffect(() => {
    if (!user) return
    let cancelled = false

    const load = async () => {
      try {
        const [settingsRes, ratesRes] = await Promise.all([settingsAPI.get(), currencyAPI.listRates()])
        if (cancelled) return
        const d = settingsRes.data as SystemSettingsDto
        const c = (d?.defaultCurrency ?? 'USD').toUpperCase()
        if (c.length === 3) setDefaultCurrency(c)
        const raw = ratesRes.data
        setRateRows(Array.isArray(raw) ? (raw as ExchangeRateLike[]) : [])
      } catch {
        if (cancelled) return
        try {
          const settingsRes = await settingsAPI.get()
          if (cancelled) return
          const d = settingsRes.data as SystemSettingsDto
          const c = (d?.defaultCurrency ?? 'USD').toUpperCase()
          if (c.length === 3) setDefaultCurrency(c)
        } catch {
          /* keep prior default */
        }
        setRateRows([])
      }
    }

    void load()
    return () => {
      cancelled = true
    }
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

  const value: DisplayCurrencyContextValue = {
    defaultCurrency,
    formatMoney,
    displayInDefault,
    refreshDisplayCurrency,
  }

  return <DisplayCurrencyContext.Provider value={value}>{children}</DisplayCurrencyContext.Provider>
}

export function useDisplayCurrency(): DisplayCurrencyContextValue {
  const ctx = useContext(DisplayCurrencyContext)
  if (!ctx) {
    return {
      defaultCurrency: 'USD',
      formatMoney: (amount: number, iso?: string) => {
        const code = (iso && iso.length === 3 ? iso : 'USD').toUpperCase()
        return formatMoneyIntl(amount, code)
      },
      displayInDefault: (amount: number, iso?: string | null) => {
        const code = (iso && iso.trim().length === 3 ? iso.trim() : 'USD').toUpperCase()
        return formatMoneyIntl(amount, code)
      },
      refreshDisplayCurrency: () => {},
    }
  }
  return ctx
}
