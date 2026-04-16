/**
 * Messages dropdown panel – placeholder for notifications.
 * Ticket-based messaging removed; dedicated ticket dashboard will be built later.
 */

import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'

interface MessagesPanelProps {
  isOpen: boolean
  onClose: () => void
  anchorRef: React.RefObject<HTMLButtonElement | null>
  onOpenCount?: (count: number) => void
}

export function MessagesPanel({ isOpen, onClose, anchorRef }: MessagesPanelProps) {
  const panelRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!isOpen) return
    const handleClick = (e: MouseEvent) => {
      if (
        panelRef.current?.contains(e.target as Node) ||
        anchorRef.current?.contains(e.target as Node)
      ) return
      onClose()
    }
    document.addEventListener('click', handleClick)
    return () => document.removeEventListener('click', handleClick)
  }, [isOpen, onClose, anchorRef])

  if (!isOpen) return null

  return (
    <div
      ref={panelRef}
      className="absolute right-0 top-full z-50 mt-1 w-96 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl dark:border-slate-600 dark:bg-slate-800"
    >
      <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-slate-600">
        <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Messages</h3>
        <Link
          to="/support/complaints"
          onClick={onClose}
          className="text-xs font-medium text-primary hover:underline"
        >
          View complaints
        </Link>
      </div>
      <div className="px-4 py-8 text-center text-sm text-slate-500 dark:text-slate-400">
        No new messages. Use Support for complaints.
      </div>
    </div>
  )
}
