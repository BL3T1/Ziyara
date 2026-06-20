import { useRef, useState } from 'react'
import type { MouseEvent } from 'react'
import { Link } from 'react-router-dom'
import { LandingHeroArt } from './LandingHeroArt'

const isTouch = typeof window !== 'undefined' && window.matchMedia('(pointer: coarse)').matches

const HERO_SRC = '/ziyara-hero-reference.png'

const HOTSPOTS = [
  { cls: 'lp-ziyara-hero__hotspot--tl', to: '/hotels',      ariaLabel: 'Hotels',             label: 'Hotels' },
  { cls: 'lp-ziyara-hero__hotspot--tr', to: '/restaurants',  ariaLabel: 'Dining',             label: 'Dining' },
  { cls: 'lp-ziyara-hero__hotspot--bl', to: '/taxis',        ariaLabel: 'Transport',          label: 'Taxis' },
  { cls: 'lp-ziyara-hero__hotspot--br', to: '/trips',        ariaLabel: 'Tours & experiences', label: 'Trips' },
]

export function ZiyaraHeroComposition() {
  const wrapRef = useRef<HTMLDivElement>(null)
  const [tilt, setTilt] = useState({ x: 0, y: 0 })
  const [imgLoaded, setImgLoaded] = useState(false)
  const [imgError, setImgError] = useState(false)

  const onMove = (e: MouseEvent<HTMLDivElement>) => {
    if (isTouch) return
    const el = wrapRef.current
    if (!el) return
    const r = el.getBoundingClientRect()
    const nx = (e.clientX - r.left) / r.width - 0.5
    const ny = (e.clientY - r.top) / r.height - 0.5
    setTilt({ x: ny * -1.8, y: nx * 2.6 })
  }

  const onLeave = () => { if (!isTouch) setTilt({ x: 0, y: 0 }) }

  return (
    <div className="lp-ziyara-hero__stage">
      <div
        ref={wrapRef}
        className="lp-ziyara-hero__parallax lp-ziyara-hero__art-root"
        onMouseMove={onMove}
        onMouseLeave={onLeave}
        style={{
          transform: isTouch ? undefined : `perspective(820px) rotateX(${tilt.x}deg) rotateY(${tilt.y}deg)`,
        }}
      >
        <div className="lp-ziyara-hero__art-frame" style={{ position: 'relative' }}>
          {/* Loading skeleton — visible until image resolves */}
          {!imgLoaded && !imgError && (
            <div
              className="absolute inset-0 rounded-[22px] animate-pulse"
              aria-hidden
              style={{
                background: 'linear-gradient(135deg, rgba(200,185,165,0.25) 0%, rgba(61,112,128,0.12) 100%)',
              }}
            />
          )}

          {!imgError ? (
            <img
              className="lp-ziyara-hero__art"
              src={HERO_SRC}
              width={1024}
              height={910}
              decoding="async"
              fetchPriority="high"
              loading="eager"
              sizes="(max-width: 900px) 92vw, min(520px, 44vw)"
              alt="Ziyara: global booking platform — stays, dining, transport, and experiences."
              style={{ opacity: imgLoaded ? 1 : 0, transition: 'opacity 0.45s ease' }}
              onLoad={() => setImgLoaded(true)}
              onError={() => { setImgError(true); setImgLoaded(true) }}
            />
          ) : (
            <div className="lp-ziyara-hero-fallback">
              <LandingHeroArt variant="inline" />
            </div>
          )}

          {/* Hotspots with visible hover labels */}
          <div className="lp-ziyara-hero__hotspots" role="group" aria-label="Service shortcuts">
            {HOTSPOTS.map((h) => (
              <Link
                key={h.to}
                to={h.to}
                className={`lp-ziyara-hero__hotspot ${h.cls}`}
                aria-label={h.ariaLabel}
              >
                <span className="lp-ziyara-hero__hotspot-label">{h.label}</span>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
