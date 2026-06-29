import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { StockNivelPage } from '../../../core/models/stock.model';
import { StockService } from '../stock.service';

@Component({
  selector: 'app-stock-alertas',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './stock-alertas.component.html',
  styleUrls: ['./stock-alertas.component.css']
})
export class StockAlertasComponent implements OnInit {
  private readonly stockSvc = inject(StockService);

  page: StockNivelPage | null = null;

  ngOnInit(): void {
    this.cargarAlertas();
  }

  cargarAlertas(): void {
    this.stockSvc.listar({ 
      soloAlertas: true, 
      tamanio: 25 
    }).subscribe({
      next: (page) => {
        this.page = page;
      },
      error: (err) => {
        console.error('Error loading stock alerts:', err);
        this.page = { contenido: [], totalElementos: 0, totalPaginas: 0, pagina: 0, tamano: 25 };
      }
    });
  }
}