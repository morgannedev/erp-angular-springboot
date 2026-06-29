// src/app/core/models/categoria.model.ts
// Fuente: openapi.yaml — schemas Categoria, CategoriaArbol, CategoriaRequest

export interface Categoria {
  id: number;
  nombre: string;
  descripcion: string | null;
  padreId: number | null;       // null = raíz (nivel 0)
  padreNombre: string | null;
  activo: boolean;
  createdAt: string;
}

/** Árbol de dos niveles devuelto por GET /categorias */
export interface CategoriaArbol {
  id: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
  subcategorias: Categoria[];   // máximo nivel 1
}

export interface CategoriaRequest {
  nombre: string;               // required, maxLength 100
  descripcion?: string | null;  // maxLength 255
  padreId?: number | null;      // null = raíz; valor = subcategoría (máx. nivel 1)
}

/** Versión reducida usada en dropdowns de Productos */
export interface CategoriaResumen {
  id: number;
  nombre: string;
}