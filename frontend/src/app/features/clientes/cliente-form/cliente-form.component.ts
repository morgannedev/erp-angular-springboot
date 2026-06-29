import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ClienteRequest } from '../../../core/models/cliente.model';
import { ClienteService } from '../cliente.service';

@Component({
  selector: 'app-cliente-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './cliente-form.component.html',
  styleUrls: ['./cliente-form.component.css']
})
export class ClienteFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly clienteSvc = inject(ClienteService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  form!: FormGroup;
  modoEdicion = false;
  guardando = false;
  errorGeneral: string | null = null;

  private clienteId: number | null = null;

  ngOnInit(): void {
    this.initForm();
    this.checkEditMode();
  }

  private initForm(): void {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      apellidos: ['', Validators.maxLength(100)],
      telefono: ['', [Validators.required, Validators.maxLength(20)]],
      email: ['', [Validators.email, Validators.maxLength(150)]],
      nif: ['', Validators.maxLength(20)],
      direccion: ['', Validators.maxLength(255)],
      ciudad: ['', Validators.maxLength(100)],
      notas: ['', Validators.maxLength(500)]
    });
  }

  private checkEditMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.modoEdicion = true;
      this.clienteId = +id;
      this.loadClientData();
    }
  }

  private loadClientData(): void {
    if (!this.clienteId) return;
    
    this.clienteSvc.obtener(this.clienteId).subscribe({
      next: (cliente) => {
        this.form.patchValue(cliente);
      },
      error: (err) => {
        console.error('Error loading client:', err);
        this.errorGeneral = 'No se pudo cargar el cliente';
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

    const dto: ClienteRequest = this.form.getRawValue();
    const op$ = this.modoEdicion
      ? this.clienteSvc.actualizar(this.clienteId!, dto)
      : this.clienteSvc.crear(dto);

    op$.subscribe({
      next: () => {
        this.router.navigate(['/clientes']);
      },
      error: (err) => {
        this.guardando = false;
        
        if (err.status === 409) {
          this.errorGeneral = 'Ya existe un cliente con este NIF, email o teléfono';
        } else if (err.status === 400) {
          this.errorGeneral = 'Datos inválidos. Verifica el formato del teléfono o email';
        } else {
          this.errorGeneral = err.error?.mensaje ?? 'Error inesperado al guardar el cliente';
        }
        
        console.error('Save error:', err);
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/clientes']);
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