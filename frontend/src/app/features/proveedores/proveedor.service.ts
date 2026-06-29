// src/app/features/proveedores/proveedor.service.ts
// Fuente: openapi.yaml /proveedores/*
// README §8: debounce 300 ms en búsquedas en tiempo real
// README §7: EMPLEADO puede consultar; solo ADMIN puede crear/editar/eliminar

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Proveedor, ProveedorPage, ProveedorRequest, ProveedorResumen } from '../../core/models/proveedor.model';

@Injectable({ providedIn: 'root' })
export class ProveedorService {

  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/proveedores`;

  // ── búsqueda con debounce (README §8: 300 ms) ──────────────────
  private readonly busquedaTerm$ = new Subject<string>();

  /** Stream de resultados de búsqueda con debounce aplicado */
  readonly resultadosBusqueda$: Observable<ProveedorPage> =
    this.busquedaTerm$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(q => this.listar({ q, soloActivos: true }))
    );

  /** Emite un término de búsqueda — el componente llama esto en (input) */
  buscar(termino: string): void {
    this.busquedaTerm$.next(termino);
  }

  // ── CRUD ────────────────────────────────────────────────────────

  /** GET /proveedores — ADMIN + EMPLEADO */
  listar(params: {
    pagina?: number;
    tamanio?: number;
    q?: string;
    soloActivos?: boolean;
  } = {}): Observable<ProveedorPage> {
    let httpParams = new HttpParams();
    if (params.pagina   != null) httpParams = httpParams.set('page',   params.pagina);
    if (params.tamanio  != null) httpParams = httpParams.set('size',   params.tamanio);
    if (params.q        != null) httpParams = httpParams.set('q',      params.q);
    if (params.soloActivos != null) httpParams = httpParams.set('activo', params.soloActivos);
    return this.http.get<ProveedorPage>(this.base, { params: httpParams });
  }

  /** GET /proveedores/{id} — ADMIN + EMPLEADO */
  obtener(id: number): Observable<Proveedor> {
    return this.http.get<Proveedor>(`${this.base}/${id}`);
  }

  /** POST /proveedores — solo ADMIN (el backend devuelve 403 si no) */
  crear(dto: ProveedorRequest): Observable<Proveedor> {
    return this.http.post<Proveedor>(this.base, dto);
  }

  /** PUT /proveedores/{id} — solo ADMIN */
  actualizar(id: number, dto: ProveedorRequest): Observable<Proveedor> {
    return this.http.put<Proveedor>(`${this.base}/${id}`, dto);
  }

  /** PATCH /proveedores/{id}/estado — solo ADMIN */
  cambiarEstado(id: number, activo: boolean): Observable<Proveedor> {
    return this.http.patch<Proveedor>(`${this.base}/${id}/estado`, { activo });
  }

  /** DELETE /proveedores/{id} — solo ADMIN (soft-delete en backend) */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  /** Lista reducida para dropdowns en el formulario de Productos */
  listarResumen(): Observable<ProveedorResumen[]> {
    return this.http.get<ProveedorResumen[]>(`${this.base}/resumen`);
  }
}