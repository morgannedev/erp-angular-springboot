import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { Cliente } from '../../../core/models/cliente.model';
import { ClienteService } from '../cliente.service';

@Component({
  selector: 'app-cliente-quick-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './cliente-quick-create.component.html',
  styleUrls: ['./cliente-quick-create.component.css']
})
export class ClienteQuickCreateComponent {
  private readonly fb = inject(FormBuilder);
  private readonly clienteSvc = inject(ClienteService);

  @Output() creado = new EventEmitter<Cliente>();
  @Output() cancelar = new EventEmitter<void>();

  guardando = false;
  errorGeneral: string | null = null;

  readonly form: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    telefono: ['', [Validators.required, Validators.maxLength(20)]],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    nif: ['', Validators.maxLength(20)]
  });

  guardar(): void {
    if (this.form.invalid) {
      this.markFormGroupTouched(this.form);
      return;
    }

    this.guardando = true;
    this.errorGeneral = null;

    this.clienteSvc.crearRapido(this.form.getRawValue()).subscribe({
      next: (cliente) => {
        this.guardando = false;
        this.form.reset();
        this.creado.emit(cliente);
      },
      error: (err) => {
        this.guardando = false;
        
        if (err.status === 409) {
          this.errorGeneral = 'Ya existe un cliente con este NIF o email';
        } else if (err.status === 400) {
          this.errorGeneral = 'Datos inválidos. Verifica el formato del teléfono o NIF';
        } else {
          this.errorGeneral = err.error?.mensaje ?? 'Error inesperado al crear el cliente';
        }
        
        console.error('Error creating client:', err);
      }
    });
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