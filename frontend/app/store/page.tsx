'use client'

import Link from 'next/link'
import { useStore } from '@/lib/store-context'
import { useEffect } from 'react'

export default function StorePage() {
  const { products, refreshProducts, addToCart } = useStore()

  useEffect(() => {
    refreshProducts()
  }, [refreshProducts])

  return (
    <div className="max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-black">🛒 Merchant Store</h1>
        <Link
          href="/store/cart"
          className="bg-amber-500 text-black px-4 py-2 rounded-lg font-semibold hover:bg-amber-600 transition-colors"
        >
          View Cart →
        </Link>
      </div>

      <p className="text-gray-600 mb-6">
        Browse products, add to cart, and pay with Wallet QR at checkout.
      </p>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {products.map((product) => (
          <div
            key={product.id}
            className="bg-white rounded-xl shadow-md border border-gray-200 overflow-hidden hover:shadow-lg transition-shadow"
          >
            <div className="p-5">
              <h2 className="text-xl font-semibold text-black mb-2">{product.name}</h2>
              <p className="text-gray-600 text-sm mb-3 line-clamp-2">{product.description}</p>
              <div className="flex justify-between items-center">
                <span className="text-lg font-bold text-green-700">
                  ${product.price.toFixed(2)}
                </span>
                <span
                  className={`text-sm px-2 py-1 rounded ${
                    product.stock > 10
                      ? 'bg-green-100 text-green-800'
                      : product.stock > 0
                        ? 'bg-amber-100 text-amber-800'
                        : 'bg-red-100 text-red-800'
                  }`}
                >
                  Stock: {product.stock}
                </span>
              </div>
              <button
                onClick={() => addToCart(product, 1)}
                disabled={product.stock === 0}
                className="mt-4 w-full bg-blue-600 text-white py-2 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {product.stock === 0 ? 'Out of stock' : 'Add to cart'}
              </button>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-8 flex gap-4">
        <Link
          href="/store/orders"
          className="text-blue-600 hover:text-blue-800 font-medium"
        >
          Manage Orders →
        </Link>
      </div>
    </div>
  )
}
