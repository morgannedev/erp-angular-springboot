package com.algedro.venta;

import com.algedro.categoria.entity.Categoria;
import com.algedro.categoria.repository.CategoriaRepository;
import com.algedro.empleado.entity.Empleado;
import com.algedro.empleado.repository.EmpleadoRepository;
import com.algedro.producto.entity.Producto;
import com.algedro.producto.repository.ProductoRepository;
import com.algedro.stock.repository.MovimientoStockRepository;
import com.algedro.usuario.Rol;
import com.algedro.usuario.Usuario;
import com.algedro.usuario.UsuarioRepository;
import com.algedro.venta.dto.VentaLineRequest;
import com.algedro.venta.dto.VentaRequest;
import com.algedro.venta.repository.DetalleVentaRepository;
import com.algedro.venta.repository.VentaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Transactional
@DisplayName("VentaIntegrationTest — Flujos de Integración Reales con Base de Datos")
class VentaIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("erp_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private DetalleVentaRepository detalleVentaRepository;
    @Autowired private VentaRepository ventaRepository;
    @Autowired private MovimientoStockRepository movimientoStockRepository;

    private Producto productoIntegracion;

    @BeforeEach
    void setupData() {
        // Orden de borrado respetando FK: movimientos → detalle → venta → producto → categoria
        movimientoStockRepository.deleteAll();
        detalleVentaRepository.deleteAll();
        ventaRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        empleadoRepository.deleteAll();
        usuarioRepository.deleteAll();

        crearEmpleado("empleado_caja", Rol.EMPLEADO);
        crearEmpleado("admin_central", Rol.ADMIN);

        // Categoria mínima requerida por el NOT NULL de productos.categoria_id
        Categoria categoria = categoriaRepository.save(Categoria.builder()
                .nombre("Test")
                .activo(true)
                .build());

        productoIntegracion = productoRepository.save(Producto.builder()
                .referencia("SKU-INT-001")
                .nombre("Producto Integración")
                .precioVenta(new BigDecimal("89.90"))
                .unidadMedida("ud")
                .stockActual(10)
                .stockMinimo(2)
                .activo(true)
                .categoria(categoria)
                .build());
    }

    private void crearEmpleado(String username, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash("noop");
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario = usuarioRepository.save(usuario);

        Empleado empleado = new Empleado();
        empleado.setUsuario(usuario);
        empleado.setNombre(username);
        empleado.setApellidos("Test");
        empleado.setCargo(rol.name());
        empleado.setFechaContratacion(LocalDate.now());
        empleadoRepository.save(empleado);
    }

    @Test
    @WithMockUser(username = "empleado_caja", roles = "EMPLEADO")
    @DisplayName("testFlujoPOSCompleto_Testcontainers → Creación de venta descuenta stock y escribe auditorías físicas")
    void testFlujoPOSCompleto_Testcontainers() throws Exception {
        VentaLineRequest linea = VentaLineRequest.builder()
                .productoId(productoIntegracion.getId())
                .cantidad(2)
                .build();

        VentaRequest request = VentaRequest.builder()
                .lineas(List.of(linea))
                .metodoPago("TARJETA")
                .build();

        mockMvc.perform(post("/ventas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Producto prodActualizado = productoRepository.findById(productoIntegracion.getId()).orElseThrow();
        assertThat(prodActualizado.getStockActual()).isEqualTo(8); // 10 - 2
        assertThat(movimientoStockRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "admin_central", roles = "ADMIN")
    @DisplayName("testFlujAnulacionCompleto_Testcontainers → Anulación por parte de ADMIN reintegra stocks físicos")
    void testFlujAnulacionCompleto_Testcontainers() throws Exception {
        VentaLineRequest linea = VentaLineRequest.builder()
                .productoId(productoIntegracion.getId())
                .cantidad(5)
                .build();

        VentaRequest request = VentaRequest.builder()
                .lineas(List.of(linea))
                .metodoPago("EFECTIVO")
                .build();

        String responseContent = mockMvc.perform(post("/ventas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> ventaCreada = objectMapper.readValue(responseContent, Map.class);
        Long idVenta = Long.valueOf(ventaCreada.get("id").toString());

        Map<String, String> anularBody = Map.of("motivoAnulacion", "Devolución por garantía");

        mockMvc.perform(post("/ventas/" + idVenta + "/anular")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(anularBody)))
                .andExpect(status().isOk());

        Producto prodRevertido = productoRepository.findById(productoIntegracion.getId()).orElseThrow();
        assertThat(prodRevertido.getStockActual()).isEqualTo(10); // stock inicial restaurado
    }
}
