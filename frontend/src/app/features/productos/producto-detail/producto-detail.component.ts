// src/app/features/productos/producto-detail/producto-detail.component.ts
import { Component, OnInit, inject } from "@angular/core";
import { CommonModule } from "@angular/common";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { ProductoService } from "../producto.service";
import { Producto } from "../../../core/models/producto.model";
import { AuthService } from "../../../core/auth/auth.service";
import { CategoriaService } from "../../categorias/categoria.service";
import { ProveedorService } from "../../proveedores/proveedor.service";

@Component({
  selector: "app-producto-detail",
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="main-glass-workspace">
      <div class="detail-shell">
        <header class="topbar">
          <div>
            <h1>DETALLE DEL PRODUCTO</h1>
            <p>Información completa del producto</p>
          </div>
          <div class="actions">
            <a routerLink="/productos" class="btn-secondary">
              <i class="fas fa-arrow-left"></i> Volver
            </a>
            <a
              *ngIf="esAdmin"
              [routerLink]="['/productos', productoId, 'editar']"
              class="btn-primary"
            >
              <i class="fas fa-edit"></i> Editar
            </a>
          </div>
        </header>

        <div *ngIf="producto" class="detail-content">
          <div class="detail-grid">
            <div class="detail-group">
              <label>Referencia</label>
              <p>{{ producto.referencia }}</p>
            </div>
            <div class="detail-group">
              <label>EAN / Código de barras</label>
              <p>{{ producto.ean || "-" }}</p>
            </div>
            <div class="detail-group">
              <label>Nombre</label>
              <p>{{ producto.nombre }}</p>
            </div>
            <div class="detail-group">
              <label>Descripción</label>
              <p>{{ producto.descripcion || "-" }}</p>
            </div>
            <div class="detail-group">
              <label>Categoría</label>
              <p>{{ nombreCategoria || producto.categoriaId || "-" }}</p>
            </div>
            <div class="detail-group">
              <label>Proveedor</label>
              <p>{{ nombreProveedor || producto.proveedorId || "-" }}</p>
            </div>
            <div class="detail-group">
              <label>PVP (€)</label>
              <p class="price">{{ producto.precioVenta | currency: "EUR" }}</p>
            </div>
            <div class="detail-group" *ngIf="esAdmin">
              <label>Coste (€)</label>
              <p>{{ (producto.precioCoste | currency: "EUR") || "-" }}</p>
            </div>
            <div class="detail-group">
              <label>Stock actual</label>
              <p [class.low-stock]="producto.enAlerta">
                {{ producto.stockActual }}
              </p>
            </div>
            <div class="detail-group">
              <label>Stock mínimo</label>
              <p>{{ producto.stockMinimo || 0 }}</p>
            </div>
            <div class="detail-group">
              <label>Stock máximo</label>
              <p>{{ producto.stockMaximo || "Sin límite" }}</p>
            </div>
            <div class="detail-group">
              <label>Unidad de medida</label>
              <p>{{ producto.unidadMedida || "ud" }}</p>
            </div>
            <div class="detail-group">
              <label>Estado</label>
              <span
                [class]="producto.activo ? 'badge badge-ok' : 'badge badge-off'"
              >
                {{ producto.activo ? "ACTIVO" : "INACTIVO" }}
              </span>
            </div>
          </div>
        </div>

        <div *ngIf="!producto && !error" class="loading-state">
          <i class="fas fa-spinner fa-spin"></i>
          <p>Cargando producto...</p>
        </div>

        <div *ngIf="error" class="alert alert-error">
          <i class="fas fa-exclamation-triangle"></i> {{ error }}
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .main-glass-workspace {
        margin: 32px;
        padding: 48px;
        background: rgba(255, 255, 255, 0.06);
        border: 1px solid rgba(255, 255, 255, 0.18);
        border-radius: 24px;
        backdrop-filter: blur(32px);
        box-shadow: 0 30px 70px rgba(0, 0, 0, 0.35);
      }
      .detail-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 24px;
      }
      .detail-group {
        display: flex;
        flex-direction: column;
        gap: 6px;
      }
      .detail-group label {
        font-size: 0.7rem;
        font-weight: 700;
        text-transform: uppercase;
        color: rgba(255, 255, 255, 0.6);
      }
      .detail-group p {
        margin: 0;
        font-size: 1rem;
      }
      .price {
        color: var(--lime);
        font-weight: 700;
        font-size: 1.2rem;
      }
      .low-stock {
        color: #f97316;
        font-weight: 700;
      }
      .badge-ok {
        background: rgba(34, 197, 94, 0.15);
        color: #4ade80;
        padding: 4px 12px;
        border-radius: 20px;
        display: inline-block;
      }
      .badge-off {
        background: rgba(239, 68, 68, 0.15);
        color: #f87171;
        padding: 4px 12px;
        border-radius: 20px;
        display: inline-block;
      }
      @media (max-width: 700px) {
        .main-glass-workspace {
          margin: 16px;
          padding: 24px;
        }
        .detail-grid {
          grid-template-columns: 1fr;
          gap: 16px;
        }
      }
    `,
  ],
})
export class ProductoDetailComponent implements OnInit {
  private readonly svc = inject(ProductoService);
  private readonly route = inject(ActivatedRoute);
  private readonly categoriaSvc = inject(CategoriaService);
  private readonly proveedorSvc = inject(ProveedorService);
  readonly authSvc = inject(AuthService);

  producto: Producto | null = null;
  productoId: number | null = null;
  error: string | null = null;
  esAdmin = this.authSvc.isAdmin();
  
  nombreCategoria: string = '';
  nombreProveedor: string = '';

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get("id");
    if (id) {
      this.productoId = +id;
      this.cargarProducto();
    }
  }

  private cargarProducto(): void {
    if (!this.productoId) return;
    
    this.svc.obtener(this.productoId).subscribe({
      next: (producto) => {
        this.producto = producto;
        this.cargarNombres(producto);
      },
      error: (err) => {
        console.error(err);
        this.error = "No se pudo cargar el producto";
      },
    });
  }

  private cargarNombres(producto: Producto): void {
    // Cargar nombre de categoría
    if (producto.categoriaId) {
      this.categoriaSvc.obtener(producto.categoriaId).subscribe({
        next: (categoria) => {
          this.nombreCategoria = categoria.nombre;
        },
        error: () => {
          this.nombreCategoria = `ID: ${producto.categoriaId}`;
        },
      });
    }

    // Cargar nombre de proveedor
    if (producto.proveedorId) {
      this.proveedorSvc.obtener(producto.proveedorId).subscribe({
        next: (proveedor) => {
          this.nombreProveedor = proveedor.nombre;
        },
        error: () => {
          this.nombreProveedor = `ID: ${producto.proveedorId}`;
        },
      });
    }
  }
}