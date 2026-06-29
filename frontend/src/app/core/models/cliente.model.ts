export interface Cliente {
  id: number;
  nombre: string;
  apellidos: string | null;
  telefono: string;
  email: string | null;
  nif: string | null;
  direccion: string | null;
  ciudad: string | null;
  notas: string | null;
  activo: boolean;
}

export interface ClienteRequest {
  nombre: string;
  apellidos?: string | null;
  telefono: string;
  email?: string | null;
  nif?: string | null;
  direccion?: string | null;
  ciudad?: string | null;
  notas?: string | null;
}

export interface ClientePage {
  contenido: Cliente[];
  pagina: number;
  tamano: number;
  totalPaginas: number;
  totalElementos: number;
}
