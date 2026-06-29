import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { CategoriaRequest, CategoriaResumen } from '../../../core/models/categoria.model';
import { CategoriaService } from '../categoria.service';

@Component({
  selector: 'app-categoria-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './categoria-form.component.html',
  styleUrls: ['./categoria-form.component.css']
})
export class CategoriaFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly svc = inject(CategoriaService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  form!: FormGroup;
  raices: CategoriaResumen[] = [];
  modoEdicion = false;
  guardando = false;
  errorGeneral: string | null = null;

  private categoriaId: number | null = null;

  ngOnInit(): void {
    this.initForm();
    this.checkEditMode();
    this.loadParentCategories();
  }

  private initForm(): void {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      descripcion: ['', Validators.maxLength(255)],
      padreId: [null]
    });
  }

  private checkEditMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.modoEdicion = true;
      this.categoriaId = +id;
      this.loadCategoryData();
    }
  }

  private loadParentCategories(): void {
    this.svc.listarRaices().subscribe({
      next: (raices) => {
        // En modo edición, excluir la categoría actual y sus subcategorías
        if (this.categoriaId != null) {
          this.raices = raices.filter(c => c.id !== this.categoriaId);
        } else {
          this.raices = raices;
        }
      },
      error: (err) => {
        console.error('Error loading parent categories:', err);
        this.raices = [];
      }
    });
  }

  private loadCategoryData(): void {
    if (!this.categoriaId) return;
    
    this.svc.obtener(this.categoriaId).subscribe({
      next: (categoria) => {
        this.form.patchValue(categoria);
        // Actualizar lista de padres excluyendo la categoría actual
        this.svc.listarRaices().subscribe({
          next: (raices) => {
            this.raices = raices.filter(c => c.id !== this.categoriaId);
          }
        });
      },
      error: (err) => {
        console.error('Error loading category:', err);
        this.errorGeneral = 'No se pudo cargar la categoría';
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

    const dto: CategoriaRequest = this.form.getRawValue();
    const op$ = this.modoEdicion
      ? this.svc.actualizar(this.categoriaId!, dto)
      : this.svc.crear(dto);

    op$.subscribe({
      next: () => {
        this.router.navigate(['/categorias']);
      },
      error: (err) => {
        this.guardando = false;
        this.errorGeneral = err.error?.mensaje ?? 'Error inesperado al guardar la categoría';
        console.error('Save error:', err);
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/categorias']);
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