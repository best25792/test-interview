import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap, catchError, of } from 'rxjs';
import { environment } from '../../environments/environment';

const REFRESH_KEY = 'auth_refresh';
const USER_ID_KEY = 'auth_user_id';

interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private accessToken = signal<string | null>(null);
  private refreshToken = signal<string | null>(null);
  private userId = signal<number | null>(null);

  isLoggedIn$ = computed(() => !!this.accessToken() && !!this.refreshToken());
  userId$ = computed(() => this.userId());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.loadFromStorage();
  }

  private loadFromStorage(): void {
    try {
      const refresh = sessionStorage.getItem(REFRESH_KEY);
      const userIdStr = sessionStorage.getItem(USER_ID_KEY);
      if (refresh) {
        this.refreshToken.set(refresh);
        const id = userIdStr ? parseInt(userIdStr, 10) : null;
        this.userId.set(Number.isNaN(id as number) ? null : id);
        this.refreshAccessToken().subscribe();
      }
    } catch {
      // ignore
    }
  }

  private decodeUserId(token: string): number | null {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const sub = payload.sub;
      if (sub == null) return null;
      const id = typeof sub === 'number' ? sub : parseInt(String(sub), 10);
      return Number.isNaN(id) ? null : id;
    } catch {
      return null;
    }
  }

  private persist(access: string, refresh: string, id: number | null): void {
    try {
      sessionStorage.setItem(REFRESH_KEY, refresh);
      if (id != null) sessionStorage.setItem(USER_ID_KEY, String(id));
    } catch {
      // ignore
    }
  }

  requestOtp(phoneNumber: string, channel: 'SMS' | 'EMAIL' = 'SMS') {
    return this.http.post<{ message: string }>(
      `${environment.userServiceUrl}/auth/login/request-otp`,
      { phoneNumber, channel }
    );
  }

  verifyOtp(phoneNumber: string, code: string, deviceId?: string) {
    return this.http
      .post<TokenResponse>(`${environment.userServiceUrl}/auth/login/verify-otp`, {
        phoneNumber,
        code,
        deviceId,
      })
      .pipe(
        tap((res) => {
          const id = this.decodeUserId(res.accessToken);
          this.accessToken.set(res.accessToken);
          this.refreshToken.set(res.refreshToken);
          this.userId.set(id);
          this.persist(res.accessToken, res.refreshToken, id);
        })
      );
  }

  refreshAccessToken() {
    const refresh = this.refreshToken();
    if (!refresh) return of(null);
    return this.http
      .post<TokenResponse>(`${environment.userServiceUrl}/auth/token/refresh`, {
        refreshToken: refresh,
      })
      .pipe(
        tap((res) => {
          const id = this.decodeUserId(res.accessToken);
          this.accessToken.set(res.accessToken);
          this.refreshToken.set(res.refreshToken);
          this.userId.set(id);
          this.persist(res.accessToken, res.refreshToken, id);
        }),
        catchError(() => {
          this.clearTokens();
          return of(null);
        })
      );
  }

  getAccessToken(): string | null {
    return this.accessToken();
  }

  getRefreshToken(): string | null {
    return this.refreshToken();
  }

  getUserId(): number | null {
    return this.userId();
  }

  logout(): void {
    const refresh = this.refreshToken();
    if (refresh) {
      this.http
        .post(`${environment.userServiceUrl}/auth/logout`, { refreshToken: refresh })
        .subscribe({ error: () => {} });
    }
    this.clearTokens();
    this.router.navigate(['/']);
  }

  clearTokens(): void {
    this.accessToken.set(null);
    this.refreshToken.set(null);
    this.userId.set(null);
    try {
      sessionStorage.removeItem(REFRESH_KEY);
      sessionStorage.removeItem(USER_ID_KEY);
    } catch {
      // ignore
    }
  }
}
