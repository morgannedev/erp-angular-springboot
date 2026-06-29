import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  MovimientoStock,
  MovimientoStockPage,
  StockEntradaRequest,
  StockNivelPage,
  StockAjusteRequest,
  ProductoStockInfo
} from '../../core/models/stock.model';

@Injectable({ providedIn: 'root' })
export class StockService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/stock`;
  private readonly productosBase = `${environment.apiBaseUrl}/productos`;

  listar(params: {
    pagina?: number;
    tamanio?: number;
    soloAlertas?: boolean;
    productoId?: number;
    proveedorId?: number;
    query?: string;  // ← NUEVO
  } = {}): Observable<StockNivelPage> {
    let httpParams = new HttpParams();
    if (params.pagina != null) httpParams = httpParams.set('page', params.pagina);
    if (params.tamanio != null) httpParams = httpParams.set('size', params.tamanio);
    if (params.soloAlertas != null) httpParams = httpParams.set('soloAlertas', params.soloAlertas);
    if (params.productoId != null) httpParams = httpParams.set('productoId', params.productoId);
    if (params.proveedorId != null) httpParams = httpParams.set('proveedorId', params.proveedorId);
    if (params.query) httpParams = httpParams.set('query', params.query);  // ← NUEVO
    return this.http.get<StockNivelPage>(this.base, { params: httpParams });
  }

  registrarEntrada(productoId: number, request: StockEntradaRequest): Observable<MovimientoStock> {
    return this.http.post<MovimientoStock>(`${this.base}/${productoId}/entradas`, request);
  }

  // NUEVO: Registrar ajuste
  registrarAjuste(productoId: number, request: StockAjusteRequest): Observable<MovimientoStock> {
    return this.http.post<MovimientoStock>(`${this.base}/${productoId}/ajustes`, request);
  }

  historial(productoId: number, params: {
    pagina?: number;
    tamanio?: number;
    tipo?: string;
  } = {}): Observable<MovimientoStockPage> {
    let httpParams = new HttpParams();
    if (params.pagina != null) httpParams = httpParams.set('page', params.pagina);
    if (params.tamanio != null) httpParams = httpParams.set('size', params.tamanio);
    if (params.tipo) httpParams = httpParams.set('tipo', params.tipo);
    return this.http.get<MovimientoStockPage>(`${this.base}/${productoId}/movimientos`, { params: httpParams });
  }

  // NUEVO: Actualizar stock mínimo
  actualizarMinimo(productoId: number, nuevoMinimo: number): Observable<void> {
    return this.http.patch<void>(`${this.base}/${productoId}/minimo`, nuevoMinimo);
  }

  // NUEVO: Actualizar stock máximo
  actualizarMaximo(productoId: number, nuevoMaximo: number): Observable<void> {
    return this.http.patch<void>(`${this.base}/${productoId}/maximo`, nuevoMaximo);
  }

  // NUEVO: Obtener información de un producto
  obtenerProducto(productoId: number): Observable<ProductoStockInfo> {
    return this.http.get<ProductoStockInfo>(`${this.productosBase}/${productoId}`);
  }
}