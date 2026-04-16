/**
 * Client-side conversion using stored exchange-rate rows (direct, inverse, multi-hop BFS).
 */

export interface ExchangeRateLike {
  fromCurrency?: string
  toCurrency?: string
  rate?: unknown
}

function normalizeCode(code: string | null | undefined, fallback: string): string {
  const c = (code ?? fallback).trim().toUpperCase()
  return c.length === 3 && /^[A-Z]{3}$/.test(c) ? c : fallback
}

function parseRate(r: unknown): number | null {
  if (r == null) return null
  const n = typeof r === 'number' ? r : Number(r)
  return Number.isFinite(n) && n > 0 ? n : null
}

/** Adjacency: currency -> neighbor -> multiplier applied to amount when moving along the edge. */
function buildMultiplierGraph(rows: ExchangeRateLike[]): Map<string, Map<string, number>> {
  const g = new Map<string, Map<string, number>>()
  const add = (from: string, to: string, mult: number) => {
    if (!g.has(from)) g.set(from, new Map())
    g.get(from)!.set(to, mult)
  }
  for (const row of rows) {
    const from = row.fromCurrency?.trim().toUpperCase()
    const to = row.toCurrency?.trim().toUpperCase()
    const rate = parseRate(row.rate)
    if (!from || !to || from.length !== 3 || to.length !== 3 || rate == null) continue
    add(from, to, rate)
    add(to, from, 1 / rate)
  }
  return g
}

export interface ConvertResult {
  /** Amount in target currency, or original amount if no path exists */
  value: number
  /** True when a conversion path was found (including same-currency) */
  converted: boolean
}

/**
 * Converts {@code amount} from {@code fromCurrency} to {@code toCurrency} using {@code rows}.
 * If no path exists, returns {@code amount} and {@code converted: false} (mirrors backend convertOrKeep).
 */
export function convertAmountWithRates(
  amount: number,
  fromCurrency: string | null | undefined,
  toCurrency: string | null | undefined,
  rows: ExchangeRateLike[],
  fallbackSource = 'USD',
): ConvertResult {
  const from = normalizeCode(fromCurrency, fallbackSource)
  const to = normalizeCode(toCurrency, fallbackSource)
  if (!Number.isFinite(amount)) return { value: 0, converted: true }
  if (from === to) return { value: amount, converted: true }

  const g = buildMultiplierGraph(rows)
  const queue: Array<{ cur: string; acc: number }> = [{ cur: from, acc: amount }]
  const visited = new Set<string>([from])

  while (queue.length > 0) {
    const { cur, acc } = queue.shift()!
    if (cur === to) return { value: acc, converted: true }
    const neighbors = g.get(cur)
    if (!neighbors) continue
    for (const [next, mult] of neighbors) {
      if (visited.has(next)) continue
      visited.add(next)
      queue.push({ cur: next, acc: acc * mult })
    }
  }

  return { value: amount, converted: false }
}
