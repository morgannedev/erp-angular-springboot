// src/app/features/categorias/categoria.service.ts
// Fuente: openapi.yaml /categorias/*
// Data-Model.md §1.4: máximo 2 niveles (raíz + subcategoría)
// README §7: EMPLEADO puede consultar; solo ADMIN puede crear/editar/eliminar

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Categoria,
  CategoriaArbol,
  CategoriaRequest,
  CategoriaResumen
} from '../../core/models/categoria.model';

@Injectable({ providedIn: 'root' })
export class CategoriaService {

  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/categorias`;

  // ── Lectura ─────────────────────────────────────────────────────

  /**
   * GET /categorias — ADMIN + EMPLEADO
   * Devuelve árbol de dos niveles: raíces con sus subcategorías anidadas.
   */
  listarArbol(soloActivas = true): Observable<CategoriaArbol[]> {
    const params = new HttpParams().set('soloActivas', soloActivas);
    return this.http.get<CategoriaArbol[]>(this.base, { params });
  }

  /** GET /categorias/{id} — ADMIN + EMPLEADO */
  obtener(id: number): Observable<Categoria> {
    return this.http.get<Categoria>(`${this.base}/${id}`);
  }

  /** Lista plana de raíces para el selector de padre en el formulario */
  listarRaices(): Observable<CategoriaResumen[]> {
    return this.http.get<CategoriaResumen[]>(`${this.base}/resumen`);
  }

  // ── Escritura (solo ADMIN — el backend devuelve 403 si no) ──────

  /**
   * POST /categorias — solo ADMIN
   * Si dto.padreId === null → crea raíz.
   * Si dto.padreId tiene valor → crea subcategoría (nivel 1).
   * El backend rechaza nivel 2 con 409.
   */
  crear(dto: CategoriaRequest): Observable<Categoria> {
    return this.http.post<Categoria>(this.base, dto);
  }

  /** PUT /categorias/{id} — solo ADMIN */
  actualizar(id: number, dto: CategoriaRequest): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.base}/${id}`, dto);
  }

  /** PATCH /categorias/{id}/estado — solo ADMIN */
  cambiarEstado(id: number, activo: boolean): Observable<Categoria> {
    return this.http.patch<Categoria>(`${this.base}/${id}/estado`, { activo });
  }

  /** DELETE /categorias/{id} — solo ADMIN (bloqueado si tiene productos) */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}