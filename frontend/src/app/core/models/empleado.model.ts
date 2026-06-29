export type RolEmpleado = 'ADMIN' | 'EMPLEADO';

export interface Empleado {
  id: number;
  usuarioId: number;
  username: string;
  rol: RolEmpleado;
  cuentaActiva: boolean;
  nombre: string;
  apellidos: string;
  dni?: string | null;
  telefono?: string | null;
  email?: string | null;
  cargo: string;
  salario?: number | null;
  fechaContratacion: string;
  fechaBaja?: string | null;
  notas?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface EmpleadoCreateRequest {
  username: string;
  password: string;
  rol: RolEmpleado;
  nombre: string;
  apellidos: string;
  dni?: string | null;
  telefono?: string | null;
  email?: string | null;
  cargo: string;
  salario?: number | null;
  fechaContratacion: string;
  notas?: string | null;
}

export interface EmpleadoUpdateRequest {
  nombre?: string;
  apellidos?: string;
  dni?: string | null;
  telefono?: string | null;
  email?: string | null;
  cargo?: string;
  salario?: number | null;
  fechaBaja?: string | null;
  notas?: string | null;
}

export interface EmpleadoPage {
  content?: Empleado[];
  contenido?: Empleado[];
  totalElements?: number;
  totalElementos?: number;
  size?: number;
  number?: number;
  pagina?: number;
}
