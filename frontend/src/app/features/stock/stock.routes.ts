import { Routes } from '@angular/router';

import { adminGuard } from '../../core/auth/admin.guard';
import { authGuard } from '../../core/auth/auth.guard';
import { StockAlertasComponent } from './stock-alertas/stock-alertas.component';
import { StockEntradaComponent } from './stock-entrada/stock-entrada.component';
import { StockHistorialComponent } from './stock-historial/stock-historial.component';
import { StockListComponent } from './stock-list/stock-list.component';
import { StockAjusteComponent } from './stock-ajuste/stock-ajuste.component';

export const stockRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard],  // Solo verifica autenticación, no roles
    children: [
      {
        path: '',
        component: StockListComponent
      },
      {
        path: 'alertas',
        component: StockAlertasComponent
      },
      {
        path: ':productoId/entrada',
        component: StockEntradaComponent,
        canActivate: [adminGuard]  // Solo ADMIN puede acceder
      },
      {
        path: ':productoId/ajuste',
        component: StockAjusteComponent,
        canActivate: [adminGuard]  // Solo ADMIN puede acceder
      },
      {
        path: ':productoId/movimientos',
        component: StockHistorialComponent
      }
    ]
  }
];