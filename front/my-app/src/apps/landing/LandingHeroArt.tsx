/**
 * Decorative SVG for the landing hero — abstract travel / horizon motif (no external assets).
 */
export function LandingHeroArt() {
  return (
    <div className="landing-3d-stage relative mx-auto w-full max-w-md select-none" aria-hidden>
      <div className="landing-aurora pointer-events-none absolute -left-6 -top-5 h-16 w-16 rounded-full bg-primary/30 blur-xl dark:bg-primary/35" />
      <div className="landing-float landing-float-slow pointer-events-none absolute -right-4 top-8 h-10 w-10 rounded-full border border-secondary/55 bg-secondary/25 shadow-xl shadow-secondary/20" />
      <div className="landing-orbit pointer-events-none absolute left-1/2 top-5 h-3 w-3 -translate-x-1/2 rounded-full bg-white/80 shadow-lg shadow-white/40 dark:bg-white/60" />
      <svg
        viewBox="0 0 440 360"
        className="landing-float landing-3d-card h-auto w-full text-primary/90 drop-shadow-[0_35px_45px_rgba(30,77,107,0.22)] dark:text-primary/80"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <linearGradient id="lh-sun" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="currentColor" stopOpacity="0.35" />
            <stop offset="100%" stopColor="currentColor" stopOpacity="0.08" />
          </linearGradient>
          <linearGradient id="lh-wave" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="currentColor" stopOpacity="0.22" />
            <stop offset="100%" stopColor="currentColor" stopOpacity="0.05" />
          </linearGradient>
          <linearGradient id="lh-gold" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#ac9e78" stopOpacity="0.55" />
            <stop offset="100%" stopColor="#ac9e78" stopOpacity="0.15" />
          </linearGradient>
        </defs>
        <circle cx="320" cy="96" r="72" fill="url(#lh-sun)" />
        <circle cx="320" cy="96" r="48" stroke="currentColor" strokeOpacity="0.2" strokeWidth="1" />
        <path
          d="M0 248 C 88 200, 176 288, 264 232 C 320 198, 360 210, 440 188 L 440 360 L 0 360 Z"
          fill="url(#lh-wave)"
        />
        <path
          d="M0 268 C 100 228, 200 300, 320 248 C 368 224, 400 232, 440 220 L 440 360 L 0 360 Z"
          fill="url(#lh-gold)"
          opacity="0.45"
        />
        <path
          d="M48 320 L 120 220 L 192 280 L 264 200 L 336 260 L 392 200"
          stroke="currentColor"
          strokeOpacity="0.35"
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <circle cx="120" cy="220" r="5" fill="currentColor" fillOpacity="0.45" />
        <circle cx="264" cy="200" r="5" fill="currentColor" fillOpacity="0.45" />
        <circle cx="392" cy="200" r="5" fill="#ac9e78" fillOpacity="0.85" />
      </svg>
    </div>
  )
}
