/**
 * Plain module to hold tokens so axios interceptors can read/update without React.
 * AuthProvider syncs state with this store and subscribes to changes.
 */

const REFRESH_KEY = 'auth_refresh'
const USER_ID_KEY = 'auth_user_id'

type Listener = () => void
const listeners: Listener[] = []

let accessToken: string | null = null
let refreshToken: string | null = null
let userId: number | null = null

function decodeUserIdFromJwt(token: string): number | null {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const sub = payload.sub
    if (sub == null) return null
    const id = typeof sub === 'number' ? sub : parseInt(String(sub), 10)
    return Number.isNaN(id) ? null : id
  } catch {
    return null
  }
}

function notify() {
  listeners.forEach((cb) => cb())
}

export const authStore = {
  getAccessToken: () => accessToken,
  getRefreshToken: () => refreshToken,
  getUserId: () => userId,
  isAuthenticated: () => !!accessToken && !!refreshToken,

  setTokens(access: string, refresh: string, id: number | null = null) {
    accessToken = access
    refreshToken = refresh
    userId = id ?? decodeUserIdFromJwt(access)
    try {
      sessionStorage.setItem(REFRESH_KEY, refresh)
      if (id != null) sessionStorage.setItem(USER_ID_KEY, String(id))
    } catch {
      // ignore
    }
    notify()
  },

  clearTokens() {
    accessToken = null
    refreshToken = null
    userId = null
    try {
      sessionStorage.removeItem(REFRESH_KEY)
      sessionStorage.removeItem(USER_ID_KEY)
    } catch {
      // ignore
    }
    notify()
  },

  getStoredRefreshToken(): string | null {
    try {
      return sessionStorage.getItem(REFRESH_KEY)
    } catch {
      return null
    }
  },

  getStoredUserId(): number | null {
    try {
      const s = sessionStorage.getItem(USER_ID_KEY)
      return s ? parseInt(s, 10) : null
    } catch {
      return null
    }
  },

  subscribe(cb: Listener) {
    listeners.push(cb)
    return () => {
      const i = listeners.indexOf(cb)
      if (i >= 0) listeners.splice(i, 1)
    }
  },
}
