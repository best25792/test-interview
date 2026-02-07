'use client'

import Link from 'next/link'
import { useCart } from '@/lib/store-context'

export default function CartPage() {
  const { items, totalItems, totalAmount, removeFromCart, setCartQuantity, clearCart } =
    useCart()

  if (items.length === 0) {
    return (
      <div className="max-w-2xl mx-auto text-center py-12">
        <h1 className="text-2xl font-bold text-black mb-4">Your cart is empty</h1>
        <p className="text-gray-600 mb-6">Add items from the store to continue.</p>
        <Link
          href="/store"
          className="inline-block bg-blue-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-blue-700"
        >
          Browse products
        </Link>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold text-black mb-6">🛒 Cart</h1>

      <div className="bg-white rounded-xl shadow-md border border-gray-200 overflow-hidden">
        <ul className="divide-y divide-gray-200">
          {items.map(({ product, quantity }) => (
            <li key={product.id} className="p-4 flex justify-between items-center">
              <div>
                <h2 className="font-semibold text-black">{product.name}</h2>
                <p className="text-sm text-gray-500">${product.price.toFixed(2)} each</p>
              </div>
              <div className="flex items-center gap-3">
                <div className="flex items-center border rounded-lg overflow-hidden">
                  <button
                    type="button"
                    onClick={() => setCartQuantity(product.id, quantity - 1)}
                    className="px-3 py-1 bg-gray-100 hover:bg-gray-200 text-black"
                  >
                    −
                  </button>
                  <span className="px-4 py-1 min-w-[2rem] text-center font-medium">
                    {quantity}
                  </span>
                  <button
                    type="button"
                    onClick={() =>
                      setCartQuantity(product.id, Math.min(quantity + 1, product.stock))
                    }
                    disabled={quantity >= product.stock}
                    className="px-3 py-1 bg-gray-100 hover:bg-gray-200 disabled:opacity-50 text-black"
                  >
                    +
                  </button>
                </div>
                <span className="font-semibold text-black w-20 text-right">
                  ${(product.price * quantity).toFixed(2)}
                </span>
                <button
                  type="button"
                  onClick={() => removeFromCart(product.id)}
                  className="text-red-600 hover:text-red-800 text-sm font-medium"
                >
                  Remove
                </button>
              </div>
            </li>
          ))}
        </ul>

        <div className="p-4 bg-gray-50 border-t flex justify-between items-center">
          <div>
            <span className="text-gray-600">Total ({totalItems} items): </span>
            <span className="text-xl font-bold text-black">${totalAmount.toFixed(2)}</span>
          </div>
          <div className="flex gap-3">
            <button
              type="button"
              onClick={() => clearCart()}
              className="px-4 py-2 text-gray-600 hover:text-red-600 font-medium"
            >
              Clear cart
            </button>
            <Link
              href="/store/checkout"
              className="bg-green-600 text-white px-6 py-2 rounded-lg font-semibold hover:bg-green-700"
            >
              Proceed to checkout
            </Link>
          </div>
        </div>
      </div>

      <p className="mt-4">
        <Link href="/store" className="text-blue-600 hover:text-blue-800">
          ← Continue shopping
        </Link>
      </p>
    </div>
  )
}
