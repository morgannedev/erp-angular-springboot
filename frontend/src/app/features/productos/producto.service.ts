import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import {
  Observable,
  Subject,
  debounceTime,
  distinctUntilChanged,
  switchMap,
} from "rxjs";

import { environment } from "../../../environments/environment";
import {
  Producto,
  ProductoPage,
  ProductoRequest,
} from "../../core/models/producto.model";

@Injectable({ providedIn: "root" })
export class ProductoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/productos`;
  private readonly busquedaTerm$ = new Subject<string>();

  readonly resultadosBusqueda$: Observable<ProductoPage> =
    this.busquedaTerm$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((q) => this.listar({ q, activo: true })),
    );

  buscar(termino: string): void {
    this.busquedaTerm$.next(termino);
  }

  listar(
    params: {
      pagina?: number;
      tamanio?: number;
      q?: string;
      categoriaId?: number;
      proveedorId?: number;
      activo?: boolean;
      sort?: string; // ← Añadir sort
    } = {},
  ): Observable<ProductoPage> {
    let httpParams = new HttpParams();
    if (params.pagina != null)
      httpParams = httpParams.set("page", params.pagina);
    if (params.tamanio != null)
      httpParams = httpParams.set("size", params.tamanio);
    if (params.q) httpParams = httpParams.set("q", params.q);
    if (params.categoriaId != null)
      httpParams = httpParams.set("categoriaId", params.categoriaId);
    if (params.proveedorId != null)
      httpParams = httpParams.set("proveedorId", params.proveedorId);
    if (params.activo != null)
      httpParams = httpParams.set("activo", params.activo);
    if (params.sort) httpParams = httpParams.set("sort", params.sort); // ← Añadir sort
    return this.http.get<ProductoPage>(this.base, { params: httpParams });
  }

  obtener(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${this.base}/${id}`);
  }

  buscarPorBarcode(ean: string): Observable<Producto> {
    return this.http.get<Producto>(
      `${this.base}/barcode/${encodeURIComponent(ean)}`,
    );
  }

  crear(dto: ProductoRequest): Observable<Producto> {
    return this.http.post<Producto>(this.base, dto);
  }

  actualizar(id: number, dto: ProductoRequest): Observable<Producto> {
    return this.http.put<Producto>(`${this.base}/${id}`, dto);
  }

  cambiarEstado(id: number, activo: boolean): Observable<Producto> {
    return this.http.patch<Producto>(`${this.base}/${id}/estado`, { activo });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
