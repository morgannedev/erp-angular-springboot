export interface StockNivel {
  productoId: number;
  referencia: string;
  nombre: string;
  stockActual: number;
  stockMinimo: number;
  stockMaximo: number | null;
  enAlerta: boolean;
  proveedorId: number | null;
  proveedorNombre: string | null;
}

export interface StockNivelPage {
  contenido: StockNivel[];
  pagina: number;
  tamano: number;
  totalPaginas: number;
  totalElementos: number;
}

export interface StockEntradaRequest {
  cantidad: number;
  proveedorId?: number | null;
  albaran?: string | null;
}

export interface StockAjusteRequest {
  cantidad: number;
  motivo: string;
  forzarNegativo?: boolean;
}

export interface MovimientoStock {
  id: number;
  productoId: number;
  productoNombre: string;
  tipo: 'ENTRADA' | 'VENTA' | 'AJUSTE' | 'ANULACION_VENTA' | 'INVENTARIO';
  cantidad: number;
  stockResultante: number;
  motivo: string | null;
  ventaId: number | null;
  proveedorId: number | null;
  albaran: string | null;
  empleadoId: number | null;
  empleadoNombre: string | null;
  fecha: string;
}

export interface MovimientoStockPage {
  contenido: MovimientoStock[];
  pagina: number;
  tamano: number;
  totalPaginas: number;
  totalElementos: number;
}

export interface ProductoStockInfo {
  id: number;
  nombre: string;
  referencia: string;
  stockActual: number;
  stockMinimo: number;
  stockMaximo: number;
}
