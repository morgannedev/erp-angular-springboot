import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Cliente, ClientePage, ClienteRequest } from '../../core/models/cliente.model';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/clientes`;
  private readonly busquedaTerm$ = new Subject<string>();

  readonly resultadosBusqueda$: Observable<ClientePage> = this.busquedaTerm$.pipe(
    debounceTime(300),
    distinctUntilChanged(),
    switchMap(q => this.listar({ q, activo: true }))
  );

  buscar(termino: string): void {
    this.busquedaTerm$.next(termino);
  }

  listar(params: { 
    pagina?: number; 
    tamanio?: number; 
    q?: string; 
    activo?: boolean;
    sort?: string;
  } = {}): Observable<ClientePage> {
    let httpParams = new HttpParams();
    if (params.pagina != null) httpParams = httpParams.set('page', params.pagina);
    if (params.tamanio != null) httpParams = httpParams.set('size', params.tamanio);
    if (params.q) httpParams = httpParams.set('q', params.q);
    if (params.activo != null) httpParams = httpParams.set('activo', params.activo);
    if (params.sort) httpParams = httpParams.set('sort', params.sort);
    return this.http.get<ClientePage>(this.base, { params: httpParams });
  }

  obtener(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.base}/${id}`);
  }

  crear(request: ClienteRequest): Observable<Cliente> {
    return this.http.post<Cliente>(this.base, request);
  }

  actualizar(id: number, request: ClienteRequest): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.base}/${id}`, request);
  }

  crearRapido(request: ClienteRequest): Observable<Cliente> {
    return this.http.post<Cliente>(`${this.base}/pos`, request);
  }

  cambiarEstado(id: number, activo: boolean): Observable<Cliente> {
    return this.http.patch<Cliente>(`${this.base}/${id}/estado`, { activo });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  exportarCsv(q?: string, activo?: boolean): Observable<Blob> {
    let params = new HttpParams();
    if (q) params = params.set('q', q);
    if (activo !== undefined) params = params.set('activo', activo);
    return this.http.get(`${this.base}/export/csv`, { params, responseType: 'blob' });
  }
}