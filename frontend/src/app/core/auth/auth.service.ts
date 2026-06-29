import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { AuthSession } from '../models/auth-session.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly sessionKey = 'algedro.auth.session';
  
  private session: AuthSession | null = null;
  private expiresAtMs: number | null  = null;

  private authStatusSubject = new BehaviorSubject<boolean>(false);
  public  authStatus$       = this.authStatusSubject.asObservable();

  private isReadySubject = new BehaviorSubject<boolean>(false);
  public  isReady$       = this.isReadySubject.asObservable();

  constructor(
    private readonly http:    HttpClient,
    private readonly ngZone:  NgZone
  ) {
    this.loadStoredSession();
  }

  // ✅ Modificado: Añadir parámetro rememberMe
  login(username: string, password: string, rememberMe: boolean = false): Observable<AuthSession> {
    console.log('🔐 Login attempt, rememberMe:', rememberMe);
    return this.http
      .post<AuthSession>(`${environment.apiBaseUrl}/auth/login`, { username, password, rememberMe })
      .pipe(
        tap(session => {
          console.log('✅ Login success, saving session');
          this.setSession(session);
          this.isReadySubject.next(true);
        }),
        catchError(this.handleError)
      );
  }

  // ✅ Nuevo método para recuperación de contraseña
  requestPasswordReset(email: string): Observable<void> {
    console.log('📧 Requesting password reset for:', email);
    return this.http
      .post<void>(`${environment.apiBaseUrl}/auth/forgot-password`, { email })
      .pipe(
        catchError(this.handleError)
      );
  }

  logout(): Observable<void> {
    const refreshToken = this.session?.refreshToken;
    if (!refreshToken) {
      this.clearSession();
      return throwError(() => new Error('No session found'));
    }

    return this.http
      .post<void>(`${environment.apiBaseUrl}/auth/logout`, { refreshToken })
      .pipe(
        tap(() => this.clearSession()),
        catchError(error => {
          this.clearSession();
          return throwError(() => error);
        })
      );
  }

  getAccessToken(): string | null {
    if (this.isTokenExpired()) {
      console.log('⚠️ Token expired');
      return null;
    }
    return this.session?.token ?? null;
  }

  getRole(): AuthSession['rol'] | null { 
    return this.session?.rol ?? null; 
  }
  
  getUsername(): string | null { 
    return this.session?.username ?? null; 
  }
  
  getEmpleadoId(): number | null { 
    return this.session?.empleadoId ?? null; 
  }

  isAuthenticated(): boolean {
    return !!this.session && !!this.session.token && !this.isTokenExpired();
  }

  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  isTokenExpired(): boolean {
    if (this.expiresAtMs === null) return true;
    return Date.now() >= this.expiresAtMs - 10_000;
  }

  clearSession(): void {
    console.log('🧹 Clearing session');
    this.session     = null;
    this.expiresAtMs = null;

    localStorage.removeItem(this.sessionKey);
    this.authStatusSubject.next(false);
    this.isReadySubject.next(true);
  }

  private setSession(session: AuthSession): void {
    console.log('💾 Saving session for:', session.username);
    console.log('💾 Refresh token:', session.refreshToken);
    
    this.session     = session;
    this.expiresAtMs = this.calculateExpirationMs(session);

    localStorage.setItem(this.sessionKey, JSON.stringify({
      token: session.token,
      refreshToken: session.refreshToken,
      tipo: session.tipo,
      rol: session.rol,
      empleadoId: session.empleadoId,
      username: session.username,
      expiraEn: session.expiraEn,
      expiresAtMs: this.expiresAtMs
    }));

    this.authStatusSubject.next(true);
  }

  private loadStoredSession(): void {
    console.log('🔄 loadStoredSession() started');
    
    const stored = localStorage.getItem(this.sessionKey);
    
    if (!stored) {
      console.log('❌ No stored session found');
      this.isReadySubject.next(true);
      this.authStatusSubject.next(false);
      return;
    }

    try {
      const session = JSON.parse(stored);
      
      // Verificar si el token ya expiró
      if (session.expiresAtMs && Date.now() >= session.expiresAtMs - 10_000) {
        console.log('⏰ Stored token expired, clearing');
        this.clearSession();
        this.isReadySubject.next(true);
        this.authStatusSubject.next(false);
        return;
      }
      
      this.session = {
        token: session.token,
        refreshToken: session.refreshToken,
        tipo: session.tipo,
        rol: session.rol,
        empleadoId: session.empleadoId,
        username: session.username,
        expiraEn: session.expiraEn
      };
      this.expiresAtMs = session.expiresAtMs;
      
      console.log('✅ Session restored for:', this.session.username);
      this.authStatusSubject.next(true);
      this.isReadySubject.next(true);
      
    } catch (error) {
      console.error('❌ Error loading stored session:', error);
      this.clearSession();
      this.isReadySubject.next(true);
      this.authStatusSubject.next(false);
    }
  }

  private calculateExpirationMs(session: AuthSession): number {
    const jwtExp = this.getJwtExpirationMs(session.token);
    if (jwtExp) return jwtExp;
    return Date.now() + (session.expiraEn * 1000);
  }

  private getJwtExpirationMs(token: string): number | null {
    if (!token) return null;
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return null;
      const payload = JSON.parse(atob(parts[1]));
      return typeof payload.exp === 'number' ? payload.exp * 1000 : null;
    } catch {
      return null;
    }
  }

  private handleError(error: any): Observable<never> {
    console.error('AuthService error:', error);
    return throwError(() => error);
  }
}