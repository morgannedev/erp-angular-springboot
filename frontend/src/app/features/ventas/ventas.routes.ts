// src/app/features/ventas/ventas.routes.ts
import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth/auth.guard';
import { PosComponent } from './pos/pos.component';
import { HistorialVentasComponent } from './historial-ventas/historial-ventas.component';

export const ventasRoutes: Routes = [
  {
    path: '',
    redirectTo: 'pos',
    pathMatch: 'full'
  },
  {
    path: 'pos',
    canActivate: [authGuard],
    component: PosComponent
  },
  {
    path: 'historial',
    canActivate: [authGuard],
    component: HistorialVentasComponent
  }
];