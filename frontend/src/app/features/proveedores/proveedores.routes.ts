import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth/auth.guard';
import { ProveedoresListComponent } from './proveedores-list/proveedores-list.component';
import { ProveedorFormComponent } from './proveedor-form/proveedor-form.component';

export const proveedoresRoutes: Routes = [
  {
    path: '',  // Ruta: /proveedores
    canActivate: [authGuard],
    component: ProveedoresListComponent
  },
  {
    path: 'nuevo',
    canActivate: [authGuard],
    component: ProveedorFormComponent
  },
  {
    path: ':id/editar',
    canActivate: [authGuard],
    component: ProveedorFormComponent
  }
];