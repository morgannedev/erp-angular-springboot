package com.algedro.cliente.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponse {
    private Long id;
    private String nombre;
    private String apellidos;
    private String telefono;
    private String email;
    private String nif; // Este campo se enmascarará en el controlador según rol
    private String direccion;
    private String ciudad;
    private String notas;
    private boolean activo;
}