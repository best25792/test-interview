import Link from 'next/link'

export default function Home() {
  return (
    <div className="max-w-6xl mx-auto">
      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold text-black mb-4">
          Welcome to Payment System
        </h1>
        <p className="text-xl text-black">
          QR Code-based E-Wallet Payment System
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {/* User Management Card */}
        <div className="bg-white rounded-lg shadow-lg p-6 hover:shadow-xl transition-shadow">
          <div className="text-4xl mb-4">👤</div>
          <h2 className="text-2xl font-semibold text-black mb-3">User Management</h2>
          <p className="text-black mb-4">
            Create users, view user details, and manage user accounts
          </p>
          <Link 
            href="/users"
            className="inline-block bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors"
          >
            Manage Users →
          </Link>
        </div>

        {/* Wallet Management Card */}
        <div className="bg-white rounded-lg shadow-lg p-6 hover:shadow-xl transition-shadow">
          <div className="text-4xl mb-4">💼</div>
          <h2 className="text-2xl font-semibold text-black mb-3">Wallet Management</h2>
          <p className="text-black mb-4">
            Check wallet balance, top up wallet, and view wallet details
          </p>
          <Link 
            href="/users"
            className="inline-block bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 transition-colors"
          >
            Manage Wallet →
          </Link>
        </div>

        {/* Merchant Store Card */}
        <div className="bg-white rounded-lg shadow-lg p-6 hover:shadow-xl transition-shadow">
          <div className="text-4xl mb-4">🛒</div>
          <h2 className="text-2xl font-semibold text-black mb-3">Merchant Store</h2>
          <p className="text-black mb-4">
            Simulate a merchant site: browse products, cart, inventory, and pay with Wallet QR
          </p>
          <Link
            href="/store"
            className="inline-block bg-amber-500 text-black px-4 py-2 rounded hover:bg-amber-600 transition-colors font-semibold"
          >
            Open Store →
          </Link>
        </div>

        {/* Payment Processing Card */}
        <div className="bg-white rounded-lg shadow-lg p-6 hover:shadow-xl transition-shadow">
          <div className="text-4xl mb-4">💳</div>
          <h2 className="text-2xl font-semibold text-black mb-3">Payment Processing</h2>
          <p className="text-black mb-4">
            Initiate payments, process QR codes, confirm and manage payments
          </p>
          <Link 
            href="/payments"
            className="inline-block bg-purple-600 text-white px-4 py-2 rounded hover:bg-purple-700 transition-colors"
          >
            Process Payments →
          </Link>
        </div>

        {/* Transaction History Card */}
        <div className="bg-white rounded-lg shadow-lg p-6 hover:shadow-xl transition-shadow">
          <div className="text-4xl mb-4">📊</div>
          <h2 className="text-2xl font-semibold text-black mb-3">Transaction History</h2>
          <p className="text-black mb-4">
            View transaction history and payment records
          </p>
          <Link 
            href="/transactions"
            className="inline-block bg-orange-600 text-white px-4 py-2 rounded hover:bg-orange-700 transition-colors"
          >
            View Transactions →
          </Link>
        </div>
      </div>

      <div className="mt-12 bg-blue-50 rounded-lg p-6">
        <h2 className="text-2xl font-semibold text-black mb-4">🔗 API Configuration</h2>
        <p className="text-black mb-2">
          <strong>Backend API:</strong> <code className="bg-gray-200 px-2 py-1 rounded">http://localhost:8080</code>
        </p>
        <p className="text-black text-sm">
          Make sure the backend server is running on port 8080 before using the frontend.
        </p>
      </div>
    </div>
  )
}
