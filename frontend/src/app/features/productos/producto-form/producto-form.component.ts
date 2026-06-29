import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { CategoriaService } from '../../categorias/categoria.service';
import { ProveedorService } from '../../proveedores/proveedor.service';
import { CategoriaResumen } from '../../../core/models/categoria.model';
import { ProveedorResumen } from '../../../core/models/proveedor.model';
import { ProductoRequest } from '../../../core/models/producto.model';
import { ProductoService } from '../producto.service';

@Component({
  selector: 'app-producto-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './producto-form.component.html',
  styleUrls: ['./producto-form.component.css']
})
export class ProductoFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly svc = inject(ProductoService);
  private readonly categoriaSvc = inject(CategoriaService);
  private readonly proveedorSvc = inject(ProveedorService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  form!: FormGroup;
  categorias: CategoriaResumen[] = [];
  proveedores: ProveedorResumen[] = [];
  modoEdicion = false;
  guardando = false;
  errorGeneral: string | null = null;

  private productoId: number | null = null;

  ngOnInit(): void {
    this.initForm();
    this.loadSelectOptions();
    this.checkEditMode();
  }

  private initForm(): void {
    this.form = this.fb.group({
      referencia: ['', [Validators.required, Validators.maxLength(80)]],
      ean: ['', Validators.maxLength(20)],
      nombre: ['', [Validators.required, Validators.maxLength(150)]],
      descripcion: [''],
      categoriaId: [null, Validators.required],
      proveedorId: [null],
      precioVenta: [null, [Validators.required, Validators.min(0.01)]],
      precioCoste: [null, Validators.min(0.01)],
      unidadMedida: ['ud', Validators.maxLength(20)],
      stockMinimo: [0, Validators.min(0)],
      stockMaximo: [null, Validators.min(0)]
    });
  }

  private loadSelectOptions(): void {
    this.categoriaSvc.listarRaices().subscribe({
      next: (categorias) => this.categorias = categorias,
      error: (err) => console.error('Error loading categories:', err)
    });
    
    this.proveedorSvc.listarResumen().subscribe({
      next: (proveedores) => this.proveedores = proveedores,
      error: (err) => console.error('Error loading suppliers:', err)
    });
  }

  private checkEditMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.modoEdicion = true;
      this.productoId = +id;
      this.loadProductData();
    }
  }

  private loadProductData(): void {
    this.svc.obtener(this.productoId!).subscribe({
      next: (producto) => {
        this.form.patchValue(producto);
      },
      error: (err) => {
        console.error('Error loading product:', err);
        this.errorGeneral = 'No se pudo cargar el producto';
      }
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.markFormGroupTouched(this.form);
      return;
    }

    this.guardando = true;
    this.errorGeneral = null;

    const dto: ProductoRequest = this.form.getRawValue();
    const op$ = this.modoEdicion
      ? this.svc.actualizar(this.productoId!, dto)
      : this.svc.crear(dto);

    op$.subscribe({
      next: () => {
        this.router.navigate(['/productos']);
      },
      error: (err) => {
        this.guardando = false;
        this.errorGeneral = err.error?.mensaje ?? 'Error inesperado al guardar el producto';
        console.error('Save error:', err);
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/productos']);
  }

  campo(name: string) {
    return this.form.get(name);
  }

  campoInvalido(name: string): boolean {
    const field = this.campo(name);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }
}