import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { EmpleadoService } from '../../../core/services/empleado.service';

@Component({
  selector: 'app-empleado-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './empleado-form.component.html',
  styleUrls: ['./empleado-form.component.css']
})
export class EmpleadoFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly empleadoService = inject(EmpleadoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  empleadoId: number | null = null;
  isSaving = false;
  errorMessage = '';

  form!: FormGroup;

  ngOnInit(): void {
    this.initForm();
    this.checkEditMode();
  }

  private initForm(): void {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50)]],
      password: ['', [Validators.minLength(6)]],
      rol: ['EMPLEADO', Validators.required],
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      apellidos: ['', [Validators.required, Validators.maxLength(150)]],
      dni: ['', Validators.maxLength(20)],
      telefono: ['', Validators.maxLength(20)],
      email: ['', [Validators.email, Validators.maxLength(150)]],
      cargo: ['', [Validators.required, Validators.maxLength(100)]],
      salario: [null as number | null, Validators.min(0)],
      fechaContratacion: ['', Validators.required],
      fechaBaja: [''],
      notas: ['']
    });
  }

  private checkEditMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.empleadoId = id ? Number(id) : null;

    if (!this.empleadoId) {
      // En modo creación, la contraseña es obligatoria
      this.form.get('password')?.addValidators(Validators.required);
      this.form.get('password')?.updateValueAndValidity();
      return;
    }

    // En modo edición, quitar required de password
    this.form.get('password')?.clearValidators();
    this.form.get('password')?.updateValueAndValidity();
    this.loadEmpleadoData();
  }

  private loadEmpleadoData(): void {
    if (!this.empleadoId) return;

    this.empleadoService.obtener(this.empleadoId).subscribe({
      next: (empleado) => {
        this.form.patchValue({
          username: empleado.username,
          rol: empleado.rol,
          nombre: empleado.nombre,
          apellidos: empleado.apellidos,
          dni: empleado.dni ?? '',
          telefono: empleado.telefono ?? '',
          email: empleado.email ?? '',
          cargo: empleado.cargo,
          salario: empleado.salario ?? null,
          fechaContratacion: empleado.fechaContratacion,
          fechaBaja: empleado.fechaBaja ?? '',
          notas: empleado.notas ?? ''
        });
      },
      error: (err) => {
        console.error('Error loading empleado:', err);
        this.errorMessage = 'No se pudo cargar el empleado.';
      }
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.markFormGroupTouched(this.form);
      return;
    }

    this.isSaving = true;
    this.errorMessage = '';
    const value = this.form.getRawValue();

    const request$ = this.empleadoId
      ? this.empleadoService.actualizar(this.empleadoId, {
          nombre: value.nombre ?? '',
          apellidos: value.apellidos ?? '',
          dni: value.dni || null,
          telefono: value.telefono || null,
          email: value.email || null,
          cargo: value.cargo ?? '',
          salario: value.salario,
          fechaBaja: value.fechaBaja || null,
          notas: value.notas || null
        })
      : this.empleadoService.crear({
          username: value.username ?? '',
          password: value.password ?? '',
          rol: value.rol as 'ADMIN' | 'EMPLEADO',
          nombre: value.nombre ?? '',
          apellidos: value.apellidos ?? '',
          dni: value.dni || null,
          telefono: value.telefono || null,
          email: value.email || null,
          cargo: value.cargo ?? '',
          salario: value.salario,
          fechaContratacion: value.fechaContratacion ?? '',
          notas: value.notas || null
        });

    request$.subscribe({
      next: () => {
        this.router.navigate(['/empleados']);
      },
      error: (err) => {
        this.isSaving = false;
        if (err.status === 409) {
          this.errorMessage = 'Ya existe un empleado con este nombre de usuario.';
        } else if (err.status === 400) {
          this.errorMessage = 'Datos inválidos. Verifica los campos obligatorios.';
        } else {
          this.errorMessage = err.error?.mensaje ?? 'No se pudo guardar el empleado.';
        }
        console.error('Save error:', err);
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/empleados']);
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