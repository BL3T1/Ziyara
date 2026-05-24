import { useEffect } from 'react'

/**
 * Activates scroll-triggered entrance animations.
 * Adds `.lp-in` to any `.lp-animate` element once it enters the viewport.
 */
export function useLandingReveal() {
  useEffect(() => {
    if (typeof window === 'undefined') return
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (prefersReduced) return

    const observe = () => {
      const els = Array.from(document.querySelectorAll<HTMLElement>('.lp-animate:not(.lp-in)'))
      if (!els.length) return null

      const observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              entry.target.classList.add('lp-in')
              observer.unobserve(entry.target)
            }
          })
        },
        { threshold: 0.08, rootMargin: '0px 0px -48px 0px' },
      )

      els.forEach((el) => observer.observe(el))
      return observer
    }

    const observer = observe()
    return () => observer?.disconnect()
  }, [])
}
