import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';
import { CategoriaArbol } from '../../../core/models/categoria.model';
import { CategoriaService } from '../categoria.service';

@Component({
  selector: 'app-categoria-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './categoria-list.component.html',
  styleUrls: ['./categoria-list.component.css']
})
export class CategoriaListComponent implements OnInit {
  private readonly svc = inject(CategoriaService);
  private readonly authSvc = inject(AuthService);

  readonly esAdmin = this.authSvc.isAdmin();
  categorias: CategoriaArbol[] = [];

  ngOnInit(): void {
    this.cargar();
  }

  toggleEstado(id: number, activo: boolean): void {
    this.svc.cambiarEstado(id, !activo).subscribe({
      next: () => {
        this.cargar();
      },
      error: (err) => {
        console.error('Error al cambiar estado de categoría:', err);
        alert(err.error?.mensaje || 'No se pudo cambiar el estado de la categoría');
      }
    });
  }

  // ✅ Método para eliminar categoría
  eliminar(id: number, nombre: string): void {
    if (confirm(`¿Eliminar la categoría "${nombre}"? Se eliminarán también sus subcategorías.`)) {
      this.svc.eliminar(id).subscribe({
        next: () => {
          this.cargar();
        },
        error: (err) => {
          console.error('Error al eliminar categoría:', err);
          const msg = err.error?.mensaje || 'No se puede eliminar la categoría porque tiene productos asociados';
          alert(msg);
        }
      });
    }
  }

  private cargar(): void {
    this.svc.listarArbol(!this.esAdmin).subscribe({
      next: (categorias) => {
        this.categorias = categorias;
      },
      error: (err) => {
        console.error('Error al cargar categorías:', err);
        this.categorias = [];
      }
    });
  }
}