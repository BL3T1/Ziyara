import { useEffect, useMemo, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { landingPublicApi } from './landingPublicApi'

type PageContent = Record<string, unknown>

const cache = new Map<string, PageContent>()

export function useLandingPageContent(slug: string) {
  const { locale } = useLanguage()
  const key = `${slug}:${locale}`
  const [content, setContent] = useState<PageContent | null>(cache.get(key) ?? null)
  const [loading, setLoading] = useState(!cache.has(key))

  useEffect(() => {
    let mounted = true
    const existing = cache.get(key)
    if (existing) {
      Promise.resolve().then(() => { setContent(existing); setLoading(false) })
      return
    }
    Promise.resolve().then(() => setLoading(true))
    landingPublicApi
      .getPageContent(slug, locale)
      .then((data) => {
        if (!mounted) return
        const obj = data ? (data as PageContent) : null
        if (obj) cache.set(key, obj)
        setContent(obj)
      })
      .catch(() => {
        if (mounted) setContent(null)
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })
    return () => {
      mounted = false
    }
  }, [key, locale, slug])

  const readString = useMemo(
    () => (name: string, fallback: string) => {
      const value = content?.[name]
      return typeof value === 'string' && value.trim().length > 0 ? value : fallback
    },
    [content],
  )

  return { content, loading, readString }
}
