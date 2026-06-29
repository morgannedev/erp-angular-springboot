// src/app/features/clientes/cliente-detail/cliente-detail.component.ts
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../../core/auth/auth.service';
import { Cliente } from '../../../core/models/cliente.model';
import { VentaDetailResponse } from '../../../core/models/venta.model';
import { ClienteService } from '../cliente.service';
import { VentaService } from '../../ventas/venta.service';

@Component({
  selector: 'app-cliente-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="main-glass-workspace">
      <div class="detail-shell">
        <header class="topbar">
          <div>
            <h1>FICHA DEL CLIENTE</h1>
            <p>Información completa y historial de compras</p>
          </div>
          <div class="actions">
            <a routerLink="/clientes" class="btn-secondary">
              <i class="fas fa-arrow-left"></i> Volver
            </a>
            <a *ngIf="esAdmin" [routerLink]="['/clientes', clienteId, 'editar']" class="btn-primary">
              <i class="fas fa-edit"></i> Editar
            </a>
          </div>
        </header>

        <!-- Datos del cliente -->
        <div *ngIf="cliente" class="detail-section">
          <div class="section-header">
            <i class="fas fa-user-circle"></i>
            <h2>Datos personales</h2>
          </div>
          <div class="detail-grid">
            <div class="detail-group">
              <label>Nombre completo</label>
              <p><strong>{{ cliente.nombre }} {{ cliente.apellidos || '' }}</strong></p>
            </div>
            <div class="detail-group">
              <label>Teléfono</label>
              <p><i class="fas fa-phone-alt"></i> {{ cliente.telefono }}</p>
            </div>
            <div class="detail-group">
              <label>Email</label>
              <p><i class="fas fa-envelope"></i> {{ cliente.email || '-' }}</p>
            </div>
            <div class="detail-group" *ngIf="esAdmin">
              <label>NIF / CIF</label>
              <p>{{ cliente.nif || '-' }}</p>
            </div>
            <div class="detail-group">
              <label>Dirección</label>
              <p><i class="fas fa-map-marker-alt"></i> {{ cliente.direccion || '-' }}</p>
            </div>
            <div class="detail-group">
              <label>Ciudad</label>
              <p><i class="fas fa-city"></i> {{ cliente.ciudad || '-' }}</p>
            </div>
            <div class="detail-group">
              <label>Notas</label>
              <p>{{ cliente.notas || '-' }}</p>
            </div>
            <div class="detail-group">
              <label>Estado</label>
              <span [class]="cliente.activo ? 'badge badge-ok' : 'badge badge-off'">
                {{ cliente.activo ? 'ACTIVO' : 'INACTIVO' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Historial de compras -->
        <div *ngIf="historial.length > 0" class="detail-section">
          <div class="section-header">
            <i class="fas fa-shopping-cart"></i>
            <h2>Historial de compras</h2>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Nº Venta</th>
                  <th>Productos</th>
                  <th>Total</th>
                  <th>Empleado</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let venta of historial">
                  <td>{{ venta.fecha | date:'dd/MM/yyyy HH:mm' }}</td>
                  <td>
                    <a [routerLink]="['/ventas/historial']" class="venta-link">
                      {{ venta.numeroVenta }}
                    </a>
                  </td>
                  <td>
                    <div *ngFor="let linea of venta.lineas" class="producto-linea">
                      {{ linea.nombreProducto }} x{{ linea.cantidad }}
                    </div>
                  </td>
                  <td class="total-cell">{{ venta.total | currency:'EUR' }}</td>
                  <td>{{ venta.empleadoNombre || 'Empleado #' + venta.empleadoId }}</td>
                  <td>
                    <span class="status-badge" [class.completed]="venta.estado === 'COMPLETADA'"
                          [class.cancelled]="venta.estado === 'ANULADA'">
                      {{ venta.estado === 'COMPLETADA' ? 'COMPLETADA' : 'ANULADA' }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div *ngIf="cliente && historial.length === 0" class="empty-state">
          <i class="fas fa-receipt"></i>
          <p>Este cliente no tiene compras registradas</p>
        </div>

        <div *ngIf="!cliente && !error" class="loading-state">
          <i class="fas fa-spinner fa-spin"></i>
          <p>Cargando información...</p>
        </div>

        <div *ngIf="error" class="alert alert-error">
          <i class="fas fa-exclamation-triangle"></i> {{ error }}
        </div>
      </div>
    </div>
  `,
  styles: [`
    .main-glass-workspace {
      margin: 32px;
      padding: 48px;
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid rgba(255, 255, 255, 0.18);
      border-radius: 24px;
      backdrop-filter: blur(32px);
      box-shadow: 0 30px 70px rgba(0, 0, 0, 0.35);
      display: flex;
      flex-direction: column;
      gap: 32px;
    }
    .detail-shell { min-height: auto; padding: 0; background: transparent; }
    .topbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; }
    .topbar h1 { margin: 0; font-size: 1.8rem; font-weight: 700; background: linear-gradient(135deg, #fff, var(--lime)); -webkit-background-clip: text; background-clip: text; color: transparent; }
    .topbar p { margin: 4px 0 0; color: rgba(255, 255, 255, 0.8); font-size: 0.85rem; }
    .actions { display: flex; gap: 12px; }
    .btn-primary, .btn-secondary { padding: 10px 20px; border-radius: 12px; font-weight: 700; font-size: 0.9rem; cursor: pointer; transition: all 0.2s; text-decoration: none; display: inline-flex; align-items: center; gap: 8px; }
    .btn-primary { background: linear-gradient(135deg, var(--lime), #9ee91d); border: none; color: #071006; }
    .btn-primary:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(198, 241, 53, 0.3); }
    .btn-secondary { background: rgba(255, 255, 255, 0.08); border: 1px solid rgba(255, 255, 255, 0.2); color: var(--text); }
    .btn-secondary:hover { background: rgba(255, 255, 255, 0.15); border-color: var(--lime); transform: translateY(-1px); }
    .detail-section { background: rgba(255, 255, 255, 0.03); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 16px; padding: 24px; }
    .section-header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid rgba(255, 255, 255, 0.1); }
    .section-header i { font-size: 1.3rem; color: var(--lime); }
    .section-header h2 { margin: 0; font-size: 1.1rem; font-weight: 700; color: rgba(255, 255, 255, 0.9); }
    .detail-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
    .detail-group { display: flex; flex-direction: column; gap: 6px; }
    .detail-group label { font-size: 0.7rem; font-weight: 700; text-transform: uppercase; color: rgba(255, 255, 255, 0.6); }
    .detail-group p { margin: 0; font-size: 0.95rem; display: flex; align-items: center; gap: 8px; }
    .table-wrap { overflow-x: auto; background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 20px; }
    table { width: 100%; border-collapse: collapse; }
    th { text-align: left; padding: 14px 16px; background: rgba(0, 0, 0, 0.2); color: rgba(255, 255, 255, 0.9); font-weight: 700; font-size: 0.75rem; text-transform: uppercase; }
    td { padding: 12px 16px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); vertical-align: middle; }
    .producto-linea { font-size: 0.8rem; padding: 2px 0; }
    .total-cell { font-weight: 700; color: var(--lime); }
    .venta-link { color: var(--lime); text-decoration: none; }
    .venta-link:hover { text-decoration: underline; }
    .status-badge { padding: 4px 10px; border-radius: 20px; font-size: 0.7rem; font-weight: 600; }
    .status-badge.completed { background: rgba(34, 197, 94, 0.15); color: #4ade80; border: 1px solid rgba(74, 222, 128, 0.3); }
    .status-badge.cancelled { background: rgba(239, 68, 68, 0.15); color: #f87171; border: 1px solid rgba(248, 113, 113, 0.3); }
    .badge { display: inline-flex; align-items: center; justify-content: center; min-width: 80px; padding: 4px 12px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; }
    .badge-ok { background: rgba(34, 197, 94, 0.15); color: #4ade80; border: 1px solid rgba(74, 222, 128, 0.3); }
    .badge-off { background: rgba(239, 68, 68, 0.15); color: #f87171; border: 1px solid rgba(248, 113, 113, 0.3); }
    .empty-state, .loading-state { text-align: center; padding: 60px; background: rgba(255, 255, 255, 0.04); border-radius: 20px; }
    .empty-state i, .loading-state i { font-size: 3rem; color: var(--lime); opacity: 0.5; margin-bottom: 16px; }
    .alert { margin-top: 20px; padding: 16px; border-radius: 12px; display: flex; align-items: center; gap: 12px; }
    .alert-error { background: rgba(248, 113, 113, 0.1); border: 1px solid rgba(248, 113, 113, 0.3); color: #f87171; }
    @media (max-width: 820px) {
      .main-glass-workspace { margin: 16px; padding: 24px; }
      .detail-grid { grid-template-columns: 1fr; gap: 16px; }
      .topbar { flex-direction: column; align-items: flex-start; }
    }
  `]
})
export class ClienteDetailComponent implements OnInit {
  private readonly clienteSvc = inject(ClienteService);
  private readonly ventaSvc = inject(VentaService);
  private readonly route = inject(ActivatedRoute);
  readonly authSvc = inject(AuthService);

  clienteId: number | null = null;
  cliente: Cliente | null = null;
  historial: VentaDetailResponse[] = [];
  error: string | null = null;
  esAdmin = this.authSvc.isAdmin();

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.clienteId = +id;
      this.cargarCliente();
      this.cargarHistorial();
    }
  }

  cargarCliente(): void {
    if (!this.clienteId) return;
    this.clienteSvc.obtener(this.clienteId).subscribe({
      next: (cliente) => this.cliente = cliente,
      error: (err) => {
        console.error(err);
        this.error = 'No se pudo cargar el cliente';
      }
    });
  }

  cargarHistorial(): void {
    if (!this.clienteId) return;
    this.ventaSvc.listar({ clienteId: this.clienteId, size: 50 }).subscribe({
      next: (page) => this.historial = page.content || [],
      error: (err) => console.error('Error cargando historial:', err)
    });
  }
}