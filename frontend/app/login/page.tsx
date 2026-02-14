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

function LoginForm() {
  const { verifyOtp, requestOtp, isAuthenticated, isLoading } = useAuth()
  const router = useRouter()
  const searchParams = useSearchParams()
  const redirect = searchParams.get('redirect') ?? '/'

  const [step, setStep] = useState<'phone' | 'otp'>('phone')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [code, setCode] = useState('')
  const [channel, setChannel] = useState<'SMS' | 'EMAIL'>('SMS')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [resendCooldown, setResendCooldown] = useState(0)

  useEffect(() => {
    if (!isLoading && isAuthenticated) router.replace(redirect)
  }, [isLoading, isAuthenticated, redirect, router])

  const handleRequestOtp = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault()
      setError(null)
      setMessage(null)
      if (!phoneNumber.trim()) return
      setLoading(true)
      try {
        await requestOtp(phoneNumber.trim(), channel)
        setMessage('OTP sent. Check your ' + (channel === 'SMS' ? 'phone' : 'email') + '.')
        setStep('otp')
        setCode('')
        setResendCooldown(60)
        let count = 60
        const interval = setInterval(() => {
          count -= 1
          setResendCooldown(count)
          if (count <= 0) clearInterval(interval)
        }, 1000)
      } catch (err: any) {
        setError(err.response?.data?.message ?? err.message ?? 'Failed to send OTP')
      } finally {
        setLoading(false)
      }
    },
    [phoneNumber, channel, requestOtp]
  )

  const handleVerifyOtp = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault()
      setError(null)
      if (!code.trim()) return
      setLoading(true)
      try {
        const deviceId = getDeviceId()
        await verifyOtp(phoneNumber.trim(), code.trim(), deviceId)
        router.replace(redirect)
      } catch (err: any) {
        setError(err.response?.data?.message ?? err.message ?? 'Invalid or expired OTP')
      } finally {
        setLoading(false)
      }
    },
    [phoneNumber, code, verifyOtp, redirect, router]
  )

  const handleResend = useCallback(() => {
    if (resendCooldown > 0) return
    setError(null)
    setMessage(null)
    setLoading(true)
    requestOtp(phoneNumber.trim(), channel)
      .then(() => {
        setMessage('OTP sent again.')
        setResendCooldown(60)
      })
      .catch((err: any) => setError(err.response?.data?.message ?? 'Failed to resend'))
      .finally(() => setLoading(false))
  }, [phoneNumber, channel, requestOtp, resendCooldown])

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
      <h1 className="text-2xl font-bold text-black mb-6">Log in</h1>
      <p className="text-gray-600 mb-6">
        Enter your phone number to receive a one-time code. In non-prod the code is logged on the
        server.
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

      {step === 'phone' && (
        <form onSubmit={handleRequestOtp} className="space-y-4">
          <div>
            <label htmlFor="phone" className="block text-sm font-medium text-gray-700 mb-1">
              Phone number
            </label>
            <input
              id="phone"
              type="tel"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              placeholder="+1234567890"
              className="w-full border border-gray-300 rounded px-3 py-2 text-black"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Channel</label>
            <select
              value={channel}
              onChange={(e) => setChannel(e.target.value as 'SMS' | 'EMAIL')}
              className="w-full border border-gray-300 rounded px-3 py-2 text-black"
            >
              <option value="SMS">SMS</option>
              <option value="EMAIL">Email</option>
            </select>
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? 'Sending...' : 'Send OTP'}
          </button>
        </form>
      )}

      {step === 'otp' && (
        <form onSubmit={handleVerifyOtp} className="space-y-4">
          <p className="text-sm text-gray-600">Code sent to {phoneNumber}</p>
          <div>
            <label htmlFor="code" className="block text-sm font-medium text-gray-700 mb-1">
              OTP code
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
          <div className="flex justify-between items-center text-sm">
            <button
              type="button"
              onClick={() => setStep('phone')}
              className="text-blue-600 hover:underline"
            >
              Change number
            </button>
            <button
              type="button"
              onClick={handleResend}
              disabled={loading || resendCooldown > 0}
              className="text-blue-600 hover:underline disabled:opacity-50"
            >
              {resendCooldown > 0 ? `Resend in ${resendCooldown}s` : 'Resend OTP'}
            </button>
          </div>
        </form>
      )}

      <p className="mt-6 text-center text-sm text-gray-500">
        <Link href="/login/recover" className="text-blue-600 hover:underline">
          Recover via email
        </Link>
        {' · '}
        <Link href="/" className="text-blue-600 hover:underline">
          Back to home
        </Link>
      </p>
    </div>
  )
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="max-w-md mx-auto mt-12 text-center text-gray-600">Loading...</div>}>
      <LoginForm />
    </Suspense>
  )
}
