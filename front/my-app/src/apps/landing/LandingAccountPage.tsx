import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useLanguage } from '../../context/LanguageContext'
import { usersAPI, getApiErrorMessage } from '../../services/api'
import type { User } from '../../types/auth'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { useToast } from './LandingToast'
import { PasswordInput } from '../../components/PasswordInput'

interface ProfileForm { firstName: string; lastName: string; phone: string }
interface PwForm { current: string; next: string; confirm: string }

export function LandingAccountPage() {
  useDocumentMeta({ title: 'My Account · Ziyara', description: 'Edit your Ziyara profile, phone number and password.' })
  const { t } = useLanguage()
  const { user, setUser, isAuthenticated } = useAuth()
  const { toast } = useToast()
  const navigate = useNavigate()

  // Redirect if not logged in
  useEffect(() => {
    if (!isAuthenticated) navigate('/login?next=/account', { replace: true })
  }, [isAuthenticated, navigate])

  // Profile form state
  const [profile, setProfile] = useState<ProfileForm>({ firstName: '', lastName: '', phone: '' })
  const [profileLoading, setProfileLoading] = useState(true)
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileSuccess, setProfileSuccess] = useState(false)
  const [profileError, setProfileError] = useState('')

  // Password form state
  const [pw, setPw] = useState<PwForm>({ current: '', next: '', confirm: '' })
  const [pwSaving, setPwSaving] = useState(false)
  const [pwSuccess, setPwSuccess] = useState(false)
  const [pwError, setPwError] = useState('')

  // Pre-fill from /users/me
  useEffect(() => {
    if (!isAuthenticated) return
    usersAPI.getMe()
      .then((res) => {
        const d = res.data as { firstName?: string; lastName?: string; phone?: string }
        setProfile({
          firstName: d.firstName ?? '',
          lastName:  d.lastName  ?? '',
          phone:     d.phone     ?? '',
        })
      })
      .catch(() => {
        setProfile({ firstName: user?.name?.split(' ')[0] ?? '', lastName: user?.name?.split(' ').slice(1).join(' ') ?? '', phone: '' })
      })
      .finally(() => setProfileLoading(false))
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAuthenticated])

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setProfileError('')
    setProfileSuccess(false)
    if (!profile.firstName.trim() || !profile.lastName.trim()) {
      setProfileError(t('account.nameRequired') || 'First and last name are required.')
      return
    }
    setProfileSaving(true)
    try {
      const res = await usersAPI.updateMe({ firstName: profile.firstName.trim(), lastName: profile.lastName.trim(), phone: profile.phone.trim() || undefined })
      const updated = res.data as { firstName?: string; lastName?: string; fullName?: string }
      // Reflect updated name in auth context so header updates
      if (user) {
        const newName = updated.fullName ?? `${updated.firstName ?? ''} ${updated.lastName ?? ''}`.trim()
        setUser({ ...(user as User), name: newName || user.name })
      }
      setProfileSuccess(true)
      toast(t('account.profileSaved') || 'Profile updated successfully.', 'success')
    } catch (err) {
      setProfileError(getApiErrorMessage(err, t('account.profileSaveFailed') || 'Failed to save profile.'))
    } finally {
      setProfileSaving(false)
    }
  }

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setPwError('')
    setPwSuccess(false)
    if (!pw.current || !pw.next) { setPwError(t('account.pwFieldsRequired') || 'All password fields are required.'); return }
    if (pw.next !== pw.confirm) { setPwError(t('account.pwMismatch') || 'New passwords do not match.'); return }
    if (pw.next.length < 8) { setPwError(t('account.pwTooShort') || 'Password must be at least 8 characters.'); return }
    setPwSaving(true)
    try {
      await usersAPI.changePassword({ currentPassword: pw.current, newPassword: pw.next })
      setPwSuccess(true)
      setPw({ current: '', next: '', confirm: '' })
      toast(t('account.pwChanged') || 'Password changed successfully.', 'success')
    } catch (err) {
      setPwError(getApiErrorMessage(err, t('account.pwChangeFailed') || 'Failed to change password.'))
    } finally {
      setPwSaving(false)
    }
  }

  if (!user) return null

  return (
    <div className="lp-sheet">
      <div className="mx-auto max-w-lg space-y-8">
        <h1 className="lp-h1 mb-2">{t('account.title') || 'My Account'}</h1>

        {/* Profile card */}
        <div className="lp-glass-card p-6">
          <h2 className="mb-4 text-lg font-semibold lp-text-heading">{t('account.profileSection') || 'Profile'}</h2>
          {profileLoading ? (
            <div className="animate-pulse space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="h-10 rounded-xl lp-skeleton" />
                <div className="h-10 rounded-xl lp-skeleton" />
              </div>
              <div className="h-10 rounded-xl lp-skeleton" />
            </div>
          ) : (
            <form onSubmit={handleProfileSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="lp-field-label">{t('account.firstName') || 'First name'}</label>
                  <input
                    type="text"
                    value={profile.firstName}
                    onChange={(e) => setProfile((p) => ({ ...p, firstName: e.target.value }))}
                    className="lp-input w-full"
                    required
                    maxLength={100}
                  />
                </div>
                <div>
                  <label className="lp-field-label">{t('account.lastName') || 'Last name'}</label>
                  <input
                    type="text"
                    value={profile.lastName}
                    onChange={(e) => setProfile((p) => ({ ...p, lastName: e.target.value }))}
                    className="lp-input w-full"
                    required
                    maxLength={100}
                  />
                </div>
              </div>
              <div>
                <label className="lp-field-label">{t('account.phone') || 'Phone'}</label>
                <input
                  type="tel"
                  value={profile.phone}
                  onChange={(e) => setProfile((p) => ({ ...p, phone: e.target.value }))}
                  className="lp-input w-full"
                  maxLength={30}
                />
              </div>
              {profileError && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{profileError}</p>}
              {profileSuccess && <p className="rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{t('account.profileSaved') || 'Profile updated successfully.'}</p>}
              <button type="submit" disabled={profileSaving} className="lp-btn lp-btn-primary w-full disabled:opacity-60">
                {profileSaving ? (t('ui.saving') || 'Saving…') : (t('account.saveProfile') || 'Save profile')}
              </button>
            </form>
          )}
        </div>

        {/* Password card */}
        <div className="lp-glass-card p-6">
          <h2 className="mb-4 text-lg font-semibold lp-text-heading">{t('account.passwordSection') || 'Change password'}</h2>
          <form onSubmit={handlePasswordSubmit} className="space-y-4">
            <div>
              <label className="lp-field-label">{t('account.currentPassword') || 'Current password'}</label>
              <PasswordInput
                id="acct-cur-pw"
                value={pw.current}
                onChange={(e) => setPw((p) => ({ ...p, current: e.target.value }))}
                className="lp-input w-full"
                wrapperClassName="mt-1"
                autoComplete="current-password"
              />
            </div>
            <div>
              <label className="lp-field-label">{t('account.newPassword') || 'New password'}</label>
              <PasswordInput
                value={pw.next}
                onChange={(e) => setPw((p) => ({ ...p, next: e.target.value }))}
                className="lp-input w-full"
                autoComplete="new-password"
              />
            </div>
            <div>
              <label className="lp-field-label">{t('account.confirmPassword') || 'Confirm new password'}</label>
              <PasswordInput
                value={pw.confirm}
                onChange={(e) => setPw((p) => ({ ...p, confirm: e.target.value }))}
                className="lp-input w-full"
                autoComplete="new-password"
              />
            </div>
            {pwError && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{pwError}</p>}
            {pwSuccess && <p className="rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{t('account.pwChanged') || 'Password changed successfully.'}</p>}
            <button type="submit" disabled={pwSaving} className="lp-btn lp-btn-primary w-full disabled:opacity-60">
              {pwSaving ? (t('ui.saving') || 'Saving…') : (t('account.changePassword') || 'Change password')}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
