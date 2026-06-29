// src/app/features/proveedores/proveedor-form/proveedor-form.component.ts
// Usado tanto para Crear (sin id en ruta) como para Editar (con id en ruta)
// Solo ADMIN llega aquí — la ruta está protegida con adminGuard

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators
} from '@angular/forms';

import { ProveedorService } from '../proveedor.service';
import { ProveedorRequest } from '../../../core/models/proveedor.model';

@Component({
  selector: 'app-proveedor-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './proveedor-form.component.html',
  styleUrls: ['./proveedor-form.component.css']
})
export class ProveedorFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly svc = inject(ProveedorService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  form!: FormGroup;
  modoEdicion = false;
  guardando = false;
  errorServidor: 'nif' | null = null;
  errorGeneral: string | null = null;

  private proveedorId: number | null = null;

  ngOnInit(): void {
    this.initForm();
    this.checkEditMode();
  }

  private initForm(): void {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(150)]],
      nif: ['', Validators.maxLength(20)],
      contactoNombre: ['', Validators.maxLength(100)],
      telefono: ['', Validators.maxLength(20)],
      email: ['', [Validators.email, Validators.maxLength(150)]],
      direccion: ['', Validators.maxLength(255)],
      ciudad: ['', Validators.maxLength(100)],
      notas: ['']
    });
  }

  private checkEditMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.modoEdicion = true;
      this.proveedorId = +id;
      this.loadProveedorData();
    }
  }

  private loadProveedorData(): void {
    if (!this.proveedorId) return;

    this.svc.obtener(this.proveedorId).subscribe({
      next: (proveedor) => {
        this.form.patchValue(proveedor);
      },
      error: (err) => {
        console.error('Error loading supplier:', err);
        this.errorGeneral = 'No se pudo cargar el proveedor';
      }
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.markFormGroupTouched(this.form);
      return;
    }

    this.guardando = true;
    this.errorServidor = null;
    this.errorGeneral = null;

    const dto: ProveedorRequest = this.form.getRawValue();
    const op$ = this.modoEdicion
      ? this.svc.actualizar(this.proveedorId!, dto)
      : this.svc.crear(dto);

    op$.subscribe({
      next: () => {
        this.router.navigate(['/proveedores']);
      },
      error: (err) => {
        this.guardando = false;
        
        if (err.status === 409) {
          this.errorServidor = 'nif'; // NIF/CIF duplicado
        } else if (err.status === 400) {
          this.errorGeneral = 'Datos inválidos. Verifica los campos obligatorios.';
        } else {
          this.errorGeneral = err.error?.mensaje ?? 'Error inesperado al guardar el proveedor';
        }
        
        console.error('Save error:', err);
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/proveedores']);
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