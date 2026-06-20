import { useDisplayCurrency } from '../context/DisplayCurrencyContext'

export function CurrencySwitcher() {
  const { selectedCurrency, setSelectedCurrency, availableCurrencies } = useDisplayCurrency()

  if (availableCurrencies.length <= 1) return null

  return (
    <select
      value={selectedCurrency}
      onChange={(e) => setSelectedCurrency(e.target.value)}
      aria-label="Display currency"
      className="rounded-lg border border-slate-300 bg-white px-2 py-1 text-xs font-medium text-slate-700 shadow-sm focus:outline-none dark:border-slate-600 dark:bg-slate-800 dark:text-slate-300"
    >
      {availableCurrencies.map((c) => (
        <option key={c} value={c}>{c}</option>
      ))}
    </select>
  )
}
