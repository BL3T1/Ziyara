import { useEffect } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

export function useLandingGSAP(locationKey: string) {
  useEffect(() => {
    if (typeof window === 'undefined') return

    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

    if (prefersReduced) {
      document.querySelectorAll<HTMLElement>('.lp-animate').forEach((el) => {
        el.style.opacity = '1'
        el.style.transform = 'none'
      })
      return
    }

    const ctx = gsap.context(() => {
      // ── Hero entrance timeline (home page only) ───────────────────────────
      const hero = document.querySelector<HTMLElement>('.lp-ziyara-hero')
      if (hero) {
        gsap.set('.lp-ziyara-hero__visual', { x: 36 })

        const tl = gsap.timeline({ delay: 0.08 })

        tl.to('.lp-ziyara-hero .lp-eyebrow', {
          opacity: 1,
          y: 0,
          duration: 0.55,
          ease: 'power3.out',
        })
          .to(
            '#hero-heading',
            { opacity: 1, y: 0, duration: 0.72, ease: 'power3.out' },
            '-=0.28',
          )
          .to(
            '.lp-ziyara-hero .lp-hero-lede',
            { opacity: 1, y: 0, duration: 0.65, ease: 'power3.out' },
            '-=0.36',
          )
          .to(
            '.lp-ziyara-hero .lp-cta .lp-btn',
            {
              opacity: 1,
              y: 0,
              scale: 1,
              duration: 0.52,
              stagger: 0.12,
              ease: 'back.out(1.6)',
            },
            '-=0.38',
          )
          .to(
            '.lp-ziyara-hero__visual',
            { opacity: 1, x: 0, duration: 0.82, ease: 'power3.out' },
            '-=0.52',
          )
      }

      // ── Scroll-reveal batch for all .lp-animate sections ─────────────────
      ScrollTrigger.batch('.lp-animate', {
        onEnter: (elements) => {
          gsap.to(elements, {
            opacity: 1,
            y: 0,
            scale: 1,
            duration: 0.68,
            stagger: 0.08,
            ease: 'power3.out',
            overwrite: true,
          })
        },
        once: true,
        start: 'top 90%',
      })

      // ── Deal card stagger ─────────────────────────────────────────────────
      const dealGrid = document.querySelector('.lp-deal-grid')
      if (dealGrid) {
        ScrollTrigger.create({
          trigger: dealGrid,
          start: 'top 88%',
          once: true,
          onEnter: () => {
            gsap.fromTo(
              '.lp-deal-grid .lp-solution-card',
              { opacity: 0, y: 28, scale: 0.96 },
              {
                opacity: 1,
                y: 0,
                scale: 1,
                duration: 0.58,
                stagger: 0.09,
                ease: 'power3.out',
                overwrite: true,
              },
            )
          },
        })
      }

      // ── City chip pop-in ──────────────────────────────────────────────────
      const sheetSection = document.querySelector('.lp-sheet')
      if (sheetSection) {
        ScrollTrigger.create({
          trigger: sheetSection,
          start: 'top 86%',
          once: true,
          onEnter: () => {
            gsap.fromTo(
              '.lp-city-chip',
              { opacity: 0, scale: 0.82, y: 10 },
              {
                opacity: 1,
                scale: 1,
                y: 0,
                duration: 0.42,
                stagger: 0.07,
                ease: 'back.out(1.5)',
                overwrite: true,
              },
            )
          },
        })
      }

      // ── Trust pillars stagger ─────────────────────────────────────────────
      const pillars = document.querySelector('.lp-pillars')
      if (pillars) {
        ScrollTrigger.create({
          trigger: pillars,
          start: 'top 85%',
          once: true,
          onEnter: () => {
            gsap.fromTo(
              '.lp-pillar',
              { opacity: 0, y: 32 },
              {
                opacity: 1,
                y: 0,
                duration: 0.65,
                stagger: 0.13,
                ease: 'power3.out',
                overwrite: true,
              },
            )
          },
        })
      }

      // ── Stats strip counter animation ─────────────────────────────────────
      const statsStrip = document.querySelector('.lp-stats-strip')
      if (statsStrip) {
        ScrollTrigger.create({
          trigger: statsStrip,
          start: 'top 92%',
          once: true,
          onEnter: () => {
            gsap.fromTo(
              '.lp-stat-item',
              { opacity: 0, y: 18, scale: 0.92 },
              {
                opacity: 1,
                y: 0,
                scale: 1,
                duration: 0.48,
                stagger: 0.1,
                ease: 'back.out(1.4)',
                overwrite: true,
              },
            )
          },
        })
      }

      // ── Partner band ──────────────────────────────────────────────────────
      const partnerBand = document.querySelector('.lp-partner-band')
      if (partnerBand) {
        ScrollTrigger.create({
          trigger: partnerBand,
          start: 'top 88%',
          once: true,
          onEnter: () => {
            gsap.fromTo(
              partnerBand,
              { opacity: 0, y: 24, scale: 0.98 },
              { opacity: 1, y: 0, scale: 1, duration: 0.7, ease: 'power3.out' },
            )
          },
        })
      }
    })

    return () => {
      ctx.revert()
      ScrollTrigger.getAll().forEach((t) => t.kill())
    }
  }, [locationKey])
}
