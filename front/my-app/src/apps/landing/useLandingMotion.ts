import { useEffect } from 'react'

/**
 * Updates CSS custom properties for subtle parallax and pointer depth effects.
 */
export function useLandingMotion() {
  useEffect(() => {
    if (typeof window === 'undefined') return

    const root = document.documentElement
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (prefersReduced) return

    // On coarse-pointer (touch) devices, pointer-based parallax provides no value and wastes frames.
    const isTouch = window.matchMedia('(pointer: coarse)').matches
    if (isTouch) {
      // Only track scroll — no rAF loop, no mousemove listener.
      const onScroll = () => {
        const v = (Math.min(window.scrollY, 1200) / 1200).toFixed(4)
        root.style.setProperty('--landing-scroll', v)
      }
      window.addEventListener('scroll', onScroll, { passive: true })
      onScroll()
      return () => {
        window.removeEventListener('scroll', onScroll)
        root.style.removeProperty('--landing-scroll')
      }
    }

    let rafId = 0
    let targetX = 0
    let targetY = 0
    let targetScroll = 0
    let currentX = 0
    let currentY = 0
    let currentScroll = 0

    const tick = () => {
      currentX += (targetX - currentX) * 0.12
      currentY += (targetY - currentY) * 0.12
      currentScroll += (targetScroll - currentScroll) * 0.1

      root.style.setProperty('--landing-mx', currentX.toFixed(4))
      root.style.setProperty('--landing-my', currentY.toFixed(4))
      root.style.setProperty('--landing-scroll', currentScroll.toFixed(4))

      rafId = window.requestAnimationFrame(tick)
    }

    const onMouseMove = (event: MouseEvent) => {
      const x = event.clientX / window.innerWidth - 0.5
      const y = event.clientY / window.innerHeight - 0.5
      targetX = Math.max(-0.5, Math.min(0.5, x))
      targetY = Math.max(-0.5, Math.min(0.5, y))
    }

    const onScroll = () => {
      targetScroll = Math.min(window.scrollY, 1200) / 1200
    }

    rafId = window.requestAnimationFrame(tick)
    window.addEventListener('mousemove', onMouseMove)
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()

    return () => {
      window.cancelAnimationFrame(rafId)
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('scroll', onScroll)
      root.style.removeProperty('--landing-mx')
      root.style.removeProperty('--landing-my')
      root.style.removeProperty('--landing-scroll')
    }
  }, [])
}
