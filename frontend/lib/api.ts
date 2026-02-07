import axios from 'axios'

// Microservice base URLs
const USER_SERVICE_URL = process.env.NEXT_PUBLIC_USER_SERVICE_URL || 'http://localhost:8081/api/v1'
const WALLET_SERVICE_URL = process.env.NEXT_PUBLIC_WALLET_SERVICE_URL || 'http://localhost:8082/api/v1'
const PAYMENT_SERVICE_URL = process.env.NEXT_PUBLIC_PAYMENT_SERVICE_URL || 'http://localhost:8083/api/v1'
const QR_SERVICE_URL = process.env.NEXT_PUBLIC_QR_SERVICE_URL || 'http://localhost:8084/api/v1'
const ORDER_SERVICE_URL = process.env.NEXT_PUBLIC_ORDER_SERVICE_URL || 'http://localhost:8085/api/v1'

// Create separate axios instances for each service
const userApiClient = axios.create({
  baseURL: USER_SERVICE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

const walletApiClient = axios.create({
  baseURL: WALLET_SERVICE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

const paymentApiClient = axios.create({
  baseURL: PAYMENT_SERVICE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

const qrApiClient = axios.create({
  baseURL: QR_SERVICE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

const orderApiClient = axios.create({
  baseURL: ORDER_SERVICE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// User API (User Service - Port 8081)
export const userApi = {
  createUser: async (data: {
    username: string
    email: string
    phoneNumber: string
    isActive?: boolean
    isVerified?: boolean
  }) => {
    const response = await userApiClient.post('/users', data)
    return response.data
  },

  getUser: async (id: number) => {
    const response = await userApiClient.get(`/users/${id}`)
    return response.data
  },

  getWalletBalance: async (id: number) => {
    const response = await userApiClient.get(`/users/${id}/wallet/balance`)
    return response.data
  },

  getAvailableBalance: async (id: number) => {
    // Available balance is from Wallet Service, but accessed via User Service
    // User Service proxies to Wallet Service
    const response = await walletApiClient.get(`/holds/users/${id}/available-balance`)
    return response.data
  },

  getWalletDetails: async (id: number) => {
    const response = await userApiClient.get(`/users/${id}/wallet`)
    return response.data
  },

  topUpWallet: async (id: number, amount: number) => {
    const response = await userApiClient.post(`/users/${id}/wallet/topup`, { amount })
    return response.data
  },
}

// Payment API (Payment Service - Port 8083)
export const paymentApi = {
  initiatePayment: async (userId: number) => {
    const response = await paymentApiClient.post('/payments/initiate', { userId })
    return response.data
  },

  getPayment: async (id: number) => {
    const response = await paymentApiClient.get(`/payments/${id}`)
    return response.data
  },

  getPaymentStatus: async (id: number) => {
    const response = await paymentApiClient.get(`/payments/${id}/status`)
    return response.data
  },

  getAllPayments: async (status?: string) => {
    const url = status ? `/payments?status=${status}` : '/payments'
    const response = await paymentApiClient.get(url)
    return response.data
  },

  processPayment: async (
    id: number,
    data: {
      qrCode: string
      amount: number
      currency: string
      merchantId: string
      description?: string
    }
  ) => {
    const response = await paymentApiClient.post(`/payments/${id}/process`, data)
    return response.data
  },

  confirmPayment: async (id: number) => {
    const response = await paymentApiClient.post(`/payments/${id}/confirm`)
    return response.data
  },

  cancelPayment: async (id: number) => {
    const response = await paymentApiClient.post(`/payments/${id}/cancel`)
    return response.data
  },

  refundPayment: async (id: number, amount?: number) => {
    const body = amount ? { amount } : {}
    const response = await paymentApiClient.post(`/payments/${id}/refund`, body)
    return response.data
  },
}

// QR Code API (QR Service - Port 8084)
export const qrCodeApi = {
  getQRCode: async (id: number) => {
    const response = await qrApiClient.get(`/qrcodes/${id}`)
    return response.data
  },

  validateQRCode: async (code: string) => {
    const response = await qrApiClient.post('/qrcodes/validate', { code })
    return response.data
  },
}

// Order & Inventory API (Order Service - Port 8085)
export const productApi = {
  listProducts: async () => {
    const response = await orderApiClient.get('/products')
    return response.data
  },
  getProduct: async (id: number) => {
    const response = await orderApiClient.get(`/products/${id}`)
    return response.data
  },
  updateStock: async (id: number, stock: number) => {
    const response = await orderApiClient.patch(`/products/${id}/stock`, { stock })
    return response.data
  },
}

export const orderApi = {
  listOrders: async (params?: { status?: string; merchantId?: string }) => {
    const search = new URLSearchParams()
    if (params?.status) search.append('status', params.status)
    if (params?.merchantId) search.append('merchantId', params.merchantId)
    const url = search.toString() ? `/orders?${search.toString()}` : '/orders'
    const response = await orderApiClient.get(url)
    return response.data
  },
  getOrder: async (id: number) => {
    const response = await orderApiClient.get(`/orders/${id}`)
    return response.data
  },
  createOrder: async (data: {
    merchantId?: string
    customerUserId: number
    paymentId?: number
    items: { productId: number; quantity: number }[]
  }) => {
    const response = await orderApiClient.post('/orders', data)
    return response.data
  },
  updateOrderStatus: async (id: number, status: string) => {
    const response = await orderApiClient.patch(`/orders/${id}/status`, { status })
    return response.data
  },
}

// Transaction API (Payment Service - Port 8083, same as Payment API)
export const transactionApi = {
  getTransaction: async (id: number) => {
    const response = await paymentApiClient.get(`/transactions/${id}`)
    return response.data
  },

  getAllTransactions: async (filters?: { paymentId?: number; type?: string }) => {
    const params = new URLSearchParams()
    if (filters?.paymentId) params.append('paymentId', filters.paymentId.toString())
    if (filters?.type) params.append('type', filters.type)
    const url = params.toString() ? `/transactions?${params.toString()}` : '/transactions'
    const response = await paymentApiClient.get(url)
    return response.data
  },
}
