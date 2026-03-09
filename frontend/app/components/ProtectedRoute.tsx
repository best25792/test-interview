'use client'

import { useAuth } from '@/lib/auth-context'
import { usePathname, useRouter } from 'next/navigation'
import { useEffect } from 'react'

type ProtectedRouteProps = {
  children: React.ReactNode
  /** If set, user must have this role (e.g. PAYMENT_USER, MERCHANT, ADMIN). */
  requiredRole?: string
}

export function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const { isAuthenticated, roles, isLoading } = useAuth()
  const router = useRouter()
  const pathname = usePathname()

  const hasRequiredRole = !requiredRole || roles.includes(requiredRole)

  useEffect(() => {
    if (isLoading) return
    if (!isAuthenticated) {
      const redirect = encodeURIComponent(pathname)
      router.replace(`/login?redirect=${redirect}`)
      return
    }
    if (!hasRequiredRole) {
      router.replace('/')
    }
  }, [isLoading, isAuthenticated, hasRequiredRole, pathname, router])

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

  if (!hasRequiredRole) {
    return (
      <div className="max-w-6xl mx-auto py-12 text-center text-gray-600">
        You do not have permission to view this page. Redirecting...
      </div>
    )
  }

  return <>{children}</>
}
