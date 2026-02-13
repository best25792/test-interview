'use client'

import React, { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { authApi } from '@/lib/api'
import { authStore } from '@/lib/auth-store'

type AuthState = {
  userId: number | null
  accessToken: string | null
  refreshToken: string | null
  isLoading: boolean
  isAuthenticated: boolean
}

const defaultState: AuthState = {
  userId: null,
  accessToken: null,
  refreshToken: null,
  isLoading: true,
  isAuthenticated: false,
}

const AuthContext = createContext<AuthState & {
  requestOtp: (phoneNumber: string, channel?: 'SMS' | 'EMAIL') => Promise<void>
  verifyOtp: (phoneNumber: string, code: string, deviceId?: string) => Promise<void>
  requestRecoveryOtp: (email: string) => Promise<void>
  verifyRecoveryOtp: (email: string, code: string, deviceId?: string) => Promise<void>
  logout: () => Promise<void>
  refreshTokens: () => Promise<boolean>
}>(null as any)

function readFromStore(): AuthState {
  return {
    userId: authStore.getUserId(),
    accessToken: authStore.getAccessToken(),
    refreshToken: authStore.getRefreshToken(),
    isLoading: false,
    isAuthenticated: authStore.isAuthenticated(),
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>(defaultState)

  const refreshTokens = useCallback(async (): Promise<boolean> => {
    const stored = authStore.getStoredRefreshToken()
    if (!stored) return false
    try {
      const data = await authApi.refresh(stored)
      authStore.setTokens(data.accessToken, data.refreshToken)
      setState(readFromStore())
      return true
    } catch {
      authStore.clearTokens()
      setState(readFromStore())
      return false
    }
  }, [])

  useEffect(() => {
    const stored = authStore.getStoredRefreshToken()
    if (!stored) {
      setState((s) => ({ ...s, isLoading: false }))
      return
    }
    refreshTokens().finally(() => {
      setState((s) => ({ ...s, isLoading: false }))
    })
  }, [refreshTokens])

  useEffect(() => {
    return authStore.subscribe(() => setState(readFromStore()))
  }, [])

  const requestOtp = useCallback(async (phoneNumber: string, channel: 'SMS' | 'EMAIL' = 'SMS') => {
    await authApi.requestOtp(phoneNumber, channel)
  }, [])

  const verifyOtp = useCallback(
    async (phoneNumber: string, code: string, deviceId?: string) => {
      const data = await authApi.verifyOtp(phoneNumber, code, deviceId)
      authStore.setTokens(data.accessToken, data.refreshToken)
      setState(readFromStore())
    },
    []
  )

  const requestRecoveryOtp = useCallback(async (email: string) => {
    await authApi.recoveryRequestOtp(email)
  }, [])

  const verifyRecoveryOtp = useCallback(
    async (email: string, code: string, deviceId?: string) => {
      const data = await authApi.recoveryVerifyOtp(email, code, deviceId)
      authStore.setTokens(data.accessToken, data.refreshToken)
      setState(readFromStore())
    },
    []
  )

  const logout = useCallback(async () => {
    const rt = authStore.getRefreshToken()
    if (rt) {
      try {
        await authApi.logout(rt)
      } catch {
        // best-effort
      }
    }
    authStore.clearTokens()
    setState(readFromStore())
  }, [])

  const value = {
    ...state,
    requestOtp,
    verifyOtp,
    requestRecoveryOtp,
    verifyRecoveryOtp,
    logout,
    refreshTokens,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
