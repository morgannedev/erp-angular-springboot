import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth/auth.guard';
import { ClienteListComponent } from './cliente-list/cliente-list.component';
import { ClienteFormComponent } from './cliente-form/cliente-form.component';
import { ClienteDetailComponent } from './cliente-detail/cliente-detail.component';

export const clientesRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    component: ClienteListComponent
  },
  {
    path: 'nuevo',
    canActivate: [authGuard],
    component: ClienteFormComponent
  },
  {
    path: ':id',
    canActivate: [authGuard],
    component: ClienteDetailComponent
  },
  {
    path: ':id/editar',
    canActivate: [authGuard],
    component: ClienteFormComponent
  }
];