import { Injectable, Injector } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private readonly injector: Injector) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // ✅ Excluir explícitamente las peticiones de auth para evitar dependencia circular
    const isAuthRequest = request.url.includes('/auth/login') || 
                          request.url.includes('/auth/refresh') || 
                          request.url.includes('/auth/logout');
    
    if (isAuthRequest) {
      console.log('🚫 Interceptor skipping auth request:', request.url);
      return next.handle(request);
    }

    // Obtener AuthService solo para peticiones no-auth
    const authService = this.injector.get(AuthService);
    const token = authService.getAccessToken();
    
    let authRequest = request;
    if (token) {
      authRequest = this.addTokenToRequest(request, token);
    }

    return next.handle(authRequest).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          console.log('⚠️ 401 error, redirecting to login');
          const router = this.injector.get(Router);
          authService.clearSession();
          router.navigate(['/login'], { queryParams: { sessionExpired: 'true' } });
        }
        return throwError(() => error);
      })
    );
  }

  private addTokenToRequest(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }
}