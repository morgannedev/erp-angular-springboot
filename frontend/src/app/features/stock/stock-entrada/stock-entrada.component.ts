import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { ProveedorResumen } from '../../../core/models/proveedor.model';
import { ProveedorService } from '../../proveedores/proveedor.service';
import { StockService } from '../stock.service';

@Component({
  selector: 'app-stock-entrada',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <h2>Registrar entrada</h2>

    <form [formGroup]="form" (ngSubmit)="guardar()">
      <label>Cantidad *
        <input formControlName="cantidad" type="number" min="1" step="1" />
        <small *ngIf="campo('cantidad')?.errors?.['required']">Obligatorio</small>
      </label>

      <label>Proveedor
        <select formControlName="proveedorId">
          <option [ngValue]="null">Sin proveedor</option>
          <option *ngFor="let p of proveedores" [ngValue]="p.id">{{ p.nombre }}</option>
        </select>
      </label>

      <label>Albaran
        <input formControlName="albaran" maxlength="100" />
      </label>

      <div class="form-actions">
        <button type="submit" [disabled]="form.invalid || guardando" class="btn btn-primary">
          {{ guardando ? 'Guardando...' : 'Guardar' }}
        </button>
        <button type="button" (click)="cancelar()" class="btn">Cancelar</button>
      </div>

      <p *ngIf="errorGeneral" class="error">{{ errorGeneral }}</p>
    </form>
  `
})
export class StockEntradaComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly stockSvc = inject(StockService);
  private readonly proveedorSvc = inject(ProveedorService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  form!: FormGroup;
  proveedores: ProveedorResumen[] = [];
  guardando = false;
  errorGeneral: string | null = null;

  private productoId!: number;

  ngOnInit(): void {
    this.productoId = Number(this.route.snapshot.paramMap.get('productoId'));
    this.form = this.fb.group({
      cantidad: [1, [Validators.required, Validators.min(1)]],
      proveedorId: [null],
      albaran: ['', Validators.maxLength(100)]
    });
    this.proveedorSvc.listarResumen().subscribe(proveedores => this.proveedores = proveedores);
  }

  guardar(): void {
    if (this.form.invalid) return;
    this.guardando = true;
    this.errorGeneral = null;

    this.stockSvc.registrarEntrada(this.productoId, this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/stock', this.productoId, 'movimientos']),
      error: err => {
        this.guardando = false;
        this.errorGeneral = err.error?.mensaje ?? 'Error inesperado';
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/stock/alertas']);
  }

  campo(name: string) {
    return this.form.get(name);
  }
}
