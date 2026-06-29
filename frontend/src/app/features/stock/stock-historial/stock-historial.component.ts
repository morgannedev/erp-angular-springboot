import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { MovimientoStockPage } from '../../../core/models/stock.model';
import { StockService } from '../stock.service';

@Component({
  selector: 'app-stock-historial',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="page-header">
      <h1>Historial de movimientos</h1>
      <div class="header-actions">
        <a routerLink="/stock" class="btn">← Volver al stock</a>
        <a routerLink="/stock/alertas" class="btn">Alertas</a>
      </div>
    </div>

    <!-- Filtro por tipo -->
    <div class="filtro">
      <label>Filtrar por tipo:</label>
      <select [(ngModel)]="filtroTipo" (change)="cargar()" class="filter-select">
        <option value="">Todos los movimientos</option>
        <option value="ENTRADA">📥 Entrada</option>
        <option value="VENTA">💰 Venta</option>
        <option value="AJUSTE">✏️ Ajuste</option>
        <option value="ANULACION_VENTA">↩️ Anulación de venta</option>
      </select>
    </div>

    <table *ngIf="page; else cargando" class="historial-table">
      <thead>
        <tr>
          <th>Fecha</th>
          <th>Tipo</th>
          <th>Cantidad</th>
          <th>Stock resultante</th>
          <th>Motivo</th>
          <th>Albarán</th>
          <th>Empleado</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let mov of page.contenido" [class.cantidad-negativa]="mov.cantidad < 0">
          <td>{{ mov.fecha | date:'dd/MM/yyyy HH:mm' }}</td>
          <td>
            <span class="tipo-badge" [class]="mov.tipo">
              {{ getTipoIcon(mov.tipo) }} {{ mov.tipo }}
            </span>
          </td>
          <td [class.cantidad]="true" [class.positiva]="mov.cantidad > 0" [class.negativa]="mov.cantidad < 0">
            {{ mov.cantidad > 0 ? '+' : '' }}{{ mov.cantidad }}
          </td>
          <td><strong>{{ mov.stockResultante }}</strong></td>
          <td>{{ mov.motivo ?? '-' }}</td>
          <td>{{ mov.albaran ?? '-' }}</td>
          <td>{{ mov.empleadoNombre ?? 'Sistema' }}</td>
        </tr>
      </tbody>
    </table>

    <div *ngIf="page && page.totalPaginas > 1" class="paginacion">
      <button [disabled]="paginaActual === 0" (click)="irA(paginaActual - 1)">◀ Anterior</button>
      <span>Página {{ paginaActual + 1 }} de {{ page.totalPaginas }}</span>
      <button [disabled]="paginaActual >= page.totalPaginas - 1" (click)="irA(paginaActual + 1)">Siguiente ▶</button>
    </div>

    <p *ngIf="page && page.contenido.length === 0" class="empty-state">
      No hay movimientos para mostrar.
    </p>

    <ng-template #cargando><div class="loading">Cargando historial...</div></ng-template>
  `,
  styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
    }
    .header-actions {
      display: flex;
      gap: 0.5rem;
    }
    .filtro {
      margin-bottom: 1.5rem;
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .filter-select {
      padding: 0.5rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      min-width: 200px;
    }
    .historial-table {
      width: 100%;
      border-collapse: collapse;
    }
    .historial-table th, .historial-table td {
      padding: 0.75rem;
      text-align: left;
      border-bottom: 1px solid #ddd;
    }
    .historial-table th {
      background: #f5f5f5;
      font-weight: 600;
    }
    .cantidad-negativa {
      background-color: #fff3cd;
    }
    .tipo-badge {
      padding: 0.25rem 0.5rem;
      border-radius: 3px;
      font-size: 0.8rem;
    }
    .tipo-badge.ENTRADA { background: #d4edda; color: #155724; }
    .tipo-badge.VENTA { background: #cce5ff; color: #004085; }
    .tipo-badge.AJUSTE { background: #fff3cd; color: #856404; }
    .tipo-badge.ANULACION_VENTA { background: #f8d7da; color: #721c24; }
    .cantidad.positiva { color: #28a745; font-weight: bold; }
    .cantidad.negativa { color: #dc3545; font-weight: bold; }
    .paginacion {
      display: flex;
      justify-content: center;
      gap: 1rem;
      margin-top: 1.5rem;
      align-items: center;
    }
    .btn {
      padding: 0.5rem 1rem;
      text-decoration: none;
      border-radius: 4px;
      background: #6c757d;
      color: white;
      display: inline-block;
    }
    .empty-state, .loading {
      text-align: center;
      padding: 2rem;
      color: #666;
    }
  `]
})
export class StockHistorialComponent implements OnInit {
  private readonly stockSvc = inject(StockService);
  private readonly route = inject(ActivatedRoute);

  page: MovimientoStockPage | null = null;
  paginaActual = 0;
  filtroTipo = '';
  private productoId!: number;

  ngOnInit(): void {
    this.productoId = Number(this.route.snapshot.paramMap.get('productoId'));
    this.cargar();
  }

  cargar(): void {
    this.stockSvc.historial(this.productoId, {
      pagina: this.paginaActual,
      tamanio: 25,
      tipo: this.filtroTipo || undefined
    }).subscribe(page => this.page = page);
  }

  irA(pagina: number): void {
    this.paginaActual = pagina;
    this.cargar();
  }

  getTipoIcon(tipo: string): string {
    const icons: Record<string, string> = {
      'ENTRADA': '📥',
      'VENTA': '💰',
      'AJUSTE': '✏️',
      'ANULACION_VENTA': '↩️'
    };
    return icons[tipo] || '📋';
  }
}