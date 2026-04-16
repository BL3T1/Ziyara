import { useNavigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import type { Role } from '../types/auth'
import { useLanguage } from '../context/LanguageContext'

interface RoleOption {
  id: string
  role: Role
  icon: ReactNode
  iconColor: string
  labelKey: string
  descKey: string
}

const ShieldIcon = () => (
  <svg className="h-12 w-12" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
  </svg>
)
const BriefcaseIcon = () => (
  <svg className="h-12 w-12" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect width="20" height="14" x="2" y="7" rx="2" ry="2" />
    <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
  </svg>
)
const DollarIcon = () => (
  <svg className="h-12 w-12" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <line x1="12" x2="12" y1="2" y2="22" />
    <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
  </svg>
)
const HeadsetIcon = () => (
  <svg className="h-12 w-12" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a9 9 0 0 1 18 0v7a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3" />
  </svg>
)
const ChartIcon = () => (
  <svg className="h-12 w-12" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M3 3v18h18" />
    <path d="m19 9-5 5-4-4-3 3" />
  </svg>
)
const UsersIcon = () => (
  <svg className="h-12 w-12" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
    <circle cx="9" cy="7" r="4" />
    <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
  </svg>
)

const ROLE_OPTIONS_STATIC: RoleOption[] = [
  {
    id: 'super_admin',
    role: 'super_admin',
    icon: <ShieldIcon />,
    iconColor: 'text-violet-600',
    labelKey: 'roleSelectPage.optSuperAdmin',
    descKey: 'roleSelectPage.optSuperAdminDesc',
  },
  {
    id: 'admin',
    role: 'admin',
    icon: <BriefcaseIcon />,
    iconColor: 'text-blue-600',
    labelKey: 'roleSelectPage.optSalesManager',
    descKey: 'roleSelectPage.optSalesManagerDesc',
  },
  {
    id: 'finance',
    role: 'finance',
    icon: <DollarIcon />,
    iconColor: 'text-emerald-600',
    labelKey: 'roleSelectPage.optFinance',
    descKey: 'roleSelectPage.optFinanceDesc',
  },
  {
    id: 'support',
    role: 'support',
    icon: <HeadsetIcon />,
    iconColor: 'text-sky-600',
    labelKey: 'roleSelectPage.optSupport',
    descKey: 'roleSelectPage.optSupportDesc',
  },
  {
    id: 'executive',
    role: 'executive',
    icon: <ChartIcon />,
    iconColor: 'text-amber-600',
    labelKey: 'roleSelectPage.optExecutive',
    descKey: 'roleSelectPage.optExecutiveDesc',
  },
  {
    id: 'hr',
    role: 'hr',
    icon: <UsersIcon />,
    iconColor: 'text-rose-600',
    labelKey: 'roleSelectPage.optHr',
    descKey: 'roleSelectPage.optHrDesc',
  },
  {
    id: 'provider',
    role: 'provider',
    icon: <BriefcaseIcon />,
    iconColor: 'text-secondary',
    labelKey: 'roleSelectPage.optProvider',
    descKey: 'roleSelectPage.optProviderDesc',
  },
]

export function RoleSelectPage() {
  const { t } = useLanguage()
  const navigate = useNavigate()

  const handleSelectRole = (role: Role) => {
    sessionStorage.setItem('pendingRole', role)
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <div className="mx-auto w-full max-w-6xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="mb-8 flex justify-center">
          <img src="/logo.png" alt="Ziyara" className="h-28 w-auto" />
        </div>
        <div className="text-center">
          <h1 className="text-4xl font-bold tracking-tight text-slate-900">{t('roleSelectPage.title')}</h1>
          <p className="mt-2 text-lg text-slate-600">{t('roleSelectPage.subtitle')}</p>
        </div>

        <div className="mt-12 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {ROLE_OPTIONS_STATIC.map((option) => (
            <button
              key={option.id}
              type="button"
              onClick={() => handleSelectRole(option.role)}
              className="flex flex-col items-start rounded-xl bg-white p-6 text-left shadow-sm ring-1 ring-slate-200 transition-colors hover:bg-slate-50 hover:ring-primary/20 focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <div className="flex h-12 w-12 items-center justify-center">
                <span className={option.iconColor}>{option.icon}</span>
              </div>
              <h3 className="mt-4 text-xl font-bold text-slate-900">{t(option.labelKey)}</h3>
              <p className="mt-2 text-sm leading-relaxed text-slate-600">{t(option.descKey)}</p>
            </button>
          ))}
        </div>

        <p className="mt-16 text-center text-sm text-slate-400">{t('roleSelectPage.footer')}</p>
      </div>
    </div>
  )
}
