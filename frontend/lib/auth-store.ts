/**
 * Plain module to hold tokens so axios interceptors can read/update without React.
 * AuthProvider syncs state with this store and subscribes to changes.
 */

const REFRESH_KEY = 'auth_refresh'
const USER_ID_KEY = 'auth_user_id'
const ROLES_KEY = 'auth_roles'

type Listener = () => void
const listeners: Listener[] = []

let accessToken: string | null = null
let refreshToken: string | null = null
let userId: number | null = null
let roles: string[] = []

function decodePayload(token: string): { sub?: string; roles?: string[] } | null {
  try {
    return JSON.parse(atob(token.split('.')[1])) as { sub?: string; roles?: string[] }
  } catch {
    return null
  }
}

function decodeUserIdFromJwt(token: string): number | null {
  const payload = decodePayload(token)
  if (payload?.sub == null) return null
  const id = parseInt(String(payload.sub), 10)
  return Number.isNaN(id) ? null : id
}

function decodeRolesFromJwt(token: string): string[] {
  const payload = decodePayload(token)
  const r = payload?.roles
  if (Array.isArray(r)) return r.filter((x): x is string => typeof x === 'string')
  return []
}

function notify() {
  listeners.forEach((cb) => cb())
}

export const authStore = {
  getAccessToken: () => accessToken,
  getRefreshToken: () => refreshToken,
  getUserId: () => userId,
  getRoles: () => roles,
  isAuthenticated: () => !!accessToken && !!refreshToken,

  setTokens(access: string, refresh: string, id: number | null = null) {
    accessToken = access
    refreshToken = refresh
    userId = id ?? decodeUserIdFromJwt(access)
    roles = decodeRolesFromJwt(access)
    try {
      sessionStorage.setItem(REFRESH_KEY, refresh)
      if (id != null) sessionStorage.setItem(USER_ID_KEY, String(id))
      sessionStorage.setItem(ROLES_KEY, JSON.stringify(roles))
    } catch {
      // ignore
    }
    notify()
  },

  clearTokens() {
    accessToken = null
    refreshToken = null
    userId = null
    roles = []
    try {
      sessionStorage.removeItem(REFRESH_KEY)
      sessionStorage.removeItem(USER_ID_KEY)
      sessionStorage.removeItem(ROLES_KEY)
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

  getStoredRoles(): string[] {
    try {
      const s = sessionStorage.getItem(ROLES_KEY)
      if (!s) return []
      const parsed = JSON.parse(s)
      return Array.isArray(parsed) ? parsed.filter((x: unknown): x is string => typeof x === 'string') : []
    } catch {
      return []
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
