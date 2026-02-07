// Merchant store: products (inventory) and types
// Product.id is string in UI; backend uses number (use String(id) when mapping from API)

export interface Product {
  id: string
  name: string
  description: string
  price: number
  currency: string
  stock: number
  image?: string
}

// When using order-service backend, products are set here so getProductsWithStock() returns them
let apiProducts: Product[] | null = null
export function setApiProducts(products: Product[]) {
  apiProducts = products
}
export function clearApiProducts() {
  apiProducts = null
}

export interface CartItem {
  product: Product
  quantity: number
}

export interface OrderItem {
  productId: string
  name: string
  price: number
  quantity: number
}

export interface Order {
  id: string
  items: OrderItem[]
  total: number
  currency: string
  status: 'pending' | 'paid' | 'shipped' | 'cancelled'
  paymentId?: number
  customerUserId?: number
  createdAt: string
}

// Default merchant ID for payment processing
export const MERCHANT_ID = 'MERCHANT_STORE_01'

// Sample inventory (products)
export const initialProducts: Product[] = [
  {
    id: 'prod-1',
    name: 'Wireless Earbuds',
    description: 'Noise-cancelling wireless earbuds with 24h battery',
    price: 49.99,
    currency: 'USD',
    stock: 50,
  },
  {
    id: 'prod-2',
    name: 'USB-C Hub',
    description: '7-in-1 USB-C hub with HDMI and SD card reader',
    price: 39.99,
    currency: 'USD',
    stock: 30,
  },
  {
    id: 'prod-3',
    name: 'Mechanical Keyboard',
    description: 'RGB mechanical keyboard with Cherry MX switches',
    price: 89.99,
    currency: 'USD',
    stock: 20,
  },
  {
    id: 'prod-4',
    name: 'Monitor Stand',
    description: 'Adjustable aluminum monitor stand',
    price: 29.99,
    currency: 'USD',
    stock: 45,
  },
  {
    id: 'prod-5',
    name: 'Webcam HD',
    description: '1080p webcam with built-in microphone',
    price: 59.99,
    currency: 'USD',
    stock: 25,
  },
  {
    id: 'prod-6',
    name: 'Laptop Sleeve',
    description: 'Water-resistant laptop sleeve for 13-15" laptops',
    price: 24.99,
    currency: 'USD',
    stock: 60,
  },
]

// In-memory inventory (decremented on order; for demo we can reset or persist in localStorage)
const inventoryKey = 'merchant-inventory'
function loadInventory(): Record<string, number> {
  if (typeof window === 'undefined') return {}
  try {
    const s = localStorage.getItem(inventoryKey)
    if (s) return JSON.parse(s)
  } catch (_) {}
  const initial: Record<string, number> = {}
  initialProducts.forEach((p) => (initial[p.id] = p.stock))
  return initial
}

function saveInventory(inv: Record<string, number>) {
  if (typeof window === 'undefined') return
  localStorage.setItem(inventoryKey, JSON.stringify(inv))
}

export function getProductStock(productId: string): number {
  const inv = loadInventory()
  return inv[productId] ?? initialProducts.find((p) => p.id === productId)?.stock ?? 0
}

export function setProductStock(productId: string, quantity: number) {
  const inv = loadInventory()
  inv[productId] = Math.max(0, quantity)
  saveInventory(inv)
}

export function decreaseStock(productId: string, by: number): boolean {
  const inv = loadInventory()
  const current = inv[productId] ?? initialProducts.find((p) => p.id === productId)?.stock ?? 0
  if (current < by) return false
  inv[productId] = current - by
  saveInventory(inv)
  return true
}

export function getProductsWithStock(): Product[] {
  if (apiProducts != null) return apiProducts
  const inv = loadInventory()
  return initialProducts.map((p) => ({
    ...p,
    stock: inv[p.id] ?? p.stock,
  }))
}

export function resetInventory() {
  if (typeof window === 'undefined') return
  localStorage.removeItem(inventoryKey)
}
