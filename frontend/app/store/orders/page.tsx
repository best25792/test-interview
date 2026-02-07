'use client'

import { useState, useEffect, useCallback } from 'react'
import Link from 'next/link'
import { useOrders } from '@/lib/store-context'
import { orderApi } from '@/lib/api'
import { MERCHANT_ID } from '@/lib/store-data'

const statusColors: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-800',
  pending: 'bg-amber-100 text-amber-800',
  PAID: 'bg-green-100 text-green-800',
  paid: 'bg-green-100 text-green-800',
  SHIPPED: 'bg-blue-100 text-blue-800',
  shipped: 'bg-blue-100 text-blue-800',
  CANCELLED: 'bg-red-100 text-red-800',
  cancelled: 'bg-red-100 text-red-800',
}

type OrderDisplay = {
  id: string
  status: string
  total: number
  currency: string
  createdAt: string
  paymentId?: number
  customerUserId?: number
  items: { name: string; price: number; quantity: number }[]
  source: 'api' | 'local'
}

export default function OrdersPage() {
  const { orders: localOrders, updateOrderStatus: updateLocalOrderStatus } = useOrders()
  const [apiOrders, setApiOrders] = useState<OrderDisplay[]>([])
  const [loading, setLoading] = useState(true)
  const [useBackend, setUseBackend] = useState(false)

  const fetchOrders = useCallback(async () => {
    try {
      const list = await orderApi.listOrders({ merchantId: MERCHANT_ID })
      const mapped: OrderDisplay[] = (list || []).map((o: {
        id: number
        status: string
        total: number
        currency: string
        createdAt: string
        paymentId?: number
        customerUserId?: number
        items?: { productName: string; unitPrice: number; quantity: number }[]
      }) => ({
        id: String(o.id),
        status: o.status,
        total: o.total,
        currency: o.currency ?? 'USD',
        createdAt: o.createdAt,
        paymentId: o.paymentId,
        customerUserId: o.customerUserId,
        items: (o.items ?? []).map((i: { productName: string; unitPrice: number; quantity: number }) => ({
          name: i.productName,
          price: i.unitPrice,
          quantity: i.quantity,
        })),
        source: 'api',
      }))
      setApiOrders(mapped)
      setUseBackend(true)
    } catch (_) {
      setUseBackend(false)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchOrders()
  }, [fetchOrders])

  const handleStatusChange = async (orderId: string, status: string, source: 'api' | 'local') => {
    if (source === 'api') {
      try {
        await orderApi.updateOrderStatus(Number(orderId), status)
        await fetchOrders()
      } catch (_) {}
      return
    }
    updateLocalOrderStatus(orderId, status as 'pending' | 'paid' | 'shipped' | 'cancelled')
  }

  const orders: OrderDisplay[] = useBackend
    ? apiOrders
    : localOrders.map((o) => ({
        id: o.id,
        status: o.status,
        total: o.total,
        currency: o.currency,
        createdAt: o.createdAt,
        paymentId: o.paymentId,
        customerUserId: o.customerUserId,
        items: o.items.map((i) => ({ name: i.name, price: i.price, quantity: i.quantity })),
        source: 'local' as const,
      }))

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-black mb-6">Orders</h1>
        <p className="text-gray-600">Loading orders...</p>
      </div>
    )
  }

  if (orders.length === 0) {
    return (
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-black mb-6">Orders</h1>
        <div className="bg-white rounded-xl shadow-md border border-gray-200 p-12 text-center">
          <p className="text-gray-600 mb-6">No orders yet.</p>
          <Link
            href="/store"
            className="inline-block bg-blue-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-blue-700"
          >
            Go to store
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-black">Orders</h1>
        <div className="flex gap-2 items-center">
          {useBackend && (
            <span className="text-sm text-green-600 font-medium">Backend</span>
          )}
          <Link
            href="/store"
            className="bg-blue-600 text-white px-4 py-2 rounded-lg font-semibold hover:bg-blue-700"
          >
            Back to store
          </Link>
        </div>
      </div>

      <div className="space-y-4">
        {orders
          .slice()
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .map((order) => (
            <div
              key={order.id}
              className="bg-white rounded-xl shadow-md border border-gray-200 overflow-hidden"
            >
              <div className="p-4 flex flex-wrap justify-between items-start gap-4">
                <div>
                  <div className="flex items-center gap-3 flex-wrap">
                    <span className="font-mono font-semibold text-black">{order.id}</span>
                    <span
                      className={`px-2 py-1 rounded text-sm font-medium ${
                        statusColors[order.status] ?? 'bg-gray-100 text-gray-800'
                      }`}
                    >
                      {order.status}
                    </span>
                    {order.paymentId != null && (
                      <span className="text-sm text-gray-500">Payment #{order.paymentId}</span>
                    )}
                  </div>
                  <p className="text-sm text-gray-500 mt-1">
                    {new Date(order.createdAt).toLocaleString()}
                    {order.customerUserId != null && (
                      <> · Customer User ID: {order.customerUserId}</>
                    )}
                  </p>
                </div>
                <div className="text-right">
                  <span className="text-lg font-bold text-black">
                    ${Number(order.total).toFixed(2)} {order.currency}
                  </span>
                </div>
              </div>
              <div className="border-t border-gray-100 px-4 py-3 bg-gray-50">
                <ul className="text-sm text-gray-700 space-y-1">
                  {order.items.map((item, idx) => (
                    <li key={idx}>
                      {item.name} × {item.quantity} — ${(item.price * item.quantity).toFixed(2)}
                    </li>
                  ))}
                </ul>
              </div>
              {(order.status === 'PAID' || order.status === 'paid') && (
                <div className="border-t border-gray-100 px-4 py-3 flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => handleStatusChange(order.id, 'SHIPPED', order.source)}
                    className="px-3 py-1.5 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700"
                  >
                    Mark shipped
                  </button>
                  <button
                    type="button"
                    onClick={() => handleStatusChange(order.id, 'CANCELLED', order.source)}
                    className="px-3 py-1.5 bg-red-100 text-red-700 rounded-lg text-sm font-medium hover:bg-red-200"
                  >
                    Cancel order
                  </button>
                </div>
              )}
              {(order.status === 'PENDING' || order.status === 'pending') && (
                <div className="border-t border-gray-100 px-4 py-3 flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => handleStatusChange(order.id, 'CANCELLED', order.source)}
                    className="px-3 py-1.5 bg-red-100 text-red-700 rounded-lg text-sm font-medium hover:bg-red-200"
                  >
                    Cancel order
                  </button>
                </div>
              )}
            </div>
          ))}
      </div>
    </div>
  )
}
