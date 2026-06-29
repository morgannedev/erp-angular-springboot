import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { filter, take, switchMap, of } from 'rxjs';

import { AuthService } from './auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router      = inject(Router);

  // ✅ Esperar a que la inicialización termine
  return authService.isReady$.pipe(
    filter(ready => ready === true),
    take(1),
    switchMap(() => {
      if (!authService.isAuthenticated()) {
        console.log('🔐 Admin guard: no autenticado');
        return of(router.createUrlTree(['/login']));
      }
      
      if (authService.isAdmin()) {
        console.log('✅ Admin guard: usuario admin autorizado');
        return of(true);
      }
      
      console.warn('⚠️ Admin guard: usuario no admin, acceso denegado');
      return of(router.createUrlTree(['/unauthorized']));
    })
  );
};