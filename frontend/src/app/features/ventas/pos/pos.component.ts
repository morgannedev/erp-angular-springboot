import { CommonModule } from "@angular/common";
import { Component, OnInit, inject, OnDestroy } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Subscription, debounceTime, Subject } from "rxjs";

import { AuthService } from "../../../core/auth/auth.service";
import { Producto } from "../../../core/models/producto.model";
import { VentaDetailResponse } from "../../../core/models/venta.model";
import { ClienteQuickCreateComponent } from "../../clientes/cliente-quick-create/cliente-quick-create.component";
import { ProductoService } from "../../productos/producto.service";
import { VentaService } from "../venta.service";
import jsPDF from "jspdf";
import html2canvas from "html2canvas";
import { ClienteService } from "../../clientes/cliente.service";
import { Cliente } from "../../../core/models/cliente.model";
import { EmpleadoService } from "../../../core/services/empleado.service";

interface LineaPos {
  producto: Producto;
  cantidad: number;
  descuentoLinea: number;
}

@Component({
  selector: "app-pos",
  standalone: true,
  imports: [CommonModule, FormsModule, ClienteQuickCreateComponent],
  templateUrl: "./pos.component.html",
  styleUrls: ["./pos.component.css"],
})
export class PosComponent implements OnInit, OnDestroy {
  private readonly productosService = inject(ProductoService);
  private readonly ventasService = inject(VentaService);
  private readonly empleadoService = inject(EmpleadoService);
  readonly auth = inject(AuthService);

  hayDescuentosSolapados = false;

  private readonly clienteService = inject(ClienteService);
  private busquedaSubscription?: Subscription;
  private searchSubject = new Subject<string>();

  // Estado de la UI
  busqueda = "";
  resultados: Producto[] = [];
  lineas: LineaPos[] = [];
  metodoPago: "EFECTIVO" | "TARJETA" | "OTRO" = "EFECTIVO";
  descuentoGlobal = 0;
  
  forzarSinStock = false;
  clienteId: number | null = null;
  mostrarClienteRapido = false;
  ticket: VentaDetailResponse | null = null;
  error = "";

  // Loading states
  isLoadingProductos = false;

  busquedaCliente = "";
  clientesResultados: Cliente[] = [];
  mostrarResultadosClientes = false;
  private clienteSearchSubject = new Subject<string>();

  ngOnInit(): void {
    // ✅ Debounce de 300ms para búsqueda de productos
    this.searchSubject.pipe(debounceTime(300)).subscribe((termino) => {
      if (termino.trim().length >= 2) {
        this.isLoadingProductos = true;
        this.productosService.buscar(termino);
      } else if (termino.trim().length === 0) {
        this.resultados = [];
      }
    });

    // Suscribirse a resultados de búsqueda
    this.busquedaSubscription =
      this.productosService.resultadosBusqueda$.subscribe((page) => {
        this.isLoadingProductos = false;
        if (page) {
          this.resultados = page.contenido || [];
        }
      });

    // Búsqueda de clientes con debounce
    this.clienteSearchSubject.pipe(debounceTime(300)).subscribe((termino) => {
      if (termino.trim().length >= 2) {
        this.clienteService.listar({ q: termino, tamanio: 10 }).subscribe({
          next: (page) => {
            this.clientesResultados = page.contenido || [];
            this.mostrarResultadosClientes = true;
          },
          error: (err) => console.error("Error buscando clientes:", err),
        });
      } else {
        this.clientesResultados = [];
        this.mostrarResultadosClientes = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.busquedaSubscription?.unsubscribe();
  }

  // ✅ Búsqueda con debounce
  buscarProductos(): void {
    this.searchSubject.next(this.busqueda);
  }

  buscarClientes(): void {
    this.clienteSearchSubject.next(this.busquedaCliente);
  }

  // Método para obtener el nombre del empleado
  private async enrichVentaConEmpleado(
    venta: VentaDetailResponse,
  ): Promise<VentaDetailResponse> {
    return new Promise((resolve) => {
      this.empleadoService.obtener(venta.empleadoId).subscribe({
        next: (empleado) => {
          venta.empleadoNombre =
            `${empleado.nombre} ${empleado.apellidos || ""}`.trim();
          resolve(venta);
        },
        error: (err) => {
          console.error("Error obteniendo empleado:", err);
          venta.empleadoNombre = `Empleado #${venta.empleadoId}`;
          resolve(venta);
        },
      });
    });
  }

  seleccionarCliente(cliente: Cliente): void {
    this.clienteId = cliente.id;
    this.busquedaCliente = `${cliente.nombre} ${cliente.apellidos || ""} - ${cliente.telefono}`;
    this.clientesResultados = [];
    this.mostrarResultadosClientes = false;
  }

  limpiarCliente(): void {
    this.clienteId = null;
    this.busquedaCliente = "";
    this.clientesResultados = [];
  }

  // ✅ Búsqueda por código de barras (sin debounce)
  buscarBarcode(): void {
    if (!this.busqueda.trim()) {
      this.error = "Ingresa un código de barras";
      return;
    }

    this.isLoadingProductos = true;
    this.productosService.buscarPorBarcode(this.busqueda.trim()).subscribe({
      next: (producto) => {
        this.agregar(producto);
        this.busqueda = ""; // Limpiar después de agregar
        this.isLoadingProductos = false;
      },
      error: () => {
        this.error = "❌ Producto no encontrado";
        this.isLoadingProductos = false;
        setTimeout(() => (this.error = ""), 3000);
      },
    });
  }

  exportarTicketPDF(): void {
    const ticketElement = document.querySelector(".ticket") as HTMLElement;
    if (!ticketElement) return;

    this.error = "";

    html2canvas(ticketElement, {
      scale: 2,
      backgroundColor: "#ffffff",
      logging: false,
    })
      .then((canvas) => {
        const imgData = canvas.toDataURL("image/png");
        const pdf = new jsPDF({
          unit: "mm",
          format: "a4",
          orientation: "portrait",
        });
        const imgWidth = 190;
        const imgHeight = (canvas.height * imgWidth) / canvas.width;
        pdf.addImage(imgData, "PNG", 10, 10, imgWidth, imgHeight);
        pdf.save(`ticket_${this.ticket?.numeroVenta || "venta"}.pdf`);
      })
      .catch((err) => {
        console.error("Error generando PDF:", err);
        this.error = "No se pudo generar el PDF";
        setTimeout(() => (this.error = ""), 3000);
      });
  }

  ocultarResultadosClientes(): void {
    setTimeout(() => {
      this.mostrarResultadosClientes = false;
    }, 200);
  }

  // Agregar producto al carrito
  agregar(producto: Producto): void {
    // Validar stock (si no se fuerza)
    if (!this.forzarSinStock && producto.stockActual <= 0) {
      this.error = `${producto.nombre} no tiene stock disponible`;
      this.ocultarResultadosClientes();
      return;
    }

    const existente = this.lineas.find(
      (linea) => linea.producto.id === producto.id,
    );
    if (existente) {
      const nuevaCantidad = existente.cantidad + 1;
      // Validar stock si no se fuerza
      if (!this.forzarSinStock && nuevaCantidad > producto.stockActual) {
        this.error = `Stock insuficiente para ${producto.nombre}. Disponible: ${producto.stockActual}`;
        setTimeout(() => (this.error = ""), 3000);
        return;
      }
      existente.cantidad = nuevaCantidad;
    } else {
      this.lineas.push({ producto, cantidad: 1, descuentoLinea: 0 });
    }
    this.error = "";
  }

  quitar(index: number): void {
    this.lineas.splice(index, 1);
  }

  // ✅ Actualizar cantidad validando stock
  actualizarCantidad(linea: LineaPos, nuevaCantidad: number): void {
    let cantidad = Number(nuevaCantidad);

    if (isNaN(nuevaCantidad) || nuevaCantidad < 1) {
      nuevaCantidad = 1;
    }
    if (!this.forzarSinStock && nuevaCantidad > linea.producto.stockActual) {
      this.error = `Stock insuficiente. Máximo: ${linea.producto.stockActual}`;
      setTimeout(() => (this.error = ""), 3000);
      return;
    }
    linea.cantidad = nuevaCantidad;
    this.verificarSolapamientoDescuentos(); // Re-verificar descuentos
  }

  totalLinea(linea: LineaPos): number {
    const subtotal = linea.producto.precioVenta * linea.cantidad;
    return Math.max(0, subtotal - (linea.descuentoLinea || 0));
  }

  subtotal(): number {
    return this.lineas.reduce(
      (sum, linea) => sum + linea.producto.precioVenta * linea.cantidad,
      0,
    );
  }

  // Método para verificar solapamiento de descuentos
  verificarSolapamientoDescuentos(): void {
    const hayDescuentosLinea = this.lineas.some(
      (l) => (l.descuentoLinea || 0) > 0,
    );
    const hayDescuentoGlobal = (this.descuentoGlobal || 0) > 0;
    this.hayDescuentosSolapados = hayDescuentosLinea && hayDescuentoGlobal;

    if (this.hayDescuentosSolapados) {
      this.error =
        "No se pueden aplicar descuentos por línea y descuento global simultáneamente";
    }
  }

  // ✅ Total con descuento global
  total(): number {
    this.verificarSolapamientoDescuentos();

    const totalConDescuentosLinea = this.lineas.reduce(
      (sum, linea) => sum + this.totalLinea(linea),
      0,
    );
    const descuentoGlobalAplicado = this.descuentoGlobal || 0;
    // Validar que el descuento global no sea mayor al subtotal
    if (descuentoGlobalAplicado > totalConDescuentosLinea) {
      this.descuentoGlobal = totalConDescuentosLinea;
      return 0;
    }
    return Math.max(0, totalConDescuentosLinea - descuentoGlobalAplicado);
  }

  // Finalizar venta
  finalizar(): void {
    // Validaciones
    if (this.lineas.length === 0) {
      this.error = "Agrega productos al carrito";
      return;
    }

    // Validar solapamiento de descuentos
    if (this.hayDescuentosSolapados) {
      this.error =
        "No se pueden aplicar ambos tipos de descuento (por línea y global)";
      setTimeout(() => (this.error = ""), 4000);
      return;
    }

    // Verificar stock antes de finalizar
    if (!this.forzarSinStock) {
      const sinStock = this.lineas.filter(
        (linea) => linea.cantidad > linea.producto.stockActual,
      );
      if (sinStock.length > 0) {
        this.error = `❌ Stock insuficiente: ${sinStock.map((l) => l.producto.nombre).join(", ")}`;
        setTimeout(() => (this.error = ""), 4000);
        return;
      }
    }

    this.error = "";

    this.ventasService
      .crear({
        clienteId: this.clienteId,
        metodoPago: this.metodoPago,
        descuentoGlobal: this.descuentoGlobal || 0,
        forzarSinStock: this.forzarSinStock,
        lineas: this.lineas.map((linea) => ({
          productoId: linea.producto.id,
          cantidad: linea.cantidad,
          descuentoLinea: linea.descuentoLinea || 0,
        })),
      })
      .subscribe({
        next: async (venta) => {
          const ventaConEmpleado = await this.enrichVentaConEmpleado(venta);

          this.ticket = venta;
          this.lineas = [];
          this.descuentoGlobal = 0;
          this.clienteId = null;

          // Auto-cerrar ticket después de 10 segundos
          setTimeout(() => {
            if (this.ticket) {
              this.ticket = null;
            }
          }, 10000);

          // Scroll al ticket
          setTimeout(() => {
            const ticketElement = document.querySelector(".ticket");
            ticketElement?.scrollIntoView({ behavior: "smooth" });
          }, 100);
        },
        error: (err) => {
          console.error("Error al crear venta:", err);

          let mensajeError = "No se pudo finalizar la venta";

          if (err.error?.mensaje) {
            mensajeError = err.error.mensaje;
          } else if (err.error?.message) {
            mensajeError = err.error.message;
          } else if (err.message) {
            mensajeError = err.message;
          }

          if (mensajeError.includes("inactivo")) {
            mensajeError = mensajeError;
          } else if (mensajeError.includes("Stock insuficiente")) {
            mensajeError = mensajeError;
          } else if (mensajeError.includes("descuentos")) {
            mensajeError = mensajeError;
          }

          this.error = mensajeError;
          setTimeout(() => {
            if (this.error === mensajeError) {
              this.error = "";
            }
          }, 5000);
        },
      });
  }

  // ✅ Limpiar carrito
  limpiarCarrito(): void {
    if (
      this.lineas.length > 0 &&
      confirm("¿Eliminar todos los productos del carrito?")
    ) {
      this.lineas = [];
      this.descuentoGlobal = 0;
      this.clienteId = null;
    }
  }
}
