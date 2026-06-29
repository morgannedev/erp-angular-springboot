import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { VentaDetailResponse, VentaPage, VentaRequest } from '../../core/models/venta.model';

@Injectable({ providedIn: 'root' })
export class VentaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/ventas`;

  crear(request: VentaRequest): Observable<VentaDetailResponse> {
    return this.http.post<VentaDetailResponse>(this.base, request);
  }

  listar(params: {
    desde?: string;
    hasta?: string;
    empleadoId?: number;
    clienteId?: number;
    metodoPago?: string;
    estado?: string;
    page?: number;
    size?: number;
  } = {}): Observable<VentaPage> {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });
    return this.http.get<VentaPage>(this.base, { params: httpParams });
  }

  exportarCsv(params: { desde?: string; hasta?: string } = {}): Observable<Blob> {
    let httpParams = new HttpParams().set('exportar', 'true');
    if (params.desde) httpParams = httpParams.set('desde', params.desde);
    if (params.hasta) httpParams = httpParams.set('hasta', params.hasta);
    return this.http.get(this.base, { params: httpParams, responseType: 'blob' });
  }

  anular(id: number, motivoAnulacion: string): Observable<VentaDetailResponse> {
    return this.http.post<VentaDetailResponse>(`${this.base}/${id}/anular`, { motivoAnulacion });
  }
}
