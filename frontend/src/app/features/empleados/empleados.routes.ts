import { Routes } from '@angular/router';
import { adminGuard } from '../../core/auth/admin.guard';
import { EmpleadoFormComponent } from './form/empleado-form.component';
import { EmpleadoListComponent } from './list/empleado-list.component';

export const empleadosRoutes: Routes = [
  {
    path: '',
    canActivate: [adminGuard],
    children: [
      {
        path: '',
        component: EmpleadoListComponent
      },
      {
        path: 'nuevo',
        component: EmpleadoFormComponent
      },
      {
        path: ':id/editar',
        component: EmpleadoFormComponent
      }
    ]
  }
];