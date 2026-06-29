import { Routes } from '@angular/router';
import { adminGuard } from '../../core/auth/admin.guard';
import { authGuard } from '../../core/auth/auth.guard';
import { CategoriaFormComponent } from './categoria-form/categoria-form.component';
import { CategoriaListComponent } from './categoria-list/categoria-list.component';

export const categoriasRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        component: CategoriaListComponent
      },
      {
        path: 'nuevo',
        component: CategoriaFormComponent,
        canActivate: [adminGuard]
      },
      {
        path: ':id/editar',
        component: CategoriaFormComponent,
        canActivate: [adminGuard]
      }
    ]
  }
];