import axios from 'axios'
import type { PageDto, ServiceDto, ServiceImageDto } from '../../types/api'

const BASE_URL = import.meta.env.VITE_API_URL ?? '/api/v1'

const publicClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

function asPage(data: unknown): PageDto<ServiceDto> | null {
  if (!data || typeof data !== 'object') return null
  const body = data as Record<string, unknown>
  const payload = (body.data ?? body) as Record<string, unknown>
  if (!Array.isArray(payload.content)) return null
  return payload as unknown as PageDto<ServiceDto>
}

export const landingPublicApi = {
  async listServices(params?: { page?: number; size?: number; type?: string }) {
    const res = await publicClient.get('/services', { params: { page: 0, size: 120, ...params } })
    return asPage(res.data)
  },
  async listServiceImages(serviceId: string) {
    const res = await publicClient.get(`/services/${serviceId}/images`)
    const body = res.data as unknown
    if (Array.isArray(body)) return body as ServiceImageDto[]
    if (body && typeof body === 'object') {
      const payload = (body as Record<string, unknown>).data
      return Array.isArray(payload) ? (payload as ServiceImageDto[]) : []
    }
    return []
  },
  async getPageContent(slug: string, lang: 'en' | 'ar') {
    const res = await publicClient.get(`/content-pages/${slug}`, { params: { lang } })
    const body = res.data as unknown
    if (body && typeof body === 'object') {
      // body = ApiEnvelope { data: ContentPageResponse { content: {...}, published, slug } }
      const dto = (body as Record<string, unknown>).data as Record<string, unknown> | undefined
      if (dto && typeof dto === 'object') {
        const contentMap = (dto as Record<string, unknown>).content
        if (contentMap && typeof contentMap === 'object') return contentMap as Record<string, unknown>
      }
    }
    return null
  },
}
