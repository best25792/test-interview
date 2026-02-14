'use client'

import { useAuth } from '@/lib/auth-context'
import { usePathname, useRouter } from 'next/navigation'
import { useEffect } from 'react'

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth()
  const router = useRouter()
  const pathname = usePathname()

  useEffect(() => {
    if (isLoading) return
    if (!isAuthenticated) {
      const redirect = encodeURIComponent(pathname)
      router.replace(`/login?redirect=${redirect}`)
    }
  }, [isLoading, isAuthenticated, pathname, router])

  if (isLoading) {
    return (
      <div className="max-w-6xl mx-auto py-12 text-center text-gray-600">
        Checking authentication...
      </div>
    )
  }

  if (!isAuthenticated) {
    return (
      <div className="max-w-6xl mx-auto py-12 text-center text-gray-600">
        Redirecting to login...
      </div>
    )
  }

  return <>{children}</>
}
