import type { Metadata } from 'next'
import './globals.css'
import { StoreProvider } from '@/lib/store-context'
import { AuthProvider } from '@/lib/auth-context'
import { NavClient } from '@/app/components/NavClient'

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
        <AuthProvider>
          <StoreProvider>
            <NavClient />
            <main className="container mx-auto px-4 py-8">
              {children}
            </main>
          </StoreProvider>
        </AuthProvider>
      </body>
    </html>
  )
}
