'use client'

import { useState } from 'react'
import { transactionApi } from '@/lib/api'

export default function TransactionsPage() {
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null)

  // View transaction
  const [transactionId, setTransactionId] = useState('')
  const [transactionData, setTransactionData] = useState<any>(null)

  // List transactions
  const [transactions, setTransactions] = useState<any[]>([])
  const [filters, setFilters] = useState({
    paymentId: '',
    type: '',
  })

  const handleGetTransaction = async () => {
    if (!transactionId) return
    setLoading(true)
    setMessage(null)
    try {
      const data = await transactionApi.getTransaction(parseInt(transactionId))
      setTransactionData(data)
    } catch (error: any) {
      setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to get transaction' })
      setTransactionData(null)
    } finally {
      setLoading(false)
    }
  }

  const handleGetAllTransactions = async () => {
    setLoading(true)
    setMessage(null)
    try {
      const queryFilters: any = {}
      if (filters.paymentId) queryFilters.paymentId = parseInt(filters.paymentId)
      if (filters.type) queryFilters.type = filters.type
      
      const data = await transactionApi.getAllTransactions(
        Object.keys(queryFilters).length > 0 ? queryFilters : undefined
      )
      setTransactions(Array.isArray(data) ? data : [])
    } catch (error: any) {
      setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to get transactions' })
      setTransactions([])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto">
      <h1 className="text-3xl font-bold text-black mb-6">Transaction History</h1>

      {message && (
        <div className={`mb-4 p-4 rounded ${
          message.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
        }`}>
          {message.text}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* View Transaction */}
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-2xl font-semibold text-black mb-4">View Transaction</h2>
          <div className="flex space-x-2 mb-4">
            <input
              type="number"
              value={transactionId}
              onChange={(e) => setTransactionId(e.target.value)}
              placeholder="Transaction ID"
              className="px-3 py-2 border rounded-md flex-1 text-black"
            />
            <button
              onClick={handleGetTransaction}
              disabled={loading}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? 'Loading...' : 'Get Transaction'}
            </button>
          </div>
          {transactionData && (
            <div className="mt-4 p-4 bg-gray-50 rounded">
              <pre className="text-sm text-black overflow-auto">{JSON.stringify(transactionData, null, 2)}</pre>
            </div>
          )}
        </div>

        {/* List Transactions */}
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-2xl font-semibold text-black mb-4">All Transactions</h2>
          <div className="space-y-4 mb-4">
            <div>
              <label className="block text-sm font-medium text-black mb-1">Payment ID (Optional)</label>
              <input
                type="number"
                value={filters.paymentId}
                onChange={(e) => setFilters({ ...filters, paymentId: e.target.value })}
                className="w-full px-3 py-2 border rounded-md text-black"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-black mb-1">Type (Optional)</label>
              <select
                value={filters.type}
                onChange={(e) => setFilters({ ...filters, type: e.target.value })}
                className="w-full px-3 py-2 border rounded-md text-black"
              >
                <option value="">All Types</option>
                <option value="PAYMENT">PAYMENT</option>
                <option value="REFUND">REFUND</option>
              </select>
            </div>
            <button
              onClick={handleGetAllTransactions}
              disabled={loading}
              className="w-full bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? 'Loading...' : 'Get Transactions'}
            </button>
          </div>
        </div>
      </div>

      {/* Transactions List */}
      {transactions.length > 0 && (
        <div className="mt-6 bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-2xl font-semibold text-black mb-4">Transaction List</h2>
          <div className="space-y-2">
            {transactions.map((transaction: any) => (
              <div key={transaction.id} className="p-4 bg-gray-50 rounded border">
                <div className="flex justify-between items-start">
                  <div>
                    <p className="text-black"><strong>ID:</strong> {transaction.id}</p>
                    <p className="text-black"><strong>Payment ID:</strong> {transaction.paymentId}</p>
                    <p className="text-black"><strong>Type:</strong> <span className={`px-2 py-1 rounded text-sm ${
                      transaction.type === 'PAYMENT' ? 'bg-green-100 text-green-800' : 'bg-orange-100 text-orange-800'
                    }`}>{transaction.type}</span></p>
                    <p className="text-black"><strong>Amount:</strong> {transaction.amount}</p>
                    <p className="text-black"><strong>Status:</strong> <span className={`px-2 py-1 rounded text-sm ${
                      transaction.status === 'COMPLETED' ? 'bg-green-100 text-green-800' :
                      transaction.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                      'bg-red-100 text-red-800'
                    }`}>{transaction.status}</span></p>
                    {transaction.reference && (
                      <p className="text-black"><strong>Reference:</strong> {transaction.reference}</p>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
