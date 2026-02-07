'use client'

import { useState, useRef, useEffect } from 'react'
import Link from 'next/link'
import { useCart } from '@/lib/store-context'
import { useStore } from '@/lib/store-context'
import { paymentApi, orderApi } from '@/lib/api'
import { MERCHANT_ID } from '@/lib/store-data'
import { decreaseStock } from '@/lib/store-data'

type Step = 'summary' | 'done' | 'error'

export default function CheckoutPage() {
  const { items, totalAmount, totalItems, clearCart } = useCart()
  const { addOrder, updateOrderStatus, refreshProducts } = useStore()

  const [step, setStep] = useState<Step>('summary')
  const [customerUserId, setCustomerUserId] = useState('')
  const [presentedQr, setPresentedQr] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)

  const [paymentId, setPaymentId] = useState<number | null>(null)
  const timerRef = useRef<NodeJS.Timeout | null>(null)

  const [orderId, setOrderId] = useState<string | null>(null)

  useEffect(() => {
    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [])

  if (items.length === 0 && step !== 'done' && step !== 'error') {
    return (
      <div className="max-w-2xl mx-auto text-center py-12">
        <h1 className="text-2xl font-bold text-black mb-4">Cart is empty</h1>
        <Link href="/store" className="text-blue-600 hover:text-blue-800">
          Go to store
        </Link>
      </div>
    )
  }

  const parsePaymentIdFromQr = (code: string): number | null => {
    const trimmed = code.trim()
    // Expected format: PAYMENT_<paymentId>_<timestamp>_<suffix>
    const m = /^PAYMENT_(\d+)_/.exec(trimmed)
    if (!m) return null
    const id = Number(m[1])
    return Number.isFinite(id) ? id : null
  }

  const handleProcessPresentedQr = async (e: React.FormEvent) => {
    e.preventDefault()
    const userId = parseInt(customerUserId, 10)
    if (!customerUserId || isNaN(userId)) {
      setMessage({ type: 'error', text: 'Please enter a valid Customer User ID.' })
      return
    }
    if (!presentedQr.trim()) {
      setMessage({ type: 'error', text: 'Please paste/scan the customer QR code.' })
      return
    }
    const pid = parsePaymentIdFromQr(presentedQr)
    if (!pid) {
      setMessage({ type: 'error', text: 'Invalid QR format. Expected: PAYMENT_<id>_...' })
      return
    }
    setPaymentId(pid)
    setLoading(true)
    setMessage(null)
    try {
      // Merchant processes payment using customer-presented QR
      await paymentApi.processPayment(pid, {
        qrCode: presentedQr.trim(),
        amount: totalAmount,
        currency: 'USD',
        merchantId: MERCHANT_ID,
        description: `Merchant Store order`,
      })
      await paymentApi.confirmPayment(pid)

      const orderItems = items.map(({ product, quantity }) => ({
        productId: Number(product.id),
        quantity,
      }))

      try {
        const created = await orderApi.createOrder({
          merchantId: MERCHANT_ID,
          customerUserId: userId,
          paymentId: pid,
          items: orderItems,
        })
        setOrderId(String(created.id))
        await orderApi.updateOrderStatus(created.id, 'PAID')
        await refreshProducts()
      } catch (_) {
        const orderItemsLocal = items.map(({ product, quantity }) => ({
          productId: product.id,
          name: product.name,
          price: product.price,
          quantity,
        }))
        for (const { product, quantity } of items) {
          decreaseStock(product.id, quantity)
        }
        await refreshProducts()
        const order = addOrder({
          items: orderItemsLocal,
          total: totalAmount,
          currency: 'USD',
          status: 'paid',
          paymentId: pid,
          customerUserId: userId,
        })
        setOrderId(order.id)
      }
      clearCart()
      setStep('done')
      setMessage({ type: 'success', text: 'Payment confirmed. Order placed.' })
    } catch (err: unknown) {
      const text =
        err && typeof err === 'object' && 'response' in err
          ? String((err as { response?: { data?: { message?: string } } }).response?.data?.message)
          : 'Failed to confirm payment.'
      setMessage({ type: 'error', text: text || 'Failed to confirm payment.' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h1 className="text-3xl font-bold text-black mb-6">Checkout</h1>

      {message && (
        <div
          className={`mb-4 p-4 rounded-lg ${
            message.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
          }`}
        >
          {message.text}
        </div>
      )}

      {step === 'summary' && (
        <div className="bg-white rounded-xl shadow-md border border-gray-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-black mb-4">Order summary</h2>
          <ul className="space-y-2 mb-4">
            {items.map(({ product, quantity }) => (
              <li key={product.id} className="flex justify-between text-black">
                <span>
                  {product.name} × {quantity}
                </span>
                <span>${(product.price * quantity).toFixed(2)}</span>
              </li>
            ))}
          </ul>
          <p className="text-xl font-bold text-black border-t pt-4">
            Total: ${totalAmount.toFixed(2)} USD
          </p>
        </div>
      )}

      {step === 'summary' && (
        <form onSubmit={handleProcessPresentedQr} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-black mb-1">
              Customer User ID (wallet owner)
            </label>
            <input
              type="number"
              value={customerUserId}
              onChange={(e) => setCustomerUserId(e.target.value)}
              placeholder="e.g. 1"
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-black"
            />
            <p className="text-sm text-gray-500 mt-1">
              This is used for creating the merchant order record.
            </p>
          </div>
          <div>
            <label className="block text-sm font-medium text-black mb-1">
              Customer QR Code (presented to merchant)
            </label>
            <textarea
              value={presentedQr}
              onChange={(e) => setPresentedQr(e.target.value)}
              placeholder="Paste/scan QR payload here (e.g. PAYMENT_123_... )"
              required
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-black font-mono"
            />
            <p className="text-sm text-gray-500 mt-1">
              The merchant receives this from the customer (scan or copy). Merchant does not generate QR.
            </p>
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-green-600 text-white py-3 rounded-lg font-semibold hover:bg-green-700 disabled:opacity-50"
          >
            {loading ? 'Processing...' : 'Process customer QR & confirm payment'}
          </button>
        </form>
      )}

      {step === 'done' && orderId && (
        <div className="bg-green-50 border border-green-200 rounded-xl p-6 text-center">
          <p className="text-xl font-semibold text-green-800 mb-2">Order placed successfully</p>
          <p className="text-black mb-4">Order ID: <strong>{orderId}</strong></p>
          <Link
            href="/store/orders"
            className="inline-block bg-green-600 text-white px-6 py-2 rounded-lg font-semibold hover:bg-green-700"
          >
            View orders
          </Link>
          <span className="mx-2">or</span>
          <Link href="/store" className="text-blue-600 hover:text-blue-800 font-medium">
            Continue shopping
          </Link>
        </div>
      )}

      {step === 'error' && (
        <div className="flex gap-4">
          <button
            type="button"
            onClick={() => {
              setStep('summary')
              setMessage(null)
              setPaymentId(null)
              setPresentedQr('')
            }}
            className="bg-blue-600 text-white px-6 py-2 rounded-lg font-semibold hover:bg-blue-700"
          >
            Try again
          </button>
          <Link href="/store/cart" className="text-gray-600 hover:text-black">
            Back to cart
          </Link>
        </div>
      )}

      <p className="mt-6">
        <Link href="/store/cart" className="text-blue-600 hover:text-blue-800">
          ← Back to cart
        </Link>
      </p>
    </div>
  )
}
