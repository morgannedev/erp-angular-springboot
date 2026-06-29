import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { Empleado } from '../../../core/models/empleado.model';
import { EmpleadoService } from '../../../core/services/empleado.service';

@Component({
  selector: 'app-empleado-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './empleado-list.component.html',
  styleUrls: ['./empleado-list.component.css']
})
export class EmpleadoListComponent implements OnInit {
  private readonly authSvc = inject(AuthService);
  
  readonly esAdmin = this.authSvc.isAdmin();
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly activeControl = new FormControl('', { nonNullable: true });
  
  empleados: Empleado[] = [];
  isLoading = false;
  errorMessage = '';

  constructor(private readonly empleadoService: EmpleadoService) {}

  ngOnInit(): void {
    this.cargar();

    this.searchControl.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged())
      .subscribe(() => this.cargar());

    this.activeControl.valueChanges.subscribe(() => this.cargar());
  }

  cargar(): void {
    this.isLoading = true;
    this.errorMessage = '';
    const estado = this.activeControl.value;
    const activo = estado === '' ? undefined : estado === 'true';

    this.empleadoService.listar(this.searchControl.value, activo).subscribe({
      next: (page) => {
        this.empleados = page.content ?? page.contenido ?? [];
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudo cargar el listado de empleados.';
        this.isLoading = false;
      }
    });
  }

  toggleCuenta(empleado: Empleado): void {
    if (!this.esAdmin) return;
    
    this.empleadoService.cambiarCuenta(empleado.id, !empleado.cuentaActiva).subscribe({
      next: (actualizado) => {
        empleado.cuentaActiva = actualizado.cuentaActiva;
      },
      error: () => {
        this.errorMessage = 'No se pudo actualizar el estado de la cuenta.';
      }
    });
  }

  eliminar(empleado: Empleado): void {
    if (!this.esAdmin) return;
    
    if (confirm(`¿Estás seguro de que deseas eliminar a ${empleado.nombre} ${empleado.apellidos}?`)) {
      this.empleadoService.eliminar(empleado.id).subscribe({
        next: () => {
          this.empleados = this.empleados.filter((item) => item.id !== empleado.id);
        },
        error: () => {
          this.errorMessage = 'No se pudo eliminar. Si tiene ventas, desactiva la cuenta.';
        }
      });
    }
  }
}