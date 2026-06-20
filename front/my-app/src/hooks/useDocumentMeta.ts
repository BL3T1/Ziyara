import { useEffect } from 'react'

interface DocumentMeta {
  title: string
  description?: string
  ogImage?: string
}

function upsertMeta(name: string, content: string, property = false) {
  const attr = property ? 'property' : 'name'
  let el = document.head.querySelector<HTMLMetaElement>(`meta[${attr}="${name}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute(attr, name)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

export function useDocumentMeta({ title, description, ogImage }: DocumentMeta) {
  useEffect(() => {
    const prevTitle = document.title
    document.title = title
    if (description) {
      upsertMeta('description', description)
      upsertMeta('og:description', description, true)
    }
    upsertMeta('og:title', title, true)
    if (ogImage) upsertMeta('og:image', ogImage, true)

    return () => {
      document.title = prevTitle
    }
  }, [title, description, ogImage])
}
