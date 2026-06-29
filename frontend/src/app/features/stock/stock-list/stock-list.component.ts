import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, Subject } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { StockNivelPage } from '../../../core/models/stock.model';
import { StockService } from '../stock.service';

@Component({
  selector: 'app-stock-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './stock-list.component.html',
  styleUrls: ['./stock-list.component.css']
})
export class StockListComponent implements OnInit {
  private readonly stockSvc = inject(StockService);
  private readonly authSvc = inject(AuthService);
  
  esAdmin = this.authSvc.isAdmin();
  page: StockNivelPage | null = null;
  paginaActual = 0;
  busqueda = '';
  private busquedaSubject = new Subject<string>();
  
  ngOnInit() {
    this.busquedaSubject.pipe(debounceTime(300)).subscribe(() => {
      this.paginaActual = 0;
      this.cargar();
    });
    this.cargar();
  }
  
  onBuscar() {
    this.busquedaSubject.next(this.busqueda);
  }
  
  cargar() {
    this.stockSvc.listar({
      pagina: this.paginaActual,
      tamanio: 25,
      soloAlertas: false,
      query: this.busqueda || undefined
    }).subscribe({
      next: (page) => this.page = page,
      error: (err) => console.error('Error loading stock:', err)
    });
  }
  
  irA(pagina: number) {
    this.paginaActual = pagina;
    this.cargar();
  }
  
  actualizarMinimo(productoId: number, event: Event) {
    const nuevoMinimo = parseInt((event.target as HTMLInputElement).value);
    if (!isNaN(nuevoMinimo) && nuevoMinimo >= 0) {
      this.stockSvc.actualizarMinimo(productoId, nuevoMinimo).subscribe({
        next: () => this.cargar(),
        error: (err) => console.error('Error updating min stock:', err)
      });
    }
  }
  
  actualizarMaximo(productoId: number, event: Event) {
    const nuevoMaximo = parseInt((event.target as HTMLInputElement).value);
    if (!isNaN(nuevoMaximo) && nuevoMaximo >= 0) {
      this.stockSvc.actualizarMaximo(productoId, nuevoMaximo).subscribe({
        next: () => this.cargar(),
        error: (err) => console.error('Error updating max stock:', err)
      });
    }
  }
}