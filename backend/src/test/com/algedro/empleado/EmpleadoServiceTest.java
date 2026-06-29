package com.algedro.empleado;

import com.algedro.empleado.dto.EmpleadoCreateRequestDTO;
import com.algedro.empleado.dto.EmpleadoResponseDTO;
import com.algedro.empleado.dto.EmpleadoUpdateRequestDTO;
import com.algedro.empleado.entity.Empleado;
import com.algedro.empleado.enums.RolEmpleado;
import com.algedro.empleado.mapper.EmpleadoMapper;
import com.algedro.empleado.repository.EmpleadoRepository;
import com.algedro.empleado.service.EmpleadoService;
import com.algedro.exception.BusinessRuleException;
import com.algedro.exception.DuplicateResourceException;
import com.algedro.exception.ResourceNotFoundException;
import com.algedro.usuario.Rol;
import com.algedro.usuario.Usuario;
import com.algedro.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para EmpleadoService — Fase 2 (TDD).
 *
 * Cubre todos los criterios de aceptación definidos en:
 *   - Contexto_e_instrucciones.md §2.1, §2.2
 *   - openapi.yaml /empleados/*
 *   - algedro-technical-design.md Fase 2
 *
 * Metodología: RED primero — estos tests se escriben ANTES de implementar EmpleadoService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmpleadoService — Tests Unitarios")
class EmpleadoServiceTest {

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmpleadoMapper empleadoMapper;

    @InjectMocks
    private EmpleadoService empleadoService;

    // ─────────────────────────────────────────────────────────────
    // Fixtures reutilizables
    // ─────────────────────────────────────────────────────────────

    private EmpleadoCreateRequestDTO buildCreateRequest() {
        EmpleadoCreateRequestDTO dto = new EmpleadoCreateRequestDTO();
        dto.setUsername("nuevo_empleado");
        dto.setPassword("pass1234");
        dto.setRol(RolEmpleado.EMPLEADO);
        dto.setNombre("Ana");
        dto.setApellidos("López García");
        dto.setCargo("Dependienta");
        dto.setFechaContratacion(LocalDate.of(2024, 1, 15));
        return dto;
    }

    private Empleado buildEmpleado(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("nuevo_empleado");
        usuario.setRol(Rol.EMPLEADO);
        usuario.setActivo(true);

        Empleado empleado = new Empleado();
        empleado.setId(id);
        empleado.setUsuario(usuario);
        empleado.setNombre("Ana");
        empleado.setApellidos("López García");
        empleado.setCargo("Dependienta");
        empleado.setFechaContratacion(LocalDate.of(2024, 1, 15));
        return empleado;
    }

    private EmpleadoResponseDTO buildResponseDTO(Long id) {
        EmpleadoResponseDTO dto = new EmpleadoResponseDTO();
        dto.setId(id);
        dto.setNombre("Ana");
        dto.setApellidos("López García");
        dto.setCargo("Dependienta");
        dto.setUsername("nuevo_empleado");
        dto.setRol(RolEmpleado.EMPLEADO);
        dto.setCuentaActiva(true);
        return dto;
    }

    // ═════════════════════════════════════════════════════════════
    // 1. CREAR EMPLEADO
    // ═════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Crear empleado")
    class CrearEmpleado {

        @Test
        @DisplayName("Debería crear empleado y usuario simultáneamente con éxito")
        void testCrearEmpleado_exitoso() {
            // GIVEN
            EmpleadoCreateRequestDTO request = buildCreateRequest();
            Empleado empleadoGuardado = buildEmpleado(10L);
            EmpleadoResponseDTO responseEsperado = buildResponseDTO(10L);

            given(usuarioRepository.existsByUsername("nuevo_empleado")).willReturn(false);
            given(passwordEncoder.encode("pass1234")).willReturn("$2a$10$hashedpassword");
            given(usuarioRepository.save(any(Usuario.class))).willAnswer(inv -> {
                Usuario u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            given(empleadoRepository.save(any(Empleado.class))).willReturn(empleadoGuardado);
            given(empleadoMapper.toResponseDTO(empleadoGuardado)).willReturn(responseEsperado);

            // WHEN
            EmpleadoResponseDTO resultado = empleadoService.crearEmpleado(request);

            // THEN
            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(10L);
            assertThat(resultado.getUsername()).isEqualTo("nuevo_empleado");

            assertThat(resultado.getRol()).isEqualTo(RolEmpleado.EMPLEADO);
            assertThat(resultado.isCuentaActiva()).isTrue();

            verify(usuarioRepository).save(any(Usuario.class));
            verify(empleadoRepository).save(any(Empleado.class));
            verify(passwordEncoder).encode("pass1234");
        }

        @Test
        @DisplayName("Debería lanzar DuplicateResourceException si el username ya existe")
        void testCrearEmpleado_usernameDuplicado_lanzaExcepcion() {
            // GIVEN
            EmpleadoCreateRequestDTO request = buildCreateRequest();
            given(usuarioRepository.existsByUsername("nuevo_empleado")).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.crearEmpleado(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("username");

            verify(usuarioRepository, never()).save(any());
            verify(empleadoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debería lanzar DuplicateResourceException si el DNI ya existe")
        void testCrearEmpleado_dniDuplicado_lanzaExcepcion() {
            // GIVEN
            EmpleadoCreateRequestDTO request = buildCreateRequest();
            request.setDni("12345678A");

            given(usuarioRepository.existsByUsername("nuevo_empleado")).willReturn(false);
            given(empleadoRepository.existsByDni("12345678A")).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.crearEmpleado(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("DNI");

            verify(empleadoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debería lanzar DuplicateResourceException si el email ya existe")
        void testCrearEmpleado_emailDuplicado_lanzaExcepcion() {
            // GIVEN
            EmpleadoCreateRequestDTO request = buildCreateRequest();
            request.setEmail("ana@algedro.com");

            given(usuarioRepository.existsByUsername("nuevo_empleado")).willReturn(false);
            given(empleadoRepository.existsByEmail("ana@algedro.com")).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.crearEmpleado(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");

            verify(empleadoRepository, never()).save(any());
        }

        @Test
        @DisplayName("La contraseña siempre debe almacenarse cifrada con bcrypt, nunca en texto plano")
        void testCrearEmpleado_passwordSeCifraConBcrypt() {
            // GIVEN
            EmpleadoCreateRequestDTO request = buildCreateRequest();
            given(usuarioRepository.existsByUsername(anyString())).willReturn(false);
            given(passwordEncoder.encode("pass1234")).willReturn("$2a$10$HASH");
            given(usuarioRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(empleadoRepository.save(any())).willReturn(buildEmpleado(1L));
            given(empleadoMapper.toResponseDTO(any())).willReturn(buildResponseDTO(1L));

            // WHEN
            empleadoService.crearEmpleado(request);

            // THEN — la contraseña original nunca llega al repositorio
            verify(passwordEncoder).encode("pass1234");
            verify(usuarioRepository).save(argThat(u ->
                    u.getPasswordHash().equals("$2a$10$HASH") &&
                    !u.getPasswordHash().equals("pass1234")
            ));
        }

        @Test
        @DisplayName("Debería lanzar BusinessRuleException si usuario ya tiene empleado asociado (relación 1:1)")
        void testCrearEmpleado_usuarioYaAsociado_lanzaExcepcion() {
            // GIVEN — simulamos un usuario existente que ya tiene empleado
            EmpleadoCreateRequestDTO request = buildCreateRequest();
            given(usuarioRepository.existsByUsername("nuevo_empleado")).willReturn(false);

            Usuario usuarioExistente = new Usuario();
            usuarioExistente.setId(99L);
            given(usuarioRepository.save(any())).willReturn(usuarioExistente);
            given(empleadoRepository.existsByUsuarioId(99L)).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.crearEmpleado(request))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 2. OBTENER EMPLEADO
    // ═════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Obtener empleado")
    class ObtenerEmpleado {

        @Test
        @DisplayName("Debería retornar el empleado cuando el ID existe")
        void testGetEmpleado_existente_retornaDTO() {
            // GIVEN
            Empleado empleado = buildEmpleado(5L);
            EmpleadoResponseDTO dto = buildResponseDTO(5L);

            given(empleadoRepository.findById(5L)).willReturn(Optional.of(empleado));
            given(empleadoMapper.toResponseDTO(empleado)).willReturn(dto);

            // WHEN
            EmpleadoResponseDTO resultado = empleadoService.getEmpleadoPorId(5L);

            // THEN
            assertThat(resultado).isNotNull();
            assertThat(resultado.getId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Debería lanzar ResourceNotFoundException cuando el ID no existe")
        void testGetEmpleado_noExiste_lanzaExcepcion() {
            // GIVEN
            given(empleadoRepository.findById(999L)).willReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.getEmpleadoPorId(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Debería retornar listado paginado de empleados")
        void testListarEmpleados_retornaPaginado() {
            // GIVEN
            Pageable pageable = PageRequest.of(0, 25);
            List<Empleado> lista = List.of(buildEmpleado(1L), buildEmpleado(2L));
            Page<Empleado> page = new PageImpl<>(lista, pageable, 2);

            given(empleadoRepository.findAll(any(Pageable.class))).willReturn(page);
            given(empleadoMapper.toResponseDTO(any(Empleado.class))).willReturn(buildResponseDTO(1L));

            // WHEN
            Page<EmpleadoResponseDTO> resultado = empleadoService.listarEmpleados(null, null, pageable);

            // THEN
            assertThat(resultado.getContent()).hasSize(2);
            assertThat(resultado.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Debería filtrar por nombre/apellidos cuando se proporciona parámetro de búsqueda")
        void testListarEmpleados_conFiltroQ_filtraPorNombre() {
            // GIVEN
            Pageable pageable = PageRequest.of(0, 25);
            List<Empleado> lista = List.of(buildEmpleado(1L));
            Page<Empleado> page = new PageImpl<>(lista, pageable, 1);

            given(empleadoRepository.findByNombreContainingIgnoreCaseOrApellidosContainingIgnoreCase(
                    eq("Ana"), eq("Ana"), any(Pageable.class)
            )).willReturn(page);
            given(empleadoMapper.toResponseDTO(any())).willReturn(buildResponseDTO(1L));

            // WHEN
            Page<EmpleadoResponseDTO> resultado = empleadoService.listarEmpleados("Ana", null, pageable);

            // THEN
            assertThat(resultado.getContent()).hasSize(1);
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 3. ACTUALIZAR EMPLEADO
    // ═════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Actualizar empleado")
    class ActualizarEmpleado {

        @Test
        @DisplayName("Debería actualizar los datos del empleado con éxito")
        void testActualizarEmpleado_exitoso() {
            // GIVEN
            Empleado existente = buildEmpleado(5L);
            EmpleadoUpdateRequestDTO request = new EmpleadoUpdateRequestDTO();
            request.setNombre("Ana María");
            request.setApellidos("López García");
            request.setCargo("Encargada");
            request.setSalario(26000.00);

            given(empleadoRepository.findById(5L)).willReturn(Optional.of(existente));
            given(empleadoRepository.save(any())).willReturn(existente);
            given(empleadoMapper.toResponseDTO(any())).willReturn(buildResponseDTO(5L));

            // WHEN
            EmpleadoResponseDTO resultado = empleadoService.actualizarEmpleado(5L, request);

            // THEN
            assertThat(resultado).isNotNull();
            verify(empleadoRepository).save(existente);
        }

        @Test
        @DisplayName("Debería lanzar ResourceNotFoundException al actualizar un ID inexistente")
        void testActualizarEmpleado_noExiste_lanzaExcepcion() {
            // GIVEN
            given(empleadoRepository.findById(999L)).willReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.actualizarEmpleado(999L, new EmpleadoUpdateRequestDTO()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Debería lanzar DuplicateResourceException si el nuevo DNI ya pertenece a otro empleado")
        void testActualizarEmpleado_dniDuplicadoEnOtroEmpleado_lanzaExcepcion() {
            // GIVEN
            Empleado existente = buildEmpleado(5L);
            existente.setDni("11111111A");

            EmpleadoUpdateRequestDTO request = new EmpleadoUpdateRequestDTO();
            request.setDni("99999999Z"); // DNI de otro empleado

            given(empleadoRepository.findById(5L)).willReturn(Optional.of(existente));
            given(empleadoRepository.existsByDniAndIdNot("99999999Z", 5L)).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.actualizarEmpleado(5L, request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("DNI");
        }

        @Test
        @DisplayName("Debería lanzar DuplicateResourceException si el nuevo email ya pertenece a otro empleado")
        void testActualizarEmpleado_emailDuplicadoEnOtroEmpleado_lanzaExcepcion() {
            // GIVEN
            Empleado existente = buildEmpleado(5L);
            EmpleadoUpdateRequestDTO request = new EmpleadoUpdateRequestDTO();
            request.setEmail("otro@algedro.com");

            given(empleadoRepository.findById(5L)).willReturn(Optional.of(existente));
            given(empleadoRepository.existsByEmailAndIdNot("otro@algedro.com", 5L)).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.actualizarEmpleado(5L, request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("Debería rechazar fecha de baja anterior a fecha de contratación")
        void testActualizarEmpleado_fechaBajaAnteriorContratacion_lanzaExcepcion() {
            // GIVEN
            Empleado existente = buildEmpleado(5L);
            existente.setFechaContratacion(LocalDate.of(2022, 1, 1));

            EmpleadoUpdateRequestDTO request = new EmpleadoUpdateRequestDTO();
            request.setFechaBaja(LocalDate.of(2021, 12, 31)); // anterior a contratación

            given(empleadoRepository.findById(5L)).willReturn(Optional.of(existente));

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.actualizarEmpleado(5L, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("fecha");
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 4. ACTIVAR / DESACTIVAR CUENTA (soft delete)
    // ═════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Activar / desactivar cuenta")
    class GestionCuenta {

        @Test
        @DisplayName("Debería desactivar la cuenta del usuario sin eliminar el registro")
        void testDesactivarEmpleado_exitoso() {
            // GIVEN
            Empleado empleado = buildEmpleado(5L);
            empleado.getUsuario().setActivo(true);

            given(empleadoRepository.findById(5L)).willReturn(Optional.of(empleado));
            given(empleadoRepository.save(any())).willReturn(empleado);
            given(empleadoMapper.toResponseDTO(any())).willReturn(buildResponseDTO(5L));

            // WHEN
            empleadoService.cambiarEstadoCuenta(5L, false);

            // THEN — el registro sigue en BD pero activo = false
            verify(usuarioRepository).save(argThat(u -> !u.isActivo()));
            verify(empleadoRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Debería activar una cuenta previamente desactivada")
        void testActivarCuenta_exitoso() {
            // GIVEN
            Empleado empleado = buildEmpleado(5L);
            empleado.getUsuario().setActivo(false);

            given(empleadoRepository.findById(5L)).willReturn(Optional.of(empleado));
            given(empleadoRepository.save(any())).willReturn(empleado);
            given(empleadoMapper.toResponseDTO(any())).willReturn(buildResponseDTO(5L));

            // WHEN
            empleadoService.cambiarEstadoCuenta(5L, true);

            // THEN
            verify(usuarioRepository).save(argThat(Usuario::isActivo));
        }

        @Test
        @DisplayName("Debería lanzar ResourceNotFoundException al intentar cambiar estado de empleado inexistente")
        void testCambiarEstadoCuenta_empleadoNoExiste_lanzaExcepcion() {
            // GIVEN
            given(empleadoRepository.findById(999L)).willReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.cambiarEstadoCuenta(999L, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 5. CAMBIAR ROL
    // ═════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Cambiar rol de usuario")
    class CambiarRol {

        @Test
        @DisplayName("Debería cambiar el rol de EMPLEADO a ADMIN con éxito")
        void testCambiarRol_aAdmin_exitoso() {
            // GIVEN
            Empleado empleado = buildEmpleado(5L);
            empleado.getUsuario().setRol(Rol.EMPLEADO);

            given(empleadoRepository.findById(5L)).willReturn(Optional.of(empleado));
            given(empleadoMapper.toResponseDTO(any())).willReturn(buildResponseDTO(5L));

            // WHEN
            empleadoService.cambiarRol(5L, "ADMIN");

            // THEN - Capturamos el argumento real enviado al repositorio
            ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(usuarioCaptor.capture());

            Usuario usuarioGuardado = usuarioCaptor.getValue();

            // Evaluamos las propiedades individualmente de forma limpia
            assertThat(usuarioGuardado).isNotNull();
            assertThat(usuarioGuardado.getRol()).isEqualTo(Rol.ADMIN);
        }

        @Test
        @DisplayName("Debería lanzar BusinessRuleException si el rol proporcionado no es válido")
        void testCambiarRol_rolInvalido_lanzaExcepcion() {
            // GIVEN
            Empleado empleado = buildEmpleado(5L);
            given(empleadoRepository.findById(5L)).willReturn(Optional.of(empleado));

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.cambiarRol(5L, "SUPERADMIN"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("rol");
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 6. RESTRICCIONES DE ELIMINACIÓN
    // ═════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Restricciones de eliminación")
    class RestriccionesEliminacion {

        @Test
        @DisplayName("Debería bloquear la eliminación si el empleado tiene ventas registradas")
        void testEliminarEmpleado_conVentas_bloqueado() {
            // GIVEN
            Empleado empleado = buildEmpleado(5L);
            given(empleadoRepository.findById(5L)).willReturn(Optional.of(empleado));
            given(empleadoRepository.tieneVentasRegistradas(5L)).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.eliminarEmpleado(5L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("com/algedro/venta");

            verify(empleadoRepository, never()).delete(any());
            verify(usuarioRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Debería recomendar desactivación en lugar de eliminación en el mensaje de error")
        void testEliminarEmpleado_conVentas_mensajeSugiereDesactivar() {
            // GIVEN
            Empleado empleado = buildEmpleado(5L);
            given(empleadoRepository.findById(5L)).willReturn(Optional.of(empleado));
            given(empleadoRepository.tieneVentasRegistradas(5L)).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.eliminarEmpleado(5L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("desactivación");
        }

        @Test
        @DisplayName("Debería bloquear la eliminación si el empleado tiene movimientos de stock registrados")
        void testEliminarEmpleado_conMovimientosStock_bloqueado() {
            // GIVEN
            Empleado empleado = buildEmpleado(5L);
            given(empleadoRepository.findById(5L)).willReturn(Optional.of(empleado));
            given(empleadoRepository.tieneVentasRegistradas(5L)).willReturn(false);
            given(empleadoRepository.tieneMovimientosStock(5L)).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.eliminarEmpleado(5L))
                    .isInstanceOf(BusinessRuleException.class);

            verify(empleadoRepository, never()).delete(any());
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 7. VALIDACIONES DE SALARIO
    // ═════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Validaciones de salario")
    class ValidacionesSalario {

        @Test
        @DisplayName("Debería rechazar salario negativo o cero al crear empleado")
        void testCrearEmpleado_salarioNegativo_lanzaExcepcion() {
            // GIVEN
            EmpleadoCreateRequestDTO request = buildCreateRequest();
            request.setSalario(-100.00);

            given(usuarioRepository.existsByUsername("nuevo_empleado")).willReturn(false);

            // WHEN / THEN
            assertThatThrownBy(() -> empleadoService.crearEmpleado(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("salario");
        }

        @Test
        @DisplayName("Debería aceptar salario null (campo opcional según modelo de datos)")
        void testCrearEmpleado_salarioNulo_esValido() {
            // GIVEN
            EmpleadoCreateRequestDTO request = buildCreateRequest();
            request.setSalario(null); // salario es nullable en Data-Model.md §1.2

            given(usuarioRepository.existsByUsername(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("$2a$10$hash");
            given(usuarioRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(empleadoRepository.save(any())).willReturn(buildEmpleado(1L));
            given(empleadoMapper.toResponseDTO(any())).willReturn(buildResponseDTO(1L));

            // WHEN / THEN — no lanza excepción
            EmpleadoResponseDTO resultado = empleadoService.crearEmpleado(request);
            assertThat(resultado).isNotNull();
        }
    }
}