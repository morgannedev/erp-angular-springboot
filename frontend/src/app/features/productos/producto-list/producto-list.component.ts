import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormControl, FormsModule } from '@angular/forms';

import { AuthService } from '../../../core/auth/auth.service';
import { Producto, ProductoPage } from '../../../core/models/producto.model';
import { CategoriaResumen } from '../../../core/models/categoria.model';
import { ProveedorResumen } from '../../../core/models/proveedor.model';
import { ProductoService } from '../producto.service';
import { CategoriaService } from '../../categorias/categoria.service';
import { ProveedorService } from '../../proveedores/proveedor.service';

@Component({
  selector: 'app-producto-list',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, FormsModule],
  templateUrl: './producto-list.component.html',
  styleUrls: ['./producto-list.component.css']
})
export class ProductoListComponent implements OnInit {
  private readonly svc = inject(ProductoService);
  private readonly authSvc = inject(AuthService);
  private readonly categoriaSvc = inject(CategoriaService);
  private readonly proveedorSvc = inject(ProveedorService);

  readonly esAdmin = this.authSvc.isAdmin();
  readonly busquedaCtrl = new FormControl('');

  page: ProductoPage | null = null;
  paginaActual = 0;
  private terminoBusqueda = '';

  // Filtros
  categorias: CategoriaResumen[] = [];
  proveedores: ProveedorResumen[] = [];
  filtroCategoriaId: number | null = null;
  filtroProveedorId: number | null = null;

  // Ordenamiento
  sortField: string = 'id';
  sortDir: 'asc' | 'desc' = 'asc';

  ngOnInit(): void {
    this.svc.resultadosBusqueda$.subscribe(p => {
      this.page = p;
      this.paginaActual = 0;
    });
    this.cargarFiltros();
    this.cargar();
  }

  cargarFiltros(): void {
    this.categoriaSvc.listarRaices().subscribe({
      next: (categorias) => this.categorias = categorias,
      error: (err) => console.error('Error loading categories:', err)
    });
    this.proveedorSvc.listarResumen().subscribe({
      next: (proveedores) => this.proveedores = proveedores,
      error: (err) => console.error('Error loading suppliers:', err)
    });
  }

  onBuscar(event: Event): void {
    this.terminoBusqueda = (event.target as HTMLInputElement).value;
    this.paginaActual = 0;
    this.svc.buscar(this.terminoBusqueda);
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

  toggleEstado(producto: Producto): void {
    this.svc.cambiarEstado(producto.id, !producto.activo).subscribe({
      next: () => this.cargar(),
      error: (err) => console.error('Error toggling state:', err)
    });
  }

  eliminar(producto: Producto): void {
    if (confirm(`¿Eliminar el producto "${producto.nombre}"? Esta acción no se puede deshacer.`)) {
      this.svc.eliminar(producto.id).subscribe({
        next: () => this.cargar(),
        error: (err) => {
          const msg = err.error?.mensaje || 'No se puede eliminar el producto porque tiene movimientos asociados';
          alert(msg);
          console.error('Error deleting product:', err);
        }
      });
    }
  }

  private cargar(): void {
    const filtrarActivos = !this.esAdmin;
    
    // Construir parámetros de ordenamiento
    let sortParam = '';
    if (this.sortField === 'referencia') sortParam = 'referencia';
    else if (this.sortField === 'nombre') sortParam = 'nombre';
    else if (this.sortField === 'precioVenta') sortParam = 'precioVenta';
    else if (this.sortField === 'stockActual') sortParam = 'stockActual';
    else sortParam = 'id';
    
    sortParam = `${sortParam},${this.sortDir}`;
    
    this.svc.listar({
      pagina: this.paginaActual,
      tamanio: 25,
      q: this.terminoBusqueda || undefined,
      categoriaId: this.filtroCategoriaId || undefined,
      proveedorId: this.filtroProveedorId || undefined,
      activo: filtrarActivos ? true : undefined
    }).subscribe({
      next: (page) => this.page = page,
      error: (err) => console.error('Error loading products:', err)
    });
  }
}