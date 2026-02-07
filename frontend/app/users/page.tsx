'use client'

import { useState } from 'react'
import { userApi } from '@/lib/api'

export default function UsersPage() {
  const [activeTab, setActiveTab] = useState<'create' | 'view' | 'wallet'>('create')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null)

  // Create user state
  const [userForm, setUserForm] = useState({
    username: '',
    email: '',
    phoneNumber: '',
    isActive: true,
    isVerified: false,
  })

  // View user state
  const [userId, setUserId] = useState('')
  const [userData, setUserData] = useState<any>(null)

  // Wallet state
  const [walletUserId, setWalletUserId] = useState('')
  const [walletData, setWalletData] = useState<any>(null)
  const [topUpAmount, setTopUpAmount] = useState('')
  const [topUpUserId, setTopUpUserId] = useState('')

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setMessage(null)
    try {
      const data = await userApi.createUser(userForm)
      setMessage({ type: 'success', text: `User created successfully! ID: ${data.id}` })
      setUserForm({ username: '', email: '', phoneNumber: '', isActive: true, isVerified: false })
    } catch (error: any) {
      setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to create user' })
    } finally {
      setLoading(false)
    }
  }

  const handleGetUser = async () => {
    if (!userId) return
    setLoading(true)
    setMessage(null)
    try {
      const data = await userApi.getUser(parseInt(userId))
      setUserData(data)
    } catch (error: any) {
      setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to get user' })
      setUserData(null)
    } finally {
      setLoading(false)
    }
  }

  const handleGetWallet = async () => {
    if (!walletUserId) return
    setLoading(true)
    setMessage(null)
    try {
      const [balance, available, details] = await Promise.all([
        userApi.getWalletBalance(parseInt(walletUserId)),
        userApi.getAvailableBalance(parseInt(walletUserId)),
        userApi.getWalletDetails(parseInt(walletUserId)),
      ])
      setWalletData({ balance, available, details })
    } catch (error: any) {
      setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to get wallet' })
      setWalletData(null)
    } finally {
      setLoading(false)
    }
  }

  const handleTopUp = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!topUpUserId || !topUpAmount) return
    setLoading(true)
    setMessage(null)
    try {
      const data = await userApi.topUpWallet(parseInt(topUpUserId), parseFloat(topUpAmount))
      setMessage({ type: 'success', text: `Wallet topped up! New balance: ${data.balance}` })
      setTopUpAmount('')
      if (walletUserId === topUpUserId) {
        handleGetWallet()
      }
    } catch (error: any) {
      setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to top up wallet' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold text-black mb-6">User Management</h1>

      {message && (
        <div className={`mb-4 p-4 rounded ${
          message.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
        }`}>
          {message.text}
        </div>
      )}

      <div className="flex space-x-2 mb-6 border-b">
        <button
          onClick={() => setActiveTab('create')}
          className={`px-4 py-2 font-semibold ${
            activeTab === 'create' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-600'
          }`}
        >
          Create User
        </button>
        <button
          onClick={() => setActiveTab('view')}
          className={`px-4 py-2 font-semibold ${
            activeTab === 'view' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-600'
          }`}
        >
          View User
        </button>
        <button
          onClick={() => setActiveTab('wallet')}
          className={`px-4 py-2 font-semibold ${
            activeTab === 'wallet' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-600'
          }`}
        >
          Wallet Management
        </button>
      </div>

      {activeTab === 'create' && (
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-2xl font-semibold text-black mb-4">Create New User</h2>
          <form onSubmit={handleCreateUser} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-black mb-1">Username</label>
              <input
                type="text"
                value={userForm.username}
                onChange={(e) => setUserForm({ ...userForm, username: e.target.value })}
                required
                className="w-full px-3 py-2 border rounded-md text-black"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-black mb-1">Email</label>
              <input
                type="email"
                value={userForm.email}
                onChange={(e) => setUserForm({ ...userForm, email: e.target.value })}
                required
                className="w-full px-3 py-2 border rounded-md text-black"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-black mb-1">Phone Number</label>
              <input
                type="tel"
                value={userForm.phoneNumber}
                onChange={(e) => setUserForm({ ...userForm, phoneNumber: e.target.value })}
                required
                className="w-full px-3 py-2 border rounded-md text-black"
              />
            </div>
            <div className="flex items-center space-x-4">
              <label className="flex items-center text-black">
                <input
                  type="checkbox"
                  checked={userForm.isActive}
                  onChange={(e) => setUserForm({ ...userForm, isActive: e.target.checked })}
                  className="mr-2"
                />
                Active
              </label>
              <label className="flex items-center text-black">
                <input
                  type="checkbox"
                  checked={userForm.isVerified}
                  onChange={(e) => setUserForm({ ...userForm, isVerified: e.target.checked })}
                  className="mr-2"
                />
                Verified
              </label>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? 'Creating...' : 'Create User'}
            </button>
          </form>
        </div>
      )}

      {activeTab === 'view' && (
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-2xl font-semibold text-black mb-4">View User Details</h2>
          <div className="flex space-x-2 mb-4">
            <input
              type="number"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              placeholder="User ID"
              className="px-3 py-2 border rounded-md flex-1 text-black"
            />
            <button
              onClick={handleGetUser}
              disabled={loading}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? 'Loading...' : 'Get User'}
            </button>
          </div>
          {userData && (
            <div className="mt-4 p-4 bg-gray-50 rounded">
              <pre className="text-sm text-black overflow-auto">{JSON.stringify(userData, null, 2)}</pre>
            </div>
          )}
        </div>
      )}

      {activeTab === 'wallet' && (
        <div className="space-y-6">
          <div className="bg-white rounded-lg shadow-lg p-6">
            <h2 className="text-2xl font-semibold text-black mb-4">View Wallet</h2>
            <div className="flex space-x-2 mb-4">
              <input
                type="number"
                value={walletUserId}
                onChange={(e) => setWalletUserId(e.target.value)}
                placeholder="User ID"
                className="px-3 py-2 border rounded-md flex-1 text-black"
              />
              <button
                onClick={handleGetWallet}
                disabled={loading}
                className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
              >
                {loading ? 'Loading...' : 'Get Wallet'}
              </button>
            </div>
            {walletData && (
              <div className="mt-4 space-y-4">
                <div className="p-4 bg-gray-50 rounded">
                  <h3 className="font-semibold text-black mb-2">Wallet Balance</h3>
                  <p className="text-2xl font-bold text-green-600">${walletData.balance.balance || 0}</p>
                </div>
                <div className="p-4 bg-gray-50 rounded">
                  <h3 className="font-semibold text-black mb-2">Available Balance</h3>
                  <p className="text-2xl font-bold text-blue-600">${walletData.available.availableBalance || 0}</p>
                </div>
                <div className="p-4 bg-gray-50 rounded">
                  <h3 className="font-semibold text-black mb-2">Wallet Details</h3>
                  <pre className="text-sm text-black overflow-auto">{JSON.stringify(walletData.details, null, 2)}</pre>
                </div>
              </div>
            )}
          </div>

          <div className="bg-white rounded-lg shadow-lg p-6">
            <h2 className="text-2xl font-semibold text-black mb-4">Top Up Wallet</h2>
            <form onSubmit={handleTopUp} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-black mb-1">User ID</label>
                <input
                  type="number"
                  value={topUpUserId}
                  onChange={(e) => setTopUpUserId(e.target.value)}
                  required
                  className="w-full px-3 py-2 border rounded-md text-black"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-black mb-1">Amount</label>
                <input
                  type="number"
                  step="0.01"
                  value={topUpAmount}
                  onChange={(e) => setTopUpAmount(e.target.value)}
                  required
                  className="w-full px-3 py-2 border rounded-md text-black"
                />
              </div>
              <button
                type="submit"
                disabled={loading}
                className="bg-green-600 text-white px-6 py-2 rounded hover:bg-green-700 disabled:opacity-50"
              >
                {loading ? 'Processing...' : 'Top Up Wallet'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
