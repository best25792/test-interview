import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.getAccessToken();

  let reqWithAuth = req;
  if (token) {
    reqWithAuth = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(reqWithAuth).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        const refresh = auth.getRefreshToken();
        if (refresh) {
          return auth.refreshAccessToken().pipe(
            switchMap(() => {
              const newToken = auth.getAccessToken();
              if (!newToken) {
                auth.clearTokens();
                router.navigate(['/login']);
                return throwError(() => err);
              }
              const retry = req.clone({
                setHeaders: { Authorization: `Bearer ${newToken}` },
              });
              return next(retry);
            }),
            catchError(() => {
              auth.clearTokens();
              router.navigate(['/login']);
              return throwError(() => err);
            })
          );
        }
        auth.clearTokens();
        router.navigate(['/login']);
      }
      return throwError(() => err);
    })
  );
};
