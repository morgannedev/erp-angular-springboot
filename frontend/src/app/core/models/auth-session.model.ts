export interface AuthSession {
  token: string;
  refreshToken: string;  // ✅ AÑADIR esta línea
  tipo: string;
  rol: 'ADMIN' | 'EMPLEADO';
  empleadoId: number;
  username: string;
  expiraEn: number;  // Segundos hasta expiración del access token
}

export interface RefreshTokenResponse {
  token: string;
  refreshToken: string;
  expiraEn: number;
}