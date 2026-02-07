'use client'

import React, { createContext, useContext, useCallback, useReducer, useEffect } from 'react'
import type { Product, CartItem, Order, OrderItem } from './store-data'
import {
  getProductsWithStock,
  initialProducts,
  setApiProducts,
  clearApiProducts,
} from './store-data'
import { productApi } from './api'

const ORDERS_KEY = 'merchant-orders'

// Cart state
type CartState = { items: CartItem[] }

type CartAction =
  | { type: 'ADD'; product: Product; quantity?: number }
  | { type: 'REMOVE'; productId: string }
  | { type: 'SET_QUANTITY'; productId: string; quantity: number }
  | { type: 'CLEAR' }

function cartReducer(state: CartState, action: CartAction): CartState {
  switch (action.type) {
    case 'ADD': {
      const qty = action.quantity ?? 1
      const existing = state.items.find((i) => i.product.id === action.product.id)
      const products = getProductsWithStock()
      const product = products.find((p) => p.id === action.product.id) ?? action.product
      const maxQty = Math.min(product.stock, existing ? existing.quantity + qty : qty)
      if (maxQty <= 0) return state
      if (existing) {
        return {
          items: state.items.map((i) =>
            i.product.id === action.product.id ? { ...i, quantity: maxQty } : i
          ),
        }
      }
      return { items: [...state.items, { product, quantity: maxQty }] }
    }
    case 'REMOVE':
      return { items: state.items.filter((i) => i.product.id !== action.productId) }
    case 'SET_QUANTITY': {
      if (action.quantity <= 0) {
        return { items: state.items.filter((i) => i.product.id !== action.productId) }
      }
      const products = getProductsWithStock()
      const product = products.find((p) => p.id === action.productId)
      const maxQty = product ? Math.min(product.stock, action.quantity) : action.quantity
      return {
        items: state.items.map((i) =>
          i.product.id === action.productId ? { ...i, quantity: maxQty } : i
        ),
      }
    }
    case 'CLEAR':
      return { items: [] }
    default:
      return state
  }
}

// Orders state (persisted)
function loadOrders(): Order[] {
  if (typeof window === 'undefined') return []
  try {
    const s = localStorage.getItem(ORDERS_KEY)
    if (s) return JSON.parse(s)
  } catch (_) {}
  return []
}

function saveOrders(orders: Order[]) {
  if (typeof window === 'undefined') return
  localStorage.setItem(ORDERS_KEY, JSON.stringify(orders))
}

type StoreContextValue = {
  cart: CartState
  addToCart: (product: Product, quantity?: number) => void
  removeFromCart: (productId: string) => void
  setCartQuantity: (productId: string, quantity: number) => void
  clearCart: () => void
  orders: Order[]
  addOrder: (order: Omit<Order, 'id' | 'createdAt'>) => Order
  updateOrderStatus: (orderId: string, status: Order['status']) => void
  getOrderById: (id: string) => Order | undefined
  products: Product[]
  refreshProducts: () => void
}

const StoreContext = createContext<StoreContextValue | null>(null)

export function StoreProvider({ children }: { children: React.ReactNode }) {
  const [cart, dispatchCart] = useReducer(cartReducer, { items: [] })
  const [orders, setOrders] = React.useState<Order[]>([])
  const [products, setProducts] = React.useState<Product[]>(initialProducts)

  const refreshProducts = useCallback(async () => {
    try {
      const list = await productApi.listProducts()
      const mapped: Product[] = (list || []).map((p: { id: number; name: string; description?: string; price: number; currency?: string; stock: number }) => ({
        id: String(p.id),
        name: p.name,
        description: p.description ?? '',
        price: Number(p.price),
        currency: p.currency ?? 'USD',
        stock: p.stock ?? 0,
      }))
      setApiProducts(mapped)
      setProducts(mapped)
    } catch (_) {
      clearApiProducts()
      setProducts(getProductsWithStock())
    }
  }, [])

  useEffect(() => {
    setOrders(loadOrders())
    setProducts(getProductsWithStock())
    refreshProducts()
  }, [refreshProducts])

  const addToCart = useCallback((product: Product, quantity?: number) => {
    dispatchCart({ type: 'ADD', product, quantity })
  }, [])

  const removeFromCart = useCallback((productId: string) => {
    dispatchCart({ type: 'REMOVE', productId })
  }, [])

  const setCartQuantity = useCallback((productId: string, quantity: number) => {
    dispatchCart({ type: 'SET_QUANTITY', productId, quantity })
  }, [])

  const clearCart = useCallback(() => {
    dispatchCart({ type: 'CLEAR' })
  }, [])

  const addOrder = useCallback(
    (order: Omit<Order, 'id' | 'createdAt'>): Order => {
      const id = `ord-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
      const newOrder: Order = {
        ...order,
        id,
        createdAt: new Date().toISOString(),
      }
      setOrders((prev) => {
        const next = [...prev, newOrder]
        saveOrders(next)
        return next
      })
      return newOrder
    },
    []
  )

  const updateOrderStatus = useCallback((orderId: string, status: Order['status']) => {
    setOrders((prev) => {
      const next = prev.map((o) => (o.id === orderId ? { ...o, status } : o))
      saveOrders(next)
      return next
    })
  }, [])

  const getOrderById = useCallback(
    (id: string) => orders.find((o) => o.id === id),
    [orders]
  )

  const value: StoreContextValue = {
    cart,
    addToCart,
    removeFromCart,
    setCartQuantity,
    clearCart,
    orders,
    addOrder,
    updateOrderStatus,
    getOrderById,
    products,
    refreshProducts,
  }

  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>
}

export function useStore() {
  const ctx = useContext(StoreContext)
  if (!ctx) throw new Error('useStore must be used within StoreProvider')
  return ctx
}

export function useCart() {
  const { cart, addToCart, removeFromCart, setCartQuantity, clearCart } = useStore()
  const totalItems = cart.items.reduce((s, i) => s + i.quantity, 0)
  const totalAmount = cart.items.reduce((s, i) => s + i.product.price * i.quantity, 0)
  return {
    items: cart.items,
    totalItems,
    totalAmount,
    addToCart,
    removeFromCart,
    setCartQuantity,
    clearCart,
  }
}

export function useOrders() {
  const { orders, addOrder, updateOrderStatus, getOrderById } = useStore()
  return { orders, addOrder, updateOrderStatus, getOrderById }
}
