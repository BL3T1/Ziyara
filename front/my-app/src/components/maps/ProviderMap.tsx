/**
 * Shared Leaflet map component. Renders provider/listing pins and optionally
 * polls a live delivery pin.
 *
 * Leaflet CSS must be loaded once — imported here so it's bundled automatically.
 */

import { useEffect, useRef } from 'react'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'

// Fix default marker icon paths broken by bundlers
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

delete (L.Icon.Default.prototype as unknown as Record<string, unknown>)._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

export interface ProviderMapPin {
  id: string
  name: string
  type: string
  latitude: number
  longitude: number
  status?: string
  thumbnailUrl?: string
}

interface Props {
  pins: ProviderMapPin[]
  center?: [number, number]
  zoom?: number
  className?: string
  /** When set, a live-tracking marker will poll for this booking's delivery location */
  liveDeliveryBookingId?: string
}

const TYPE_COLORS: Record<string, string> = {
  HOTEL: '#1e4d6b',
  RESORT: '#1a6b4d',
  RESTAURANT: '#b45309',
  TAXI: '#7c3aed',
  TRIP: '#0369a1',
}

function pinColor(type: string) {
  return TYPE_COLORS[type.toUpperCase()] ?? '#475569'
}

function createPinIcon(type: string) {
  const color = pinColor(type)
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="36" viewBox="0 0 28 36">
    <path d="M14 0C6.268 0 0 6.268 0 14c0 9.333 14 22 14 22S28 23.333 28 14C28 6.268 21.732 0 14 0z" fill="${color}" opacity="0.9"/>
    <circle cx="14" cy="14" r="6" fill="white" opacity="0.95"/>
  </svg>`
  return L.divIcon({
    html: svg,
    className: '',
    iconSize: [28, 36],
    iconAnchor: [14, 36],
    popupAnchor: [0, -36],
  })
}

function createLiveIcon() {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32">
    <circle cx="16" cy="16" r="14" fill="#059669" opacity="0.2"/>
    <circle cx="16" cy="16" r="9" fill="#059669" opacity="0.9"/>
    <circle cx="16" cy="16" r="4" fill="white"/>
  </svg>`
  return L.divIcon({
    html: svg,
    className: '',
    iconSize: [32, 32],
    iconAnchor: [16, 16],
    popupAnchor: [0, -18],
  })
}

export function ProviderMap({
  pins,
  center = [24.7136, 46.6753],
  zoom = 6,
  className = 'h-[520px] w-full rounded-xl overflow-hidden',
  liveDeliveryBookingId,
}: Props) {
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<L.Map | null>(null)
  const liveMarkerRef = useRef<L.Marker | null>(null)
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // Initialise map once
  useEffect(() => {
    if (!containerRef.current || mapRef.current) return
    const map = L.map(containerRef.current).setView(center, zoom)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(map)
    mapRef.current = map
    return () => {
      map.remove()
      mapRef.current = null
    }
    // Only on mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Update pins whenever they change
  useEffect(() => {
    const map = mapRef.current
    if (!map) return
    // Clear previous provider markers (stored in a layer group)
    map.eachLayer((layer) => {
      if ((layer as L.Marker & { _isProviderPin?: boolean })._isProviderPin) {
        map.removeLayer(layer)
      }
    })
    pins.forEach((pin) => {
      const marker = L.marker([pin.latitude, pin.longitude], {
        icon: createPinIcon(pin.type),
      })
        .bindPopup(
          `<div style="min-width:160px">
            <strong style="font-size:13px">${pin.name}</strong>
            <div style="margin-top:4px;font-size:11px;color:#64748b">${pin.type}${pin.status ? ` · ${pin.status}` : ''}</div>
          </div>`,
        )
        .addTo(map)
      ;(marker as L.Marker & { _isProviderPin?: boolean })._isProviderPin = true
    })

    if (pins.length > 0) {
      const group = L.featureGroup(
        pins.map((p) => L.marker([p.latitude, p.longitude]))
      )
      map.fitBounds(group.getBounds().pad(0.15), { maxZoom: 13 })
    }
  }, [pins])

  // Live delivery polling
  useEffect(() => {
    const map = mapRef.current
    if (!map || !liveDeliveryBookingId) {
      if (pollRef.current) clearInterval(pollRef.current)
      if (liveMarkerRef.current) {
        liveMarkerRef.current.remove()
        liveMarkerRef.current = null
      }
      return
    }

    const fetchLive = async () => {
      try {
        const res = await fetch(`/api/v1/map/delivery/${liveDeliveryBookingId}`, {
          credentials: 'include',
        })
        if (!res.ok) return
        const data = (await res.json()) as { latitude: number; longitude: number; status?: string }
        const latlng: L.LatLngExpression = [data.latitude, data.longitude]
        if (liveMarkerRef.current) {
          liveMarkerRef.current.setLatLng(latlng)
        } else {
          liveMarkerRef.current = L.marker(latlng, { icon: createLiveIcon() })
            .bindPopup(`<strong>Live delivery</strong>${data.status ? `<br/>${data.status}` : ''}`)
            .addTo(map)
        }
      } catch {
        // silently ignore network errors between polls
      }
    }

    fetchLive()
    pollRef.current = setInterval(fetchLive, 10_000)
    return () => {
      if (pollRef.current) clearInterval(pollRef.current)
    }
  }, [liveDeliveryBookingId])

  return <div ref={containerRef} className={className} />
}
