import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#1e4d6b',
          50: '#f0f7ff',
          100: '#daeeff',
          200: '#bddeff',
          300: '#90caff',
          400: '#5baef0',
          500: '#3793dc',
          600: '#2275c1',
          700: '#1e5e9c',
          800: '#1e4d6b',
          900: '#1c3f59',
          950: '#12293c',
        },
        secondary: {
          DEFAULT: '#ac9e78',
          50: '#faf8f2',
          100: '#f3eedf',
          200: '#e6dbbf',
          300: '#d4c197',
          400: '#c2a870',
          500: '#ac9e78',
          600: '#9c8a61',
          700: '#81704f',
          800: '#6a5d44',
          900: '#574e3a',
          950: '#2e291e',
        },
        zinc: {
          925: '#111113',
          950: '#0c0c0e',
        },
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
        display: ['"DM Sans"', 'Inter', 'sans-serif'],
        mono: ['"JetBrains Mono"', '"Fira Code"', 'monospace'],
      },
      fontSize: {
        '2xs': ['0.625rem', { lineHeight: '1rem' }],
      },
      boxShadow: {
        'glow-primary': '0 0 24px -4px rgba(30, 77, 107, 0.45)',
        'glow-secondary': '0 0 24px -4px rgba(172, 158, 120, 0.35)',
        'card-light': '0 1px 3px 0 rgba(0,0,0,0.04), 0 4px 16px -4px rgba(0,0,0,0.06)',
        'card-dark': '0 1px 2px 0 rgba(0,0,0,0.4), 0 8px 32px -8px rgba(0,0,0,0.6)',
        'header': '0 1px 0 0 rgba(255,255,255,0.04), 0 4px 24px -4px rgba(0,0,0,0.5)',
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'noise': "url(\"data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E\")",
      },
      keyframes: {
        'fade-in': {
          from: { opacity: '0' },
          to: { opacity: '1' },
        },
        'fade-up': {
          from: { opacity: '0', transform: 'translateY(12px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        'slide-in-right': {
          from: { opacity: '0', transform: 'translateX(16px)' },
          to: { opacity: '1', transform: 'translateX(0)' },
        },
        'scale-in': {
          from: { opacity: '0', transform: 'scale(0.95)' },
          to: { opacity: '1', transform: 'scale(1)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
      },
      animation: {
        'fade-in': 'fade-in 0.2s ease-out',
        'fade-up': 'fade-up 0.3s ease-out',
        'slide-in-right': 'slide-in-right 0.25s ease-out',
        'scale-in': 'scale-in 0.2s ease-out',
        shimmer: 'shimmer 2s linear infinite',
      },
      transitionTimingFunction: {
        'out-expo': 'cubic-bezier(0.19, 1, 0.22, 1)',
        'in-out-expo': 'cubic-bezier(0.87, 0, 0.13, 1)',
      },
      borderRadius: {
        '4xl': '2rem',
      },
    },
  },
  plugins: [],
} satisfies Config
