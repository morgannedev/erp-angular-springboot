package com.algedro.cliente;

import com.algedro.cliente.entity.Cliente;
import com.algedro.cliente.dto.ClienteRequest;
import com.algedro.cliente.dto.ClienteResponse;
import com.algedro.cliente.mapper.ClienteMapper;
import com.algedro.cliente.service.ClienteService;
import com.algedro.exception.BusinessRuleException;
import com.algedro.exception.ConflictException;
import com.algedro.exception.ResourceNotFoundException;
import com.algedro.cliente.repository.ClienteRepository;
import com.algedro.venta.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests unitarios para ClienteService — ciclo TDD RED.
 *
 * Ejecutar con: mvn test -Dtest=ClienteServiceTest
 * Todos deben FALLAR hasta que se implemente ClienteService (paso 6.5).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService — Tests unitarios")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private VentaRepository ventaRepository;

    @Spy
    private ClienteMapper clienteMapper = new ClienteMapper();

    @InjectMocks
    private ClienteService clienteService;

    // ── Fixtures ──────────────────────────────────────────────

    private Cliente clienteActivo;
    private Cliente clienteInactivo;

    @BeforeEach
    void setUp() {
        clienteActivo = Cliente.builder()
                .id(1L)
                .nombre("Lucía")
                .apellidos("Torres Méndez")
                .telefono("612555101")
                .email("lucia.torres@email.com")
                .nif("12345678A")
                .activo(true)
                .fechaAlta(LocalDate.of(2023, 2, 10))
                .build();

        clienteInactivo = Cliente.builder()
                .id(2L)
                .nombre("Roberto")
                .apellidos("Díaz Peral")
                .telefono("623444202")
                .activo(false)
                .fechaAlta(LocalDate.of(2024, 6, 20))
                .build();
    }

    // ══════════════════════════════════════════════════════════
    // CREAR CLIENTE
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("crear()")
    class Crear {

        /**
         * CASO OBLIGATORIO del spec §3.5:
         * "El formulario de creación rápida desde el POS requiere únicamente
         * nombre y teléfono como campos obligatorios; el resto son opcionales."
         */
        @Test
        @DisplayName("testCreacionRapida_soloNombreYTelefono_exitoso — mínimo válido → 201")
        void testCreacionRapida_soloNombreYTelefono_exitoso() {
            // GIVEN — request mínimo: solo nombre y teléfono (el resto null)
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Ana")
                    .telefono("666000111")
                    .build();

            Cliente clienteGuardado = Cliente.builder()
                    .id(10L)
                    .nombre("Ana")
                    .telefono("666000111")
                    .activo(true)
                    .fechaAlta(LocalDate.now())
                    .build();

            given(clienteRepository.save(any(Cliente.class))).willReturn(clienteGuardado);

            // WHEN
            ClienteResponse response = clienteService.crear(request);

            // THEN — se guarda y devuelve el cliente con solo los campos básicos
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getNombre()).isEqualTo("Ana");
            assertThat(response.getTelefono()).isEqualTo("666000111");
            assertThat(response.getNif()).isNull();
            assertThat(response.getEmail()).isNull();
            then(clienteRepository).should().save(any(Cliente.class));
        }

        @Test
        @DisplayName("crear_completo_exitoso — todos los campos → guarda correctamente")
        void testCrear_completo_exitoso() {
            // GIVEN
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Lucía")
                    .apellidos("Torres Méndez")
                    .telefono("612555101")
                    .email("lucia@email.com")
                    .nif("12345678A")
                    .direccion("Calle Mayor 1")
                    .ciudad("Madrid")
                    .notas("Cliente habitual de limpieza")
                    .build();

            given(clienteRepository.existsByNif("12345678A")).willReturn(false);
            given(clienteRepository.existsByEmail("lucia@email.com")).willReturn(false);
            given(clienteRepository.save(any(Cliente.class))).willReturn(clienteActivo);

            // WHEN
            ClienteResponse response = clienteService.crear(request);

            // THEN
            assertThat(response).isNotNull();
            assertThat(response.getNombre()).isEqualTo("Lucía");
            then(clienteRepository).should().save(any(Cliente.class));
        }

        /**
         * CASO OBLIGATORIO del spec §3.5:
         * "El NIF/NIE, si se introduce, es único en el sistema;
         * se muestra error si se intenta duplicar."
         */
        @Test
        @DisplayName("testCrear_nifDuplicado_409 — NIF ya registrado → ConflictException")
        void testCrear_nifDuplicado_409() {
            // GIVEN
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Pedro")
                    .telefono("677888999")
                    .nif("12345678A") // NIF ya usado por clienteActivo
                    .build();

            given(clienteRepository.existsByNif("12345678A")).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> clienteService.crear(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("NIF");

            then(clienteRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("testCrear_emailDuplicado_409 — email ya registrado → ConflictException")
        void testCrear_emailDuplicado_409() {
            // GIVEN
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Carmen")
                    .telefono("677000111")
                    .email("lucia.torres@email.com") // email ya existente
                    .build();

            given(clienteRepository.existsByEmail("lucia.torres@email.com")).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> clienteService.crear(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("email");

            then(clienteRepository).should(never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════
    // ACTUALIZAR CLIENTE
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("actualizar()")
    class Actualizar {

        @Test
        @DisplayName("actualizar_exitoso — datos válidos → devuelve cliente actualizado")
        void testActualizar_exitoso() {
            // GIVEN
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Lucía")
                    .apellidos("Torres Méndez Actualizada")
                    .telefono("612555101")
                    .build();

            given(clienteRepository.findById(1L)).willReturn(Optional.of(clienteActivo));
            given(clienteRepository.save(any(Cliente.class))).willAnswer(inv -> inv.getArgument(0));

            // WHEN
            ClienteResponse response = clienteService.actualizar(1L, request);

            // THEN
            assertThat(response.getApellidos()).isEqualTo("Torres Méndez Actualizada");
            then(clienteRepository).should().save(any(Cliente.class));
        }

        @Test
        @DisplayName("actualizar_nifDuplicadoDeOtroCliente_409 — NIF pertenece a otro → ConflictException")
        void testActualizar_nifDuplicadoDeOtroCliente_409() {
            // GIVEN — nif "99999999Z" ya pertenece a otro cliente (id != 1)
            ClienteRequest request = ClienteRequest.builder()
                    .nombre("Lucía")
                    .telefono("612555101")
                    .nif("99999999Z")
                    .build();

            given(clienteRepository.findById(1L)).willReturn(Optional.of(clienteActivo));
            given(clienteRepository.existsByNifAndIdNot("99999999Z", 1L)).willReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> clienteService.actualizar(1L, request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("NIF");
        }

        @Test
        @DisplayName("actualizar_noExiste_404 → ResourceNotFoundException")
        void testActualizar_noExiste_404() {
            // GIVEN
            given(clienteRepository.findById(999L)).willReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> clienteService.actualizar(999L, ClienteRequest.builder()
                    .nombre("X").telefono("000").build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════
    // ELIMINAR CLIENTE
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("eliminar()")
    class Eliminar {

        /**
         * CASO OBLIGATORIO del spec §3.5:
         * "La eliminación de un cliente con ventas asociadas está bloqueada;
         * el sistema sugiere desactivarlo en su lugar."
         */
        @Test
        @DisplayName("testEliminar_conVentas_bloqueado — cliente con historial de ventas → BusinessRuleException")
        void testEliminar_conVentas_bloqueado() {
            // GIVEN — el cliente tiene ventas registradas
            given(clienteRepository.findById(1L)).willReturn(Optional.of(clienteActivo));
            given(ventaRepository.existsByClienteId(1L)).willReturn(true);

            // WHEN / THEN — debe bloquearse con sugerencia de desactivar
            assertThatThrownBy(() -> clienteService.eliminar(1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("desactivar");

            // El registro NO debe eliminarse de BD
            then(clienteRepository).should(never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("eliminar_sinVentas_exitoso — cliente limpio → se elimina correctamente")
        void testEliminar_sinVentas_exitoso() {
            // GIVEN — cliente sin ventas asociadas
            given(clienteRepository.findById(1L)).willReturn(Optional.of(clienteActivo));
            given(ventaRepository.existsByClienteId(1L)).willReturn(false);
            willDoNothing().given(clienteRepository).deleteById(1L);

            // WHEN
            clienteService.eliminar(1L);

            // THEN
            then(clienteRepository).should().deleteById(1L);
        }

        @Test
        @DisplayName("eliminar_noExiste_404 → ResourceNotFoundException")
        void testEliminar_noExiste_404() {
            // GIVEN
            given(clienteRepository.findById(999L)).willReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> clienteService.eliminar(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════
    // CAMBIAR ESTADO (activar / desactivar)
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("cambiarEstado()")
    class CambiarEstado {

        @Test
        @DisplayName("desactivar_noEliminaRegistro — activo=false → persiste en BD sin borrar")
        void testDesactivar_noEliminaRegistro() {
            // GIVEN
            given(clienteRepository.findById(1L)).willReturn(Optional.of(clienteActivo));
            given(clienteRepository.save(any(Cliente.class))).willAnswer(inv -> inv.getArgument(0));

            // WHEN
            ClienteResponse response = clienteService.cambiarEstado(1L, false);

            // THEN — marcado inactivo pero no borrado
            assertThat(response.isActivo()).isFalse();
            then(clienteRepository).should().save(any(Cliente.class));
            then(clienteRepository).should(never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("reactivar_exitoso — activo=true → devuelve cliente activo")
        void testReactivar_exitoso() {
            // GIVEN
            given(clienteRepository.findById(2L)).willReturn(Optional.of(clienteInactivo));
            given(clienteRepository.save(any(Cliente.class))).willAnswer(inv -> inv.getArgument(0));

            // WHEN
            ClienteResponse response = clienteService.cambiarEstado(2L, true);

            // THEN
            assertThat(response.isActivo()).isTrue();
        }

        @Test
        @DisplayName("cambiarEstado_noExiste_404 → ResourceNotFoundException")
        void testCambiarEstado_noExiste_404() {
            given(clienteRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.cambiarEstado(999L, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════
    // BÚSQUEDA / LISTADO
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listar()")
    class Listar {

        /**
         * CASO OBLIGATORIO del spec §3.5:
         * "Los clientes desactivados no aparecen en las búsquedas del POS
         * ni en listados activos."
         */
        @Test
        @DisplayName("testBuscar_clienteInactivo_noAparece — búsqueda estándar excluye inactivos")
        void testBuscar_clienteInactivo_noAparece() {
            // GIVEN — el repository filtra por activo=true; el inactivo no se incluye
            Page<Cliente> soloActivos = new PageImpl<>(List.of(clienteActivo));
            given(clienteRepository.buscar("Torres", true, PageRequest.of(0, 25)))
                    .willReturn(soloActivos);

            // WHEN
            Page<ClienteResponse> resultado = clienteService.listar("Torres", true, PageRequest.of(0, 25));

            // THEN — solo aparece el activo
            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).getNombre()).isEqualTo("Lucía");
            // El inactivo (Roberto) no aparece en los resultados
            assertThat(resultado.getContent())
                    .noneMatch(c -> c.getNombre().equals("Roberto"));
        }

        @Test
        @DisplayName("buscar_clienteInactivo_aparece_siAdmin_pideInactivos — activo=false visible para ADMIN")
        void testBuscar_clienteInactivo_aparece_siAdmin_pideInactivos() {
            // GIVEN — ADMIN pasa activo=false explícitamente
            Page<Cliente> todos = new PageImpl<>(List.of(clienteInactivo));
            given(clienteRepository.buscar(null, false, PageRequest.of(0, 25)))
                    .willReturn(todos);

            // WHEN
            Page<ClienteResponse> resultado = clienteService.listar(null, false, PageRequest.of(0, 25));

            // THEN — aparece el inactivo
            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).isActivo()).isFalse();
        }

        @Test
        @DisplayName("listar_sinFiltros_devuelveActivos_paginado — default page=0, size=25, activo=true")
        void testListar_sinFiltros_devuelveActivos_paginado() {
            // GIVEN
            List<Cliente> clientes = Collections.nCopies(25, clienteActivo);
            Page<Cliente> pagina = new PageImpl<>(clientes, PageRequest.of(0, 25), 42);
            given(clienteRepository.buscar(null, true, PageRequest.of(0, 25)))
                    .willReturn(pagina);

            // WHEN
            Page<ClienteResponse> resultado = clienteService.listar(null, true, PageRequest.of(0, 25));

            // THEN
            assertThat(resultado.getTotalElements()).isEqualTo(42);
            assertThat(resultado.getContent()).hasSize(25);
        }

        @Test
        @DisplayName("buscar_porTelefono_devuelveCoincidencias")
        void testBuscar_porTelefono() {
            // GIVEN
            Page<Cliente> pagina = new PageImpl<>(List.of(clienteActivo));
            given(clienteRepository.buscar("612", true, PageRequest.of(0, 25)))
                    .willReturn(pagina);

            // WHEN
            Page<ClienteResponse> resultado = clienteService.listar("612", true, PageRequest.of(0, 25));

            // THEN
            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).getTelefono()).isEqualTo("612555101");
        }
    }

    // ══════════════════════════════════════════════════════════
    // GET BY ID
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("getById_activo_200 — ID existente → devuelve cliente")
        void testGetById_activo_200() {
            // GIVEN
            given(clienteRepository.findById(1L)).willReturn(Optional.of(clienteActivo));

            // WHEN
            ClienteResponse response = clienteService.getById(1L);

            // THEN
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getNombre()).isEqualTo("Lucía");
        }

        @Test
        @DisplayName("getById_noExiste_404 → ResourceNotFoundException")
        void testGetById_noExiste_404() {
            // GIVEN
            given(clienteRepository.findById(999L)).willReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> clienteService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("getById_inactivo_siempreVisible — inactivo sí aparece en getById (solo oculto en búsquedas)")
        void testGetById_inactivo_siempreVisible() {
            // GIVEN — getById no filtra por activo; el cliente puede verse aunque esté inactivo
            given(clienteRepository.findById(2L)).willReturn(Optional.of(clienteInactivo));

            // WHEN
            ClienteResponse response = clienteService.getById(2L);

            // THEN — el cliente inactivo es accesible por ID (historial de ventas lo referencia)
            assertThat(response.isActivo()).isFalse();
        }
    }

    // ══════════════════════════════════════════════════════════
    // EXPORTAR CSV
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("exportarCsv()")
    class ExportarCsv {

        @Test
        @DisplayName("exportarCsv_devuelveContenidoCsv — lista todos los clientes en formato CSV")
        void testExportarCsv_devuelveContenidoCsv() {
            // GIVEN
            given(clienteRepository.findAll()).willReturn(List.of(clienteActivo, clienteInactivo));

            // WHEN
            String csv = clienteService.exportarCsv();

            // THEN — cabecera presente y dos filas de datos
            assertThat(csv).contains("nombre", "apellidos", "telefono");
            assertThat(csv).contains("Lucía");
            assertThat(csv).contains("Roberto");
        }

        @Test
        @DisplayName("exportarCsv_listaVacia_devuelveSoloCabecera")
        void testExportarCsv_listaVacia_devuelveSoloCabecera() {
            // GIVEN
            given(clienteRepository.findAll()).willReturn(Collections.emptyList());

            // WHEN
            String csv = clienteService.exportarCsv();

            // THEN
            assertThat(csv).isNotBlank();
            assertThat(csv).contains("nombre"); // Solo la cabecera
            assertThat(csv.lines().count()).isEqualTo(1L);
        }
    }

    // ══════════════════════════════════════════════════════════
    // HISTORIAL DE COMPRAS
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getHistorialCompras()")
    class HistorialCompras {

        @Test
        @DisplayName("getHistorialCompras_clienteExiste_devuelvePaginado")
        void testGetHistorialCompras_clienteExiste() {
            // GIVEN
            given(clienteRepository.findById(1L)).willReturn(Optional.of(clienteActivo));
            given(ventaRepository.findByClienteIdAndFechaBetween(
                    eq(1L), any(), any(), eq(PageRequest.of(0, 25))))
                    .willReturn(Page.empty());

            // WHEN
            var resultado = clienteService.getHistorialCompras(1L, null, null, PageRequest.of(0, 25));

            // THEN
            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("getHistorialCompras_clienteNoExiste_404 → ResourceNotFoundException")
        void testGetHistorialCompras_noExiste_404() {
            // GIVEN
            given(clienteRepository.findById(999L)).willReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> clienteService.getHistorialCompras(999L, null, null, PageRequest.of(0, 25)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}