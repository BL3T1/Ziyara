import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, act, waitFor } from '@testing-library/react'
import { AuthProvider, useAuth, getStoredToken, setStoredToken } from '../context/AuthContext'

function TestConsumer() {
  const { user, isAuthenticated, setUser, logout } = useAuth()
  return (
    <div>
      <span data-testid="authenticated">{String(isAuthenticated)}</span>
      <span data-testid="user-email">{user?.email ?? 'none'}</span>
      <button type="button" onClick={() => setUser({ id: '1', email: 'test@test.com', name: 'Test', role: 'admin' })}>
        Login
      </button>
      <button type="button" onClick={() => setUser(null)}>Clear</button>
      <button type="button" onClick={logout}>Logout</button>
    </div>
  )
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('provides unauthenticated state by default', () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    )
    expect(screen.getByTestId('authenticated')).toHaveTextContent('false')
    expect(screen.getByTestId('user-email')).toHaveTextContent('none')
  })

  it('updates state when setUser is called', async () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    )
    screen.getByText('Login').click()
    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true')
      expect(screen.getByTestId('user-email')).toHaveTextContent('test@test.com')
    })
  })

  it('persists user to localStorage when set', async () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    )
    screen.getByText('Login').click()
    await waitFor(() => {
      const stored = localStorage.getItem('user')
      expect(stored).toBeTruthy()
      expect(JSON.parse(stored!).email).toBe('test@test.com')
    })
  })

  it('logout clears user and token from localStorage', () => {
    setStoredToken('fake-token')
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    )
    act(() => {
      screen.getByText('Login').click()
    })
    expect(localStorage.getItem('user')).toBeTruthy()
    act(() => {
      screen.getByText('Logout').click()
    })
    expect(localStorage.getItem('user')).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('getStoredToken and setStoredToken work', () => {
    expect(getStoredToken()).toBeNull()
    setStoredToken('abc')
    expect(getStoredToken()).toBe('abc')
  })
})
