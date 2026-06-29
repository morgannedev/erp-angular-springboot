package com.algedro.cliente.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequest {
    private String nombre;
    private String apellidos;
    private String telefono;
    private String email;
    private String nif;
    private String direccion;
    private String ciudad;
    private String notas;
}