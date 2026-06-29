import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { AuthService } from './core/auth/auth.service';
import { NavbarComponent } from './features/navbar/navbar.component'; // Ajusta tus rutas de importación si difieren
import { SidebarComponent } from './features/sidebar/sidebar.component'; 

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent, SidebarComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  private readonly authService = inject(AuthService);

  // Exponemos los flujos al HTML
  public isReady$!: Observable<boolean>;
  public isAuthenticated$!: Observable<boolean>;

  ngOnInit(): void {
    // 1. Monitoreamos si el servicio ya terminó de validar el F5/Refresh Token
    this.isReady$ = this.authService.isReady$;

    // 2. Monitoreamos si el estado final es autenticado o no
    this.isAuthenticated$ = this.authService.authStatus$; 
  }
}