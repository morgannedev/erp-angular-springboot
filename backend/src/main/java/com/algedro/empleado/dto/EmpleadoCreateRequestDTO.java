package com.algedro.empleado.dto;

import com.algedro.empleado.enums.RolEmpleado;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
public class EmpleadoCreateRequestDTO {

    // ─────────────────────────────────────────────────────────────
    // Datos de la Cuenta de Usuario
    // ─────────────────────────────────────────────────────────────

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 50, message = "El nombre de usuario debe tener entre 4 y 50 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private RolEmpleado rol;

    // ─────────────────────────────────────────────────────────────
    // Datos Personales y Profesionales del Empleado
    // ─────────────────────────────────────────────────────────────

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
    private String apellidos;

    @Size(max = 20, message = "El DNI no puede superar los 20 caracteres")
    private String dni; // Nullable según OpenAPI

    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    private String telefono; // Nullable según OpenAPI

    @Email(message = "El formato del email no es válido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email; // Nullable según OpenAPI

    @NotBlank(message = "El cargo es obligatorio")
    @Size(max = 100, message = "El cargo no puede superar los 100 caracteres")
    private String cargo;

    // Se usa BigDecimal en el DTO de entrada para manejar con precisión la entrada monetaria
    @DecimalMin(value = "0.01", message = "El salario debe ser mayor que cero")
    private Double salario; // Nullable según OpenAPI

    @NotNull(message = "La fecha de contratación es obligatoria")
    private LocalDate fechaContratacion;

    private String notas; // Nullable según OpenAPI

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public RolEmpleado getRol() {
        return rol;
    }

    public void setRol(RolEmpleado rol) {
        this.rol = rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public Double getSalario() {
        return salario;
    }

    public void setSalario(Double salario) {
        this.salario = salario;
    }

    public LocalDate getFechaContratacion() {
        return fechaContratacion;
    }

    public void setFechaContratacion(LocalDate fechaContratacion) {
        this.fechaContratacion = fechaContratacion;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }
}
