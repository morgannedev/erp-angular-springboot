import { Routes } from '@angular/router';
import { adminGuard } from '../../core/auth/admin.guard';
import { authGuard } from '../../core/auth/auth.guard';
import { ProductoFormComponent } from './producto-form/producto-form.component';
import { ProductoListComponent } from './producto-list/producto-list.component';
import { ProductoDetailComponent } from './producto-detail/producto-detail.component';

export const productosRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        component: ProductoListComponent
      },
      {
        path: 'nuevo',
        component: ProductoFormComponent,
        canActivate: [adminGuard]
      },
      {
        path: ':id',
        component: ProductoDetailComponent,
        canActivate: [authGuard]
      },
      {
        path: ':id/editar',
        component: ProductoFormComponent,
        canActivate: [adminGuard]
      }
    ]
  }
];