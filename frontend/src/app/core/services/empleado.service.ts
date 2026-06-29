import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Empleado,
  EmpleadoCreateRequest,
  EmpleadoPage,
  EmpleadoUpdateRequest,
  RolEmpleado
} from '../models/empleado.model';

@Injectable({
  providedIn: 'root'
})
export class EmpleadoService {
  private readonly baseUrl = `${environment.apiBaseUrl}/empleados`;

  constructor(private readonly http: HttpClient) {}

  listar(q?: string, activo?: boolean): Observable<EmpleadoPage> {
    let params = new HttpParams().set('size', 50);

    if (q?.trim()) {
      params = params.set('q', q.trim());
    }

    if (activo !== undefined) {
      params = params.set('activo', activo);
    }

    return this.http.get<EmpleadoPage>(this.baseUrl, { params });
  }

  obtener(id: number): Observable<Empleado> {
    return this.http.get<Empleado>(`${this.baseUrl}/${id}`);
  }

  crear(request: EmpleadoCreateRequest): Observable<Empleado> {
    return this.http.post<Empleado>(this.baseUrl, request);
  }

  actualizar(id: number, request: EmpleadoUpdateRequest): Observable<Empleado> {
    return this.http.put<Empleado>(`${this.baseUrl}/${id}`, request);
  }

  cambiarCuenta(id: number, activo: boolean): Observable<Empleado> {
    return this.http.patch<Empleado>(`${this.baseUrl}/${id}/cuenta`, { activo });
  }

  cambiarRol(id: number, rol: RolEmpleado): Observable<Empleado> {
    return this.http.patch<Empleado>(`${this.baseUrl}/${id}/rol`, { rol });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
