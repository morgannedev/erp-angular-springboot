// src/app/core/models/proveedor.model.ts
// Fuente: openapi.yaml — schemas Proveedor, ProveedorRequest, ProveedorResumen

export interface Proveedor {
  id: number;
  nombre: string;
  nif: string | null;
  contactoNombre: string | null;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
  ciudad: string | null;
  activo: boolean;
  notas: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProveedorRequest {
  nombre: string;            // required, maxLength 150
  nif?: string | null;       // maxLength 20
  contactoNombre?: string | null;
  telefono?: string | null;
  email?: string | null;
  direccion?: string | null;
  ciudad?: string | null;
  notas?: string | null;
}

/** Versión reducida usada en dropdowns de Productos */
export interface ProveedorResumen {
  id: number;
  nombre: string;
}

/** PageResponse del backend con clave "contenido" */
export interface ProveedorPage {
  contenido: Proveedor[];
  pagina: number;
  tamano: number;
  totalPaginas: number;
  totalElementos: number;
}
