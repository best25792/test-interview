'use client'

import { useState, useEffect, useRef } from 'react'
import { paymentApi } from '@/lib/api'
import dynamic from 'next/dynamic'
import { ProtectedRoute } from '@/app/components/ProtectedRoute'
import { useAuth } from '@/lib/auth-context'

const QRCode = dynamic(() => import('qrcode.react'), { ssr: false })

export default function PaymentsPage() {
  const { userId: authUserId } = useAuth()
  const [activeTab, setActiveTab] = useState<'initiate' | 'list' | 'process' | 'view'>('initiate')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null)

  // Initiate payment
  const [initiateUserId, setInitiateUserId] = useState('')
  const [paymentResult, setPaymentResult] = useState<any>(null)
  const [timeRemaining, setTimeRemaining] = useState<number | null>(null)
  const [isExpired, setIsExpired] = useState(false)
  const [isPolling, setIsPolling] = useState(false)
  const timerRef = useRef<NodeJS.Timeout | null>(null)
  const pollingRef = useRef<NodeJS.Timeout | null>(null)

  // View payment
  const [paymentId, setPaymentId] = useState('')
  const [paymentData, setPaymentData] = useState<any>(null)

  // Process payment
  const [processPaymentId, setProcessPaymentId] = useState('')
  const [processForm, setProcessForm] = useState({
    qrCode: '',
    amount: '',
    currency: 'USD',
    merchantId: '',
    description: '',
  })

  // Confirm/Cancel/Refund
  const [actionPaymentId, setActionPaymentId] = useState('')
  const [refundAmount, setRefundAmount] = useState('')

  // List payments
  const [payments, setPayments] = useState<any[]>([])
  const [statusFilter, setStatusFilter] = useState('')

  const handleInitiatePayment = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!initiateUserId) return
    setLoading(true)
    setMessage(null)
    setIsExpired(false)
    
    // Stop any existing polling
    if (pollingRef.current) {
      clearInterval(pollingRef.current)
      pollingRef.current = null
    }
    
    try {
      // Step 1: Initiate payment (Saga Pattern - returns only payment ID)
      const data = await paymentApi.initiatePayment(parseInt(initiateUserId))
      const paymentId = data.transactionId
      
      console.log('[Initiate Payment] Payment initiated, paymentId:', paymentId)
      
      setMessage({ type: 'success', text: 'Payment initiated. Waiting for QR code...' })
      
      // Initialize payment result with transaction ID to show in UI
      setPaymentResult({
        transactionId: paymentId,
        message: 'Waiting for QR code generation... (Attempt 0/60)'
      })
      
      // Step 2: Start polling for payment status until QR code is available
      setIsPolling(true)
      startPollingPaymentStatus(paymentId)
      
    } catch (error: any) {
      console.error('[Initiate Payment] Error:', error)
      setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to initiate payment' })
      setPaymentResult(null)
      setTimeRemaining(null)
      setIsExpired(false)
      setIsPolling(false)
      if (pollingRef.current) {
        clearInterval(pollingRef.current)
        pollingRef.current = null
      }
    } finally {
      setLoading(false)
    }
  }

  const startPollingPaymentStatus = (paymentId: number) => {
    let pollCount = 0
    const maxPolls = 60 // Poll for up to 60 times (5 minutes with 5 second intervals)
    
    const poll = async () => {
      try {
        console.log(`[Polling] Attempt ${pollCount + 1}/${maxPolls} for paymentId: ${paymentId}`)
        const statusData = await paymentApi.getPaymentStatus(paymentId)
        pollCount++
        
        console.log(`[Polling] Payment status response:`, {
          paymentId: statusData.id,
          status: statusData.status,
          hasQRCode: !!statusData.qrCode,
          qrCodeCode: statusData.qrCode?.code
        })
        
        // Check if QR code is available
        if (statusData.qrCode && statusData.qrCode.code) {
          // QR code is ready!
          console.log(`[Polling] QR code found! Stopping polling.`)
          setPaymentResult({
            transactionId: paymentId,
            qrCode: statusData.qrCode.code,
            expiresAt: statusData.qrCode.expiresAt,
            message: 'QR code generated successfully!'
          })
          
          setIsPolling(false)
          if (pollingRef.current) {
            clearInterval(pollingRef.current)
            pollingRef.current = null
          }
          
          setMessage({ type: 'success', text: 'QR code generated successfully!' })
          
          // Start timer
          if (statusData.qrCode.expiresAt) {
            const expiresAt = new Date(statusData.qrCode.expiresAt).getTime()
            startTimer(expiresAt)
          } else {
            const expiresAt = Date.now() + (15 * 60 * 1000)
            startTimer(expiresAt)
          }
        } else if (pollCount >= maxPolls) {
          // Timeout - stop polling
          console.log(`[Polling] Timeout reached after ${pollCount} attempts`)
          setIsPolling(false)
          if (pollingRef.current) {
            clearInterval(pollingRef.current)
            pollingRef.current = null
          }
          setMessage({ type: 'error', text: 'Timeout waiting for QR code. Please try again.' })
        } else {
          // Continue polling - update UI to show polling status
          console.log(`[Polling] No QR code yet, continuing... (attempt ${pollCount}/${maxPolls})`)
          setPaymentResult((prev: { transactionId: number; message: string } | null) => ({
            ...prev,
            transactionId: paymentId,
            message: `Waiting for QR code... (Attempt ${pollCount}/${maxPolls})`
          }))
        }
      } catch (error: any) {
        console.error('[Polling] Error polling payment status:', error)
        // Continue polling on error (might be temporary)
        setPaymentResult((prev: { transactionId: number; message: string } | null) => ({
          ...prev,
          transactionId: paymentId,
          message: `Error polling status, retrying... (Attempt ${pollCount}/${maxPolls})`
        }))
      }
    }
    
    // Poll immediately, then every 5 seconds
    console.log(`[Polling] Starting polling for paymentId: ${paymentId}`)
    poll()
    pollingRef.current = setInterval(poll, 5000)
  }

  const startTimer = (expiresAt: number) => {
    // Clear existing timer
    if (timerRef.current) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
    
    // Reset expired state
    setIsExpired(false)
    
    const updateTimer = () => {
      const now = Date.now()
      const remaining = Math.max(0, Math.floor((expiresAt - now) / 1000))
      
      setTimeRemaining(remaining)
      
      if (remaining <= 0) {
        setIsExpired(true)
        if (timerRef.current) {
          clearInterval(timerRef.current)
          timerRef.current = null
        }
      }
    }
    
    // Update immediately
    updateTimer()
    
    // Update every second
    timerRef.current = setInterval(updateTimer, 1000)
  }

  const handleRetry = async () => {
    // Clear existing timers first
    if (timerRef.current) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
    if (pollingRef.current) {
      clearInterval(pollingRef.current)
      pollingRef.current = null
    }
    
    // Reset timer state
    setTimeRemaining(null)
    setIsExpired(false)
    setMessage(null)
    setIsPolling(false)
    
    // Automatically regenerate if we have a user ID
    if (initiateUserId) {
      setLoading(true)
      try {
        // Initiate new payment (Saga Pattern)
        const data = await paymentApi.initiatePayment(parseInt(initiateUserId))
        const paymentId = data.transactionId
        
        setMessage({ type: 'success', text: 'Payment initiated. Waiting for QR code...' })
        
        // Start polling for new QR code
        setIsPolling(true)
        startPollingPaymentStatus(paymentId)
      } catch (error: any) {
        setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to generate new QR code' })
        setTimeRemaining(null)
        setIsExpired(false)
        setIsPolling(false)
      } finally {
        setLoading(false)
      }
    } else {
      // If no user ID, just clear the result
      setPaymentResult(null)
    }
  }

  // Cleanup timers on unmount
  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current)
      }
      if (pollingRef.current) {
        clearInterval(pollingRef.current)
      }
    }
  }, [])

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }

  const handleGetPayment = async () => {
    if (!paymentId) return
    setLoading(true)
    setMessage(null)
    try {
      const data = await paymentApi.getPayment(parseInt(paymentId))
      setPaymentData(data)
    } catch (error: any) {
      setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to get payment' })
      setPaymentData(null)
    } finally {
      setLoading(false)
    }
  }

  const handleProcessPayment = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!processPaymentId) return
    setLoading(true)
    setMessage(null)
    try {
      const data = await paymentApi.processPayment(parseInt(processPaymentId), {
        qrCode: processForm.qrCode,
        amount: parseFloat(processForm.amount),
        currency: processForm.currency,
        merchantId: processForm.merchantId,
        description: processForm.description,
      })
      setMessage({ type: 'success', text: 'Payment processed successfully!' })
      setPaymentData(data)
    } catch (error: any) {
      setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to process payment' })
    } finally {
      setLoading(false)
    }
  }

  const handleConfirmPayment = async () => {
    if (!actionPaymentId) return
    setLoading(true)
    setMessage(null)
    try {
      const data = await paymentApi.confirmPayment(parseInt(actionPaymentId))
      setMessage({ type: 'success', text: 'Payment confirmed successfully!' })
      setPaymentData(data)
    } catch (error: any) {
      setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to confirm payment' })
    } finally {
      setLoading(false)
    }
  }

  const handleCancelPayment = async () => {
    if (!actionPaymentId) return
    setLoading(true)
    setMessage(null)
    try {
      const data = await paymentApi.cancelPayment(parseInt(actionPaymentId))
      setMessage({ type: 'success', text: 'Payment cancelled successfully!' })
      setPaymentData(data)
    } catch (error: any) {
      setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to cancel payment' })
    } finally {
      setLoading(false)
    }
  }

  const handleRefundPayment = async () => {
    if (!actionPaymentId) return
    setLoading(true)
    setMessage(null)
    try {
      const amount = refundAmount ? parseFloat(refundAmount) : undefined
      const data = await paymentApi.refundPayment(parseInt(actionPaymentId), amount)
      setMessage({ type: 'success', text: 'Refund processed successfully!' })
      setPaymentData(data)
    } catch (error: any) {
      setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to refund payment' })
    } finally {
      setLoading(false)
    }
  }

  const handleGetAllPayments = async () => {
    setLoading(true)
    setMessage(null)
    try {
      const data = await paymentApi.getAllPayments(statusFilter || undefined)
      setPayments(Array.isArray(data) ? data : [])
    } catch (error: any) {
      setMessage({ type: 'error', text: error.response?.data?.message || 'Failed to get payments' })
      setPayments([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (authUserId != null && !initiateUserId) setInitiateUserId(String(authUserId))
    // eslint-disable-next-line react-hooks/exhaustive-deps -- only default when auth loads, don't overwrite if user cleared
  }, [authUserId])

  return (
    <ProtectedRoute>
    <div className="max-w-6xl mx-auto">
      <h1 className="text-3xl font-bold text-black mb-6">Payment Management</h1>

      {message && (
        <div className={`mb-4 p-4 rounded ${
          message.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
        }`}>
          {message.text}
        </div>
      )}

      <div className="flex flex-wrap space-x-2 mb-6 border-b">
        <button
          onClick={() => setActiveTab('initiate')}
          className={`px-4 py-2 font-semibold ${
            activeTab === 'initiate' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-600'
          }`}
        >
          Initiate Payment
        </button>
        <button
          onClick={() => setActiveTab('list')}
          className={`px-4 py-2 font-semibold ${
            activeTab === 'list' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-600'
          }`}
        >
          List Payments
        </button>
        <button
          onClick={() => setActiveTab('process')}
          className={`px-4 py-2 font-semibold ${
            activeTab === 'process' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-600'
          }`}
        >
          Process Payment
        </button>
        <button
          onClick={() => setActiveTab('view')}
          className={`px-4 py-2 font-semibold ${
            activeTab === 'view' ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-600'
          }`}
        >
          View/Manage Payment
        </button>
      </div>

      {activeTab === 'initiate' && (
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-2xl font-semibold text-black mb-4">Initiate Payment (User)</h2>
          <form onSubmit={handleInitiatePayment} className="space-y-4 mb-4">
            <div>
              <label className="block text-sm font-medium text-black mb-1">User ID</label>
              <input
                type="number"
                value={initiateUserId}
                onChange={(e) => setInitiateUserId(e.target.value)}
                required
                className="w-full px-3 py-2 border rounded-md text-black"
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? 'Initiating...' : 'Initiate Payment'}
            </button>
          </form>

          {(paymentResult || isPolling) && (
            <div className="mt-6 p-4 bg-gray-50 rounded">
              <h3 className="font-semibold text-black mb-2">Payment Initiated</h3>
              <div className="mb-4">
                {paymentResult?.transactionId && (
                  <p className="text-black"><strong>Transaction ID:</strong> {paymentResult.transactionId}</p>
                )}
                
                {(isPolling || (paymentResult?.message && !paymentResult?.qrCode)) && (
                  <div className="mt-3 p-3 bg-blue-50 border border-blue-200 rounded">
                    <p className="text-blue-800 font-semibold">🔄 Waiting for QR code generation...</p>
                    <p className="text-blue-600 text-sm mt-1">
                      {paymentResult?.message || 'Polling payment status...'}
                    </p>
                  </div>
                )}
                
                {paymentResult?.qrCode && (
                  <>
                    <p className="text-black"><strong>QR Code:</strong> {paymentResult.qrCode}</p>
                    <div className="mt-3">
                      {timeRemaining !== null ? (
                        <p className={`text-lg font-bold ${isExpired ? 'text-red-600' : timeRemaining < 60 ? 'text-orange-600' : 'text-green-600'}`}>
                          {isExpired ? '⏰ QR Code Expired' : `⏱️ Time Remaining: ${formatTime(timeRemaining)}`}
                        </p>
                      ) : paymentResult.expiresAt ? (
                        <p className="text-lg font-bold text-gray-600">
                          ⏱️ Timer starting...
                        </p>
                      ) : (
                        <p className="text-lg font-bold text-gray-600">
                          ⏱️ QR Code expires in 15 minutes
                        </p>
                      )}
                    </div>
                  </>
                )}
              </div>
              {!isExpired && paymentResult?.qrCode && (
                <div className="flex justify-center mb-4">
                  <QRCode value={paymentResult.qrCode || ''} size={256} />
                </div>
              )}
              {isExpired && paymentResult?.qrCode && (
                <div className="text-center mb-4">
                  <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
                    <p className="font-semibold">QR Code has expired</p>
                    <p className="text-sm mt-1">Please generate a new QR code to continue</p>
                  </div>
                </div>
              )}
              {paymentResult?.qrCode && (
                <div className="flex justify-center gap-2">
                  <button
                    onClick={handleRetry}
                    disabled={loading || !initiateUserId}
                    className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 transition-colors disabled:opacity-50"
                  >
                    {loading ? 'Generating...' : 'Generate New QR Code'}
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {activeTab === 'list' && (
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-2xl font-semibold text-black mb-4">All Payments</h2>
          <div className="flex space-x-2 mb-4">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-3 py-2 border rounded-md text-black"
            >
              <option value="">All Status</option>
              <option value="PENDING">PENDING</option>
              <option value="PROCESSING">PROCESSING</option>
              <option value="COMPLETED">COMPLETED</option>
              <option value="FAILED">FAILED</option>
              <option value="CANCELLED">CANCELLED</option>
              <option value="REFUNDED">REFUNDED</option>
            </select>
            <button
              onClick={handleGetAllPayments}
              disabled={loading}
              className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? 'Loading...' : 'Get Payments'}
            </button>
          </div>
          {payments.length > 0 && (
            <div className="mt-4 space-y-2">
              {payments.map((payment: any) => (
                <div key={payment.id} className="p-4 bg-gray-50 rounded border">
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="text-black"><strong>ID:</strong> {payment.id}</p>
                      <p className="text-black"><strong>Amount:</strong> {payment.amount} {payment.currency}</p>
                      <p className="text-black"><strong>Status:</strong> <span className={`px-2 py-1 rounded text-sm ${
                        payment.status === 'COMPLETED' ? 'bg-green-100 text-green-800' :
                        payment.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                        payment.status === 'PROCESSING' ? 'bg-blue-100 text-blue-800' :
                        'bg-red-100 text-red-800'
                      }`}>{payment.status}</span></p>
                    </div>
                    <button
                      onClick={() => {
                        setPaymentId(payment.id.toString())
                        setActiveTab('view')
                      }}
                      className="text-blue-600 hover:text-blue-800"
                    >
                      View Details →
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {activeTab === 'process' && (
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-2xl font-semibold text-black mb-4">Process Payment (Merchant)</h2>
          <form onSubmit={handleProcessPayment} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-black mb-1">Payment ID</label>
              <input
                type="number"
                value={processPaymentId}
                onChange={(e) => setProcessPaymentId(e.target.value)}
                required
                className="w-full px-3 py-2 border rounded-md text-black"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-black mb-1">QR Code</label>
              <input
                type="text"
                value={processForm.qrCode}
                onChange={(e) => setProcessForm({ ...processForm, qrCode: e.target.value })}
                required
                className="w-full px-3 py-2 border rounded-md text-black"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-black mb-1">Amount</label>
                <input
                  type="number"
                  step="0.01"
                  value={processForm.amount}
                  onChange={(e) => setProcessForm({ ...processForm, amount: e.target.value })}
                  required
                  className="w-full px-3 py-2 border rounded-md text-black"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-black mb-1">Currency</label>
                <input
                  type="text"
                  value={processForm.currency}
                  onChange={(e) => setProcessForm({ ...processForm, currency: e.target.value })}
                  required
                  className="w-full px-3 py-2 border rounded-md text-black"
                />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-black mb-1">Merchant ID</label>
              <input
                type="text"
                value={processForm.merchantId}
                onChange={(e) => setProcessForm({ ...processForm, merchantId: e.target.value })}
                required
                className="w-full px-3 py-2 border rounded-md text-black"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-black mb-1">Description (Optional)</label>
              <input
                type="text"
                value={processForm.description}
                onChange={(e) => setProcessForm({ ...processForm, description: e.target.value })}
                className="w-full px-3 py-2 border rounded-md text-black"
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="bg-green-600 text-white px-6 py-2 rounded hover:bg-green-700 disabled:opacity-50"
            >
              {loading ? 'Processing...' : 'Process Payment'}
            </button>
          </form>
        </div>
      )}

      {activeTab === 'view' && (
        <div className="space-y-6">
          <div className="bg-white rounded-lg shadow-lg p-6">
            <h2 className="text-2xl font-semibold text-black mb-4">View Payment</h2>
            <div className="flex space-x-2 mb-4">
              <input
                type="number"
                value={paymentId}
                onChange={(e) => setPaymentId(e.target.value)}
                placeholder="Payment ID"
                className="px-3 py-2 border rounded-md flex-1 text-black"
              />
              <button
                onClick={handleGetPayment}
                disabled={loading}
                className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
              >
                {loading ? 'Loading...' : 'Get Payment'}
              </button>
            </div>
            {paymentData && (
              <div className="mt-4 p-4 bg-gray-50 rounded">
                <pre className="text-sm text-black overflow-auto">{JSON.stringify(paymentData, null, 2)}</pre>
              </div>
            )}
          </div>

          <div className="bg-white rounded-lg shadow-lg p-6">
            <h2 className="text-2xl font-semibold text-black mb-4">Payment Actions</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-black mb-1">Payment ID</label>
                <input
                  type="number"
                  value={actionPaymentId}
                  onChange={(e) => setActionPaymentId(e.target.value)}
                  className="w-full px-3 py-2 border rounded-md text-black"
                />
              </div>
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={handleConfirmPayment}
                  disabled={loading || !actionPaymentId}
                  className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 disabled:opacity-50"
                >
                  Confirm Payment
                </button>
                <button
                  onClick={handleCancelPayment}
                  disabled={loading || !actionPaymentId}
                  className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 disabled:opacity-50"
                >
                  Cancel Payment
                </button>
              </div>
              <div className="border-t pt-4">
                <div className="mb-2">
                  <label className="block text-sm font-medium text-black mb-1">Refund Amount (Optional, leave empty for full refund)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={refundAmount}
                    onChange={(e) => setRefundAmount(e.target.value)}
                    placeholder="Amount"
                    className="w-full px-3 py-2 border rounded-md text-black"
                  />
                </div>
                <button
                  onClick={handleRefundPayment}
                  disabled={loading || !actionPaymentId}
                  className="bg-orange-600 text-white px-4 py-2 rounded hover:bg-orange-700 disabled:opacity-50"
                >
                  Refund Payment
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
    </ProtectedRoute>
  )
}
