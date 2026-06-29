import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { filter, take, switchMap, of } from 'rxjs';

import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router      = inject(Router);

  return authService.isReady$.pipe(
    filter(ready => ready === true),
    take(1),
    switchMap(() => {
      if (authService.isAuthenticated()) {
        console.log('✅ Auth guard: usuario autenticado');
        return of(true);
      }
      console.log('🔐 Auth guard: no autenticado, redirigiendo a login');
      return of(router.createUrlTree(['/login']));
    })
  );
};