// src/app/features/proveedores/proveedores-list/proveedores-list.component.ts
// ADMIN: ve botones Crear / Editar / Activar·Desactivar
// EMPLEADO: solo lectura (el backend también bloquea en 403)

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormControl } from '@angular/forms';

import { ProveedorService } from '../proveedor.service';
import { Proveedor, ProveedorPage } from '../../../core/models/proveedor.model';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-proveedores-list',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './proveedores-list.component.html',
  styleUrls: ['./proveedores-list.component.css']
})
export class ProveedoresListComponent implements OnInit {
  private readonly svc = inject(ProveedorService);
  private readonly authSvc = inject(AuthService);

  readonly esAdmin = this.authSvc.isAdmin();
  readonly busquedaCtrl = new FormControl('');

  page: ProveedorPage | null = null;
  paginaActual = 0;
  private terminoBusqueda = '';

  ngOnInit(): void {
    // Suscripción al stream de búsqueda con debounce del service
    this.svc.resultadosBusqueda$.subscribe({
      next: (page) => {
        this.page = page;
        this.paginaActual = 0;
      },
      error: (err) => console.error('Error en búsqueda:', err)
    });
    this.cargar();
  }

  onBuscar(event: Event): void {
    const termino = (event.target as HTMLInputElement).value;
    this.terminoBusqueda = termino;
    this.paginaActual = 0;
    this.svc.buscar(termino); // el debounce vive en el service
  }

  irA(pagina: number): void {
    this.paginaActual = pagina;
    this.cargar();
  }

  toggleEstado(proveedor: Proveedor): void {
    this.svc.cambiarEstado(proveedor.id, !proveedor.activo).subscribe({
      next: () => this.cargar(),
      error: (err) => console.error('Error al cambiar estado:', err)
    });
  }

  private cargar(): void {
    this.svc.listar({
      pagina: this.paginaActual,
      tamanio: 25, // README §8: paginación por defecto 25 elementos
      q: this.terminoBusqueda || undefined
    }).subscribe({
      next: (page) => this.page = page,
      error: (err) => console.error('Error al cargar proveedores:', err)
    });
  }
}