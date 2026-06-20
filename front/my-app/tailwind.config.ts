import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        /* Primary — Amber Gold ("Desert Horizon" brand) */
        primary: {
          DEFAULT: '#C8893A',
          50:  '#FDF6E8',
          100: '#F8EAC8',
          200: '#F0D09A',
          300: '#E8B870',
          400: '#D9A055',
          500: '#C8893A',
          600: '#B8742C',
          700: '#A35F20',
          800: '#8C4A18',
          900: '#6B3510',
          950: '#3D200A',
        },
        /* Secondary — Deep Teal */
        secondary: {
          DEFAULT: '#1A7A8F',
          50:  '#EAF7FA',
          100: '#C8EDF4',
          200: '#90D5E2',
          300: '#52B8CC',
          400: '#2B9AAF',
          500: '#1A7A8F',
          600: '#176878',
          700: '#125162',
          800: '#0D3A48',
          900: '#07232D',
          950: '#031318',
        },
        /* Accent — Terracotta */
        accent: {
          DEFAULT: '#C8432C',
          100: '#FAE5E0',
          300: '#E58F80',
          400: '#D9614E',
          500: '#C8432C',
          600: '#B43520',
          700: '#932A12',
        },
        /* Warm neutrals */
        warm: {
          50:  '#F8F5EE',
          100: '#EDE9E0',
          200: '#D8D2C4',
          300: '#BFB8A8',
          400: '#A09790',
          500: '#8A8070',
          600: '#6B6250',
          700: '#4F4835',
          800: '#38311F',
          900: '#252015',
          950: '#18130C',
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
        'glow-primary': '0 0 24px -4px rgba(200, 137, 58, 0.45)',
        'glow-secondary': '0 0 24px -4px rgba(26, 122, 143, 0.35)',
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
