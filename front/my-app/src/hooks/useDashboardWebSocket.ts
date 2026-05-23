import { useEffect, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { DashboardBootstrapDto, DashboardLiveDto } from '../types/api'
import { getStoredToken } from '../context/AuthContext'

const apiBase: string = import.meta.env.VITE_API_URL ?? ''
const WS_URL = apiBase.replace(/\/api\/v1\/?$/, '') + '/ws'
const TOPIC = '/topic/dashboard/live'

export function useDashboardWebSocket(
  bootstrapQueryKey: readonly [string, string, string, string],
) {
  const queryClient = useQueryClient()
  const clientRef = useRef<Client | null>(null)
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    const token = getStoredToken()
    const stompClient = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5_000,
      onConnect: () => {
        setConnected(true)
        stompClient.subscribe(TOPIC, (frame) => {
          try {
            const live = JSON.parse(frame.body) as DashboardLiveDto
            queryClient.setQueryData<DashboardBootstrapDto | undefined>(
              bootstrapQueryKey,
              (old) => {
                if (!old) return old
                return { ...old, kpis: live.kpis, activity: live.activity, serviceHealth: live.serviceHealth }
              },
            )
          } catch {
            // malformed frame — ignore
          }
        })
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
    })
    stompClient.activate()
    clientRef.current = stompClient

    return () => {
      stompClient.deactivate()
      setConnected(false)
    }
  }, [bootstrapQueryKey, queryClient])

  return connected
}
