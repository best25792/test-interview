'use client'

import { useAuth } from '@/lib/auth-context'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { useCallback, useEffect, useState, Suspense } from 'react'

const DEVICE_ID_KEY = 'payment_system_device_id'

function getDeviceId(): string {
  if (typeof window === 'undefined') return 'web'
  let id = localStorage.getItem(DEVICE_ID_KEY)
  if (!id) {
    id = crypto.randomUUID?.() ?? `web-${Date.now()}`
    localStorage.setItem(DEVICE_ID_KEY, id)
  }
  return id
}

function RecoverForm() {
  const { verifyRecoveryOtp, requestRecoveryOtp, isAuthenticated, isLoading } = useAuth()
  const router = useRouter()
  const searchParams = useSearchParams()
  const redirect = searchParams.get('redirect') ?? '/'

  const [step, setStep] = useState<'email' | 'otp'>('email')
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    if (!isLoading && isAuthenticated) router.replace(redirect)
  }, [isLoading, isAuthenticated, redirect, router])

  const handleRequestOtp = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault()
      setError(null)
      setMessage(null)
      if (!email.trim()) return
      setLoading(true)
      try {
        await requestRecoveryOtp(email.trim())
        setMessage('Recovery OTP sent to your email.')
        setStep('otp')
        setCode('')
      } catch (err: any) {
        setError(err.response?.data?.message ?? err.message ?? 'Failed to send recovery OTP')
      } finally {
        setLoading(false)
      }
    },
    [email, requestRecoveryOtp]
  )

  const handleVerifyOtp = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault()
      setError(null)
      if (!code.trim()) return
      setLoading(true)
      try {
        const deviceId = getDeviceId()
        await verifyRecoveryOtp(email.trim(), code.trim(), deviceId)
        router.replace(redirect)
      } catch (err: any) {
        setError(err.response?.data?.message ?? err.message ?? 'Invalid or expired code')
      } finally {
        setLoading(false)
      }
    },
    [email, code, verifyRecoveryOtp, redirect, router]
  )

  if (isLoading) {
    return (
      <div className="max-w-md mx-auto mt-12 text-center text-gray-600">
        Checking session...
      </div>
    )
  }

  if (isAuthenticated) {
    return (
      <div className="max-w-md mx-auto mt-12 text-center text-gray-600">
        Redirecting...
      </div>
    )
  }

  return (
    <div className="max-w-md mx-auto mt-12 bg-white rounded-lg shadow-lg p-6">
      <h1 className="text-2xl font-bold text-black mb-6">Account recovery</h1>
      <p className="text-gray-600 mb-6">
        Enter your email to receive a one-time code. In non-prod the code is logged on the server.
      </p>

      {error && (
        <div className="mb-4 p-3 bg-red-100 text-red-800 rounded" role="alert">
          {error}
        </div>
      )}
      {message && (
        <div className="mb-4 p-3 bg-green-100 text-green-800 rounded" role="status">
          {message}
        </div>
      )}

      {step === 'email' && (
        <form onSubmit={handleRequestOtp} className="space-y-4">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
              Email
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              className="w-full border border-gray-300 rounded px-3 py-2 text-black"
              required
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? 'Sending...' : 'Send recovery code'}
          </button>
        </form>
      )}

      {step === 'otp' && (
        <form onSubmit={handleVerifyOtp} className="space-y-4">
          <p className="text-sm text-gray-600">Code sent to {email}</p>
          <div>
            <label htmlFor="code" className="block text-sm font-medium text-gray-700 mb-1">
              Recovery code
            </label>
            <input
              id="code"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 8))}
              placeholder="123456"
              className="w-full border border-gray-300 rounded px-3 py-2 text-black"
              required
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? 'Verifying...' : 'Verify and log in'}
          </button>
          <button
            type="button"
            onClick={() => setStep('email')}
            className="w-full text-sm text-blue-600 hover:underline"
          >
            Use a different email
          </button>
        </form>
      )}

      <p className="mt-6 text-center text-sm text-gray-500">
        <Link href="/login" className="text-blue-600 hover:underline">
          Back to login
        </Link>
        {' · '}
        <Link href="/" className="text-blue-600 hover:underline">
          Home
        </Link>
      </p>
    </div>
  )
}

export default function RecoverPage() {
  return (
    <Suspense fallback={<div className="max-w-md mx-auto mt-12 text-center text-gray-600">Loading...</div>}>
      <RecoverForm />
    </Suspense>
  )
}
