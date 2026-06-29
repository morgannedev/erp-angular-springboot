// app.routes.ts
import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { authRoutes } from './features/auth/auth.routes';
import { DashboardComponent } from './features/dashboard/dashboard.component';

export const routes: Routes = [
  ...authRoutes,
  {
    path: '',
    component: DashboardComponent,
    canActivate: [authGuard]
  },
  {
    path: 'clientes',
    loadChildren: () => import('./features/clientes/clientes.routes').then(m => m.clientesRoutes),
    canActivate: [authGuard]
  },
  {
    path: 'proveedores',
    loadChildren: () => import('./features/proveedores/proveedores.routes').then(m => m.proveedoresRoutes),
    canActivate: [authGuard]
  },
  {
    path: 'productos',
    loadChildren: () => import('./features/productos/productos.routes').then(m => m.productosRoutes),
    canActivate: [authGuard]
  },
  {
    path: 'ventas',
    loadChildren: () => import('./features/ventas/ventas.routes').then(m => m.ventasRoutes),
    canActivate: [authGuard]
  },
  {
    path: 'empleados',
    loadChildren: () => import('./features/empleados/empleados.routes').then(m => m.empleadosRoutes),
    canActivate: [authGuard]
  },
  {
    path: 'categorias',
    loadChildren: () => import('./features/categorias/categorias.routes').then(m => m.categoriasRoutes),
    canActivate: [authGuard]
  },
  {
    path: 'stock',
    loadChildren: () => import('./features/stock/stock.routes').then(m => m.stockRoutes),
    canActivate: [authGuard]
  },
  {
    path: '**',
    redirectTo: ''
  }
];