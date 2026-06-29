import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  errorMessage = '';
  isSubmitting = false;
  showPassword = false;
  showForgotPasswordModal = false;

  readonly form = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(50)]],
    password: ['', [Validators.required]],
    rememberMe: [false] // Agregado para el checkbox
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  // Método agregado
  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  // ✅ Abre el modal informativo
  forgotPassword(event: Event): void {
    event.preventDefault();
    this.showForgotPasswordModal = true;
  }

  // ✅ Cierra el modal
  closeForgotPasswordModal(): void {
    this.showForgotPasswordModal = false;
  }

  // Método agregado
  socialLogin(provider: string): void {
    console.log(`Login con ${provider}`);
    // Aquí iría la integración con OAuth de cada proveedor
    // Por ahora es un placeholder
    this.errorMessage = `Login con ${provider} no implementado aún`;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage = '';
    this.isSubmitting = true;

    const { username, password, rememberMe } = this.form.getRawValue();
    
    this.authService.login(username, password).subscribe({
      next: () => {
        this.isSubmitting = false;
        void this.router.navigateByUrl('/');
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error.status === 423
          ? 'La cuenta está bloqueada temporalmente.'
          : error.status === 401
            ? 'Credenciales incorrectas.'
            : 'Error de conexión. Inténtalo de nuevo más tarde.';
      }
    });
  }
}