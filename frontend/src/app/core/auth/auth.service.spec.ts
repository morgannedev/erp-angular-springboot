import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { AuthSession } from '../models/auth-session.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const mockSession: AuthSession = {
    token: 'jwt-token',
    refreshToken: 'refresh-uuid-123',
    tipo: 'Bearer',
    rol: 'ADMIN',
    empleadoId: 1,
    username: 'admin',
    expiraEn: 28800
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deberia autenticar y guardar el token', () => {
    service.login('admin', 'admin123').subscribe((response) => {
      expect(response.token).toBe('jwt-token');
      expect(response.refreshToken).toBe('refresh-uuid-123');
      expect(service.getRole()).toBe('ADMIN');
      expect(service.isAuthenticated()).toBeTrue();
    });

    const request = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    expect(request.request.method).toBe('POST');
    request.flush(mockSession);
  });

  it('deberia cerrar sesion y limpiar', () => {
    // Primero establecer sesión manualmente
    service['setSession'](mockSession);
    
    expect(service.isAuthenticated()).toBeTrue();

    service.logout().subscribe(() => {
      expect(service.getAccessToken()).toBeNull();
      expect(service.isAuthenticated()).toBeFalse();
    });

    const request = httpMock.expectOne(`${environment.apiBaseUrl}/auth/logout`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ refreshToken: 'refresh-uuid-123' });
    request.flush({});
  });
});