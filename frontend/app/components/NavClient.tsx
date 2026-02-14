'use client'

import { useAuth } from '@/lib/auth-context'
import Link from 'next/link'
import { useRouter } from 'next/navigation'

export function NavClient() {
  const { isAuthenticated, userId, logout, isLoading } = useAuth()
  const router = useRouter()

  const handleLogout = async () => {
    await logout()
    router.push('/')
  }

  return (
    <nav className="bg-blue-600 text-white shadow-lg">
      <div className="container mx-auto px-4 py-4">
        <div className="flex justify-between items-center">
          <h1 className="text-2xl font-bold">💳 Payment System</h1>
          <div className="flex items-center space-x-4">
            <Link href="/" className="hover:text-blue-200 px-3 py-2 rounded">
              Home
            </Link>
            <Link href="/store" className="hover:text-blue-200 px-3 py-2 rounded">
              Merchant Store
            </Link>
            <Link href="/users" className="hover:text-blue-200 px-3 py-2 rounded">
              Users
            </Link>
            <Link href="/payments" className="hover:text-blue-200 px-3 py-2 rounded">
              Payments
            </Link>
            <Link href="/transactions" className="hover:text-blue-200 px-3 py-2 rounded">
              Transactions
            </Link>
            {!isLoading &&
              (isAuthenticated ? (
                <>
                  {userId != null && (
                    <span className="text-blue-200 text-sm">User {userId}</span>
                  )}
                  <button
                    type="button"
                    onClick={handleLogout}
                    className="hover:text-blue-200 px-3 py-2 rounded"
                  >
                    Logout
                  </button>
                </>
              ) : (
                <Link href="/login" className="hover:text-blue-200 px-3 py-2 rounded">
                  Login
                </Link>
              ))}
          </div>
        </div>
      </div>
    </nav>
  )
}
