import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';
import { Cliente, ClientePage } from '../../../core/models/cliente.model';
import { ClienteQuickCreateComponent } from '../cliente-quick-create/cliente-quick-create.component';
import { ClienteService } from '../cliente.service';

@Component({
  selector: 'app-cliente-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink, ClienteQuickCreateComponent],
  templateUrl: './cliente-list.component.html',
  styleUrls: ['./cliente-list.component.css'] 
})
export class ClienteListComponent implements OnInit {
  private readonly clienteSvc = inject(ClienteService);
  private readonly authSvc = inject(AuthService);
  private readonly router = inject(Router);

  readonly esAdmin = this.authSvc.isAdmin();
  readonly busquedaCtrl = new FormControl('');

  page: ClientePage | null = null;
  paginaActual = 0;
  mostrarRapido = false;
  private terminoBusqueda = '';

  // Filtros
  filtroActivo: boolean | null = null;

  // Ordenamiento
  sortField: string = 'nombre';
  sortDir: 'asc' | 'desc' = 'asc';

  ngOnInit(): void {
    this.clienteSvc.resultadosBusqueda$.subscribe({
      next: (page) => {
        this.page = page;
        this.paginaActual = 0;
      },
      error: (err) => console.error('Error en búsqueda:', err)
    });
    this.cargar();
  }

  onBuscar(event: Event): void {
    this.terminoBusqueda = (event.target as HTMLInputElement).value;
    this.paginaActual = 0;
    this.clienteSvc.buscar(this.terminoBusqueda);
  }

  aplicarFiltros(): void {
    this.paginaActual = 0;
    this.cargar();
  }

  ordenarPor(campo: string): void {
    if (this.sortField === campo) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = campo;
      this.sortDir = 'asc';
    }
    this.cargar();
  }

  irA(pagina: number): void {
    this.paginaActual = pagina;
    this.cargar();
  }

  onCreado(): void {
    this.mostrarRapido = false;
    this.cargar();
  }

  toggleEstado(id: number, activo: boolean): void {
    this.clienteSvc.cambiarEstado(id, !activo).subscribe({
      next: () => this.cargar(),
      error: (err) => console.error('Error al cambiar estado:', err)
    });
  }

  eliminar(cliente: Cliente): void {
    if (confirm(`¿Eliminar el cliente "${cliente.nombre} ${cliente.apellidos || ''}"? Esta acción no se puede deshacer.`)) {
      this.clienteSvc.eliminar(cliente.id).subscribe({
        next: () => this.cargar(),
        error: (err) => {
          if (err.status === 409 || err.error?.mensaje?.includes('ventas')) {
            alert('No se puede eliminar el cliente porque tiene ventas asociadas. Desactívelo en su lugar.');
          } else {
            alert(err.error?.mensaje || 'No se pudo eliminar el cliente');
          }
          console.error('Error deleting client:', err);
        }
      });
    }
  }

  exportarCsv(): void {
    // ✅ Corregido: Convertir null a undefined correctamente
    let activoParam: boolean | undefined;
    if (this.filtroActivo !== null) {
      activoParam = this.filtroActivo;
    } else if (!this.esAdmin) {
      activoParam = true;
    } else {
      activoParam = undefined;
    }
    
    this.clienteSvc.exportarCsv(this.terminoBusqueda, activoParam).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `clientes_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.csv`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error exporting CSV:', err);
        alert('No se pudo exportar el listado');
      }
    });
  }

  verDetalle(id: number): void {
    this.router.navigate(['/clientes', id]);
  }

  private cargar(): void {
    // ✅ Corregido: Convertir null a undefined correctamente
    let activoParam: boolean | undefined;
    if (this.esAdmin) {
      activoParam = this.filtroActivo !== null ? this.filtroActivo : undefined;
    } else {
      activoParam = true;
    }
    
    this.clienteSvc.listar({
      pagina: this.paginaActual,
      tamanio: 25,
      q: this.terminoBusqueda || undefined,
      activo: activoParam,
      sort: `${this.sortField},${this.sortDir}`
    }).subscribe({
      next: (page) => this.page = page,
      error: (err) => console.error('Error al cargar clientes:', err)
    });
  }
}