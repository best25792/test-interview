import type { Metadata } from 'next'
import './globals.css'
import Link from 'next/link'
import { StoreProvider } from '@/lib/store-context'

export const metadata: Metadata = {
  title: 'Payment System - E-Wallet',
  description: 'QR Code Payment System Frontend',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body>
        <StoreProvider>
          <nav className="bg-blue-600 text-white shadow-lg">
            <div className="container mx-auto px-4 py-4">
              <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold">💳 Payment System</h1>
                <div className="flex space-x-4">
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
                </div>
              </div>
            </div>
          </nav>
          <main className="container mx-auto px-4 py-8">
            {children}
          </main>
        </StoreProvider>
      </body>
    </html>
  )
}
