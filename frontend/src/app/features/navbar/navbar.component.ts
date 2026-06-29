import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { filter, map } from 'rxjs/operators';

/** Mapa de rutas a título legible para el breadcrumb del header */
const ROUTE_TITLES: Record<string, string> = {
  '/':            'DASHBOARD',
  '/ventas':      'VENTAS',
  '/compras':     'COMPRAS',
  '/productos':   'PRODUCTOS',
  '/categorias':  'CATEGORÍAS',
  '/clientes':    'CLIENTES',
  '/proveedores': 'PROVEEDORES',
  '/empleados':   'EMPLEADOS',
  '/operaciones': 'OPERACIONES',
  '/stock':       'STOCK',
  '/pos':         'PUNTO DE VENTA',
};

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent {
  private readonly authService = inject(AuthService);
  private readonly router      = inject(Router);

  /** Título del módulo activo, actualizado en cada navegación */
  currentTitle = 'DASHBOARD';

  get username(): string { return this.authService.getUsername() ?? ''; }
  get role(): string     { return this.authService.getRole() ?? ''; }

  constructor() {
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      map((e: any) => {
        // Busca coincidencia exacta primero, luego por prefijo
        const url = (e as NavigationEnd).urlAfterRedirects.split('?')[0];
        if (ROUTE_TITLES[url]) return ROUTE_TITLES[url];
        const prefix = Object.keys(ROUTE_TITLES).find(k => k !== '/' && url.startsWith(k));
        return prefix ? ROUTE_TITLES[prefix] : 'Algedro ERP';
      })
    ).subscribe(title => this.currentTitle = title);
  }

  logout(): void {
    this.authService.logout().subscribe({
      next:  () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login'])
    });
  }
}