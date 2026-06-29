export interface Producto {
  id: number;
  referencia: string;
  ean: string | null;
  nombre: string;
  descripcion: string | null;
  categoriaId: number;
  proveedorId: number | null;
  precioVenta: number;
  precioCoste: number | null;
  unidadMedida: string | null;
  stockActual: number;
  stockMinimo: number | null;
  stockMaximo: number | null;
  enAlerta: boolean;
  activo: boolean;
}

export interface ProductoRequest {
  referencia: string;
  ean?: string | null;
  nombre: string;
  descripcion?: string | null;
  categoriaId: number;
  proveedorId?: number | null;
  precioVenta: number;
  precioCoste?: number | null;
  stockMinimo?: number | null;
  stockMaximo?: number | null;
  unidadMedida?: string | null;
}

export interface ProductoPage {
  contenido: Producto[];
  pagina: number;
  tamano: number;
  totalPaginas: number;
  totalElementos: number;
}
