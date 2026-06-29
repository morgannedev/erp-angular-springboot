export interface VentaLineRequest {
  productoId: number;
  cantidad: number;
  descuentoLinea?: number | null;
}

export interface VentaRequest {
  lineas: VentaLineRequest[];
  clienteId?: number | null;
  descuentoGlobal?: number | null;
  metodoPago: 'EFECTIVO' | 'TARJETA' | 'OTRO';
  notas?: string | null;
  forzarSinStock?: boolean;
}

export interface VentaLineResponse {
  id: number;
  productoId: number;
  nombreProducto: string;
  cantidad: number;
  precioUnitario: number;
  descuentoLinea: number;
  subtotalLinea: number;
}

export interface VentaDetailResponse {
  id: number;
  numeroVenta: string;
  empleadoId: number;
  empleadoNombre?: string;
  clienteId: number | null;
  fecha: string;
  subtotal: number;
  descuentoGlobal: number;
  total: number;
  metodoPago: string;
  estado: string;
  motivoAnulacion: string | null;
  fechaAnulacion: string | null;
  notas: string | null;
  lineas: VentaLineResponse[];
  urlTicket: string;
}

export interface VentaPage {
  content: VentaDetailResponse[];
  number: number;
  size: number;
  totalPages: number;
  totalElements: number;
}
