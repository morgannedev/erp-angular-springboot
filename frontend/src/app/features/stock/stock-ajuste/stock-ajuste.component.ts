import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { StockService } from '../stock.service';

@Component({
  selector: 'app-stock-ajuste',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="ajuste-container">
      <h2>Ajuste manual de stock</h2>
      
      <form [formGroup]="form" (ngSubmit)="guardar()">
        <div class="form-group">
          <label>Cantidad (puede ser negativa) *</label>
          <input 
            type="number" 
            formControlName="cantidad" 
            step="1"
            class="form-control"
          />
          <small *ngIf="campo('cantidad')?.errors?.['required']" class="error-text">
            La cantidad es obligatoria
          </small>
          <small *ngIf="campo('cantidad')?.errors?.['zero']" class="error-text">
            La cantidad no puede ser cero
          </small>
        </div>

        <div class="form-group">
          <label>Motivo *</label>
          <textarea 
            formControlName="motivo" 
            rows="3" 
            maxlength="200"
            class="form-control"
            placeholder="Explique el motivo del ajuste (mínimo 10 caracteres)..."
          ></textarea>
          <small *ngIf="campo('motivo')?.errors?.['required']" class="error-text">
            El motivo es obligatorio
          </small>
          <small *ngIf="campo('motivo')?.errors?.['minlength']" class="error-text">
            El motivo debe tener al menos 10 caracteres
          </small>
          <small class="hint-text">{{ campo('motivo')?.value?.length || 0 }}/200 caracteres</small>
        </div>

        <!-- Advertencia de stock negativo -->
        <div *ngIf="stockResultanteNegativo" class="warning-box">
          <p>⚠️ <strong>Advertencia:</strong> Este ajuste dejará el stock en <strong>{{ stockResultante }}</strong> unidades (negativo).</p>
          <label class="checkbox-label">
            <input type="checkbox" formControlName="forzarNegativo" />
            Forzar ajuste negativo
          </label>
        </div>

        <div class="info-stock-actual" *ngIf="stockActual !== null">
          Stock actual: <strong>{{ stockActual }}</strong> unidades
        </div>

        <div class="form-actions">
          <button type="submit" [disabled]="form.invalid || guardando" class="btn btn-primary">
            {{ guardando ? 'Aplicando...' : 'Aplicar ajuste' }}
          </button>
          <button type="button" (click)="cancelar()" class="btn btn-secondary">Cancelar</button>
        </div>

        <div *ngIf="errorGeneral" class="error-general">
          ❌ {{ errorGeneral }}
        </div>
      </form>
    </div>
  `,
  styles: [`
    .ajuste-container {
      max-width: 600px;
      margin: 0 auto;
      padding: 1rem;
    }
    h2 {
      margin-bottom: 1.5rem;
      color: #333;
    }
    .form-group {
      margin-bottom: 1.5rem;
    }
    label {
      display: block;
      font-weight: 500;
      margin-bottom: 0.5rem;
      color: #555;
    }
    .form-control {
      width: 100%;
      padding: 0.5rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
    }
    textarea.form-control {
      resize: vertical;
    }
    .error-text {
      color: #dc3545;
      font-size: 0.8rem;
      margin-top: 0.25rem;
      display: block;
    }
    .hint-text {
      color: #666;
      font-size: 0.75rem;
      margin-top: 0.25rem;
      display: block;
    }
    .warning-box {
      background: #fff3cd;
      border: 1px solid #ffc107;
      border-radius: 4px;
      padding: 1rem;
      margin-bottom: 1.5rem;
    }
    .warning-box p {
      margin: 0 0 0.5rem 0;
      color: #856404;
    }
    .checkbox-label {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      cursor: pointer;
      font-weight: normal;
    }
    .checkbox-label input {
      width: auto;
    }
    .info-stock-actual {
      background: #e7f3ff;
      padding: 0.75rem;
      border-radius: 4px;
      margin-bottom: 1.5rem;
      text-align: center;
    }
    .form-actions {
      display: flex;
      gap: 1rem;
      margin-top: 1rem;
    }
    .btn {
      padding: 0.5rem 1.5rem;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 1rem;
    }
    .btn-primary {
      background: #007bff;
      color: white;
    }
    .btn-primary:disabled {
      background: #6c757d;
      cursor: not-allowed;
    }
    .btn-secondary {
      background: #6c757d;
      color: white;
    }
    .error-general {
      background: #f8d7da;
      color: #721c24;
      padding: 0.75rem;
      border-radius: 4px;
      margin-top: 1rem;
    }
  `]
})
export class StockAjusteComponent implements OnInit {
  private fb = inject(FormBuilder);
  private stockSvc = inject(StockService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  
  form!: FormGroup;
  guardando = false;
  errorGeneral: string | null = null;
  stockResultante = 0;
  stockResultanteNegativo = false;
  stockActual: number | null = null;
  private productoId!: number;
  
  ngOnInit() {
    this.productoId = Number(this.route.snapshot.paramMap.get('productoId'));
    
    this.form = this.fb.group({
      cantidad: [0, [Validators.required, this.cantidadNoCero]],
      motivo: ['', [Validators.required, Validators.minLength(10)]],
      forzarNegativo: [false]
    });
    
    // Obtener stock actual
    this.stockSvc.obtenerProducto(this.productoId).subscribe({
      next: (producto) => {
        this.stockActual = producto.stockActual;
        this.actualizarAdvertencia();
      },
      error: () => {
        this.stockActual = null;
      }
    });
    
    this.form.get('cantidad')?.valueChanges.subscribe(() => {
      this.actualizarAdvertencia();
      // Resetear forzarNegativo si ya no es necesario
      if (!this.stockResultanteNegativo && this.form.get('forzarNegativo')?.value) {
        this.form.get('forzarNegativo')?.setValue(false);
      }
    });
  }
  
  cantidadNoCero(control: AbstractControl): ValidationErrors | null {
    return control.value === 0 ? { zero: true } : null;
  }
  
  actualizarAdvertencia() {
    const cantidad = this.form.get('cantidad')?.value || 0;
    if (this.stockActual !== null) {
      this.stockResultante = this.stockActual + cantidad;
      this.stockResultanteNegativo = this.stockResultante < 0;
    }
  }
  
  guardar() {
    if (this.form.invalid) return;
    
    // Validación extra para negativo sin forzar
    if (this.stockResultanteNegativo && !this.form.get('forzarNegativo')?.value) {
      this.errorGeneral = 'Debe marcar "Forzar ajuste negativo" para continuar';
      return;
    }
    
    this.guardando = true;
    this.errorGeneral = null;
    
    this.stockSvc.registrarAjuste(this.productoId, this.form.value).subscribe({
      next: () => {
        this.router.navigate(['/stock']);
      },
      error: (err) => {
        this.guardando = false;
        this.errorGeneral = err.error?.mensaje || err.error?.message || 'Error al aplicar el ajuste';
      }
    });
  }
  
  cancelar() {
    this.router.navigate(['/stock']);
  }
  
  campo(name: string) {
    return this.form.get(name);
  }
}