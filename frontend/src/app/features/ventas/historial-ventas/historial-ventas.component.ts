import { CommonModule } from "@angular/common";
import { Component, OnInit, inject } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { RouterLink } from "@angular/router";

import { AuthService } from "../../../core/auth/auth.service";
import {
  VentaDetailResponse,
  VentaPage,
} from "../../../core/models/venta.model";
import { Empleado } from "../../../core/models/empleado.model";
import { VentaService } from "../venta.service";
import { EmpleadoService } from "../../../core/services/empleado.service";
import jsPDF from "jspdf";
import html2canvas from "html2canvas";

@Component({
  selector: "app-historial-ventas",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: "./historial-ventas.component.html",
  styleUrls: ["./historial-ventas.component.css"],
})
export class HistorialVentasComponent implements OnInit {
  private readonly ventasService = inject(VentaService);
  private readonly empleadoService = inject(EmpleadoService);
  readonly auth = inject(AuthService);

  // Historial
  historial: VentaDetailResponse[] = [];
  isLoading = false;
  error = "";

  // Filtros
  filtroDesde = "";
  filtroHasta = "";
  filtroMetodo = "";
  filtroEmpleadoId: number | null = null;
  filtroEstado = "";

  // Paginación
  paginaActual = 0;
  totalPaginas = 0;
  totalElementos = 0;
  tamanio = 25;

  // Empleados para filtro (solo admin)
  empleados: Empleado[] = [];

  // Modal detalle
  ventaSeleccionada: VentaDetailResponse | null = null;

  ngOnInit(): void {
    this.cargarHistorial();
    if (this.auth.isAdmin()) {
      this.cargarEmpleados();
    }
  }

  cargarEmpleados(): void {
    this.empleadoService.listar("", true).subscribe({
      next: (page: any) => {
        this.empleados = page.content || page.contenido || [];
      },
      error: (err: any) => console.error("Error cargando empleados:", err),
    });
  }

  cargarHistorial(): void {
    this.isLoading = true;
    this.error = "";

    const params: any = {
      page: this.paginaActual,
      size: this.tamanio,
    };

    if (this.filtroDesde) params.desde = this.filtroDesde;
    if (this.filtroHasta) params.hasta = this.filtroHasta;
    if (this.filtroMetodo) params.metodoPago = this.filtroMetodo;
    if (this.filtroEstado) params.estado = this.filtroEstado;
    if (this.auth.isAdmin() && this.filtroEmpleadoId) {
      params.empleadoId = this.filtroEmpleadoId;
    }

    this.ventasService.listar(params).subscribe({
      next: (page: VentaPage) => {
        this.historial = page.content || [];
        this.totalPaginas = page.totalPages || 0;
        this.totalElementos = page.totalElements || 0;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error("Error cargando historial:", err);
        this.error = "No se pudo cargar el historial de ventas";
        this.isLoading = false;
        setTimeout(() => (this.error = ""), 5000);
      },
    });
  }

  cambiarPagina(pagina: number): void {
    if (pagina >= 0 && pagina < this.totalPaginas) {
      this.paginaActual = pagina;
      this.cargarHistorial();
    }
  }

  aplicarFiltros(): void {
    this.paginaActual = 0;
    this.cargarHistorial();
  }

  limpiarFiltros(): void {
    this.filtroDesde = "";
    this.filtroHasta = "";
    this.filtroMetodo = "";
    this.filtroEmpleadoId = null;
    this.filtroEstado = "";
    this.paginaActual = 0;
    this.cargarHistorial();
  }

  verDetalle(venta: VentaDetailResponse): void {
    this.ventaSeleccionada = venta;
  }

  cerrarDetalle(): void {
    this.ventaSeleccionada = null;
  }

  async descargarTicketPDF(venta: VentaDetailResponse): Promise<void> {
    const ticketHTML = this.generarTicketHTML(venta);
    const tempDiv = document.createElement("div");
    tempDiv.innerHTML = ticketHTML;
    tempDiv.style.position = "absolute";
    tempDiv.style.left = "-9999px";
    tempDiv.style.top = "-9999px";
    document.body.appendChild(tempDiv);

    try {
      const canvas = await html2canvas(tempDiv, {
        scale: 2,
        backgroundColor: "#ffffff",
        logging: false,
      });
      const imgData = canvas.toDataURL("image/png");
      const pdf = new jsPDF({
        unit: "mm",
        format: "a4",
        orientation: "portrait",
      });
      const imgWidth = 190;
      const imgHeight = (canvas.height * imgWidth) / canvas.width;
      pdf.addImage(imgData, "PNG", 10, 10, imgWidth, imgHeight);
      pdf.save(`ticket_${venta.numeroVenta}.pdf`);
    } catch (err) {
      console.error("Error generando PDF:", err);
      this.error = "No se pudo generar el PDF";
      setTimeout(() => (this.error = ""), 3000);
    } finally {
      document.body.removeChild(tempDiv);
    }
  }

  private generarTicketHTML(venta: VentaDetailResponse): string {
    return `
      <div style="font-family: monospace; width: 300px; padding: 16px;">
        <div style="text-align: center; border-bottom: 1px dashed #ccc; padding-bottom: 8px;">
          <h2 style="margin: 0;">ALGEDRO S.L.</h2>
          <p>Ticket de venta</p>
        </div>
        <div style="padding: 12px 0;">
          <p><strong>${venta.numeroVenta}</strong> · ${new Date(venta.fecha).toLocaleString()}</p>
          ${venta.empleadoNombre ? `<p><i>Atendido por: ${venta.empleadoNombre}</i></p>` : ""}
          <div style="margin: 12px 0;">
            ${(venta.lineas || [])
              .map(
                (linea: any) => `
              <div style="display: flex; justify-content: space-between; font-size: 12px;">
                <span>${linea.nombreProducto} x${linea.cantidad}</span>
                <span>${linea.subtotalLinea.toFixed(2)}€</span>
              </div>
            `,
              )
              .join("")}
          </div>
          ${
            venta.descuentoGlobal > 0
              ? `
            <div style="display: flex; justify-content: space-between; font-size: 12px;">
              <span>Descuento global:</span>
              <span>-${venta.descuentoGlobal.toFixed(2)}€</span>
            </div>
          `
              : ""
          }
          <div style="border-top: 1px dashed #ccc; padding-top: 8px; text-align: right;">
            <strong>Total: ${venta.total.toFixed(2)}€</strong>
          </div>
          <div style="text-align: center; font-size: 10px; margin-top: 12px;">
            Pago: ${venta.metodoPago}
          </div>
        </div>
      </div>
    `;
  }

  anular(venta: VentaDetailResponse): void {
    if (!this.auth.isAdmin()) {
      this.error = "Solo administradores pueden anular ventas";
      return;
    }

    const motivoAnulacion = prompt("Motivo de anulación:");
    if (!motivoAnulacion || motivoAnulacion.trim() === "") return;

    this.ventasService.anular(venta.id, motivoAnulacion).subscribe({
      next: () => {
        this.cargarHistorial();
        this.cerrarDetalle();
        this.error = "";
      },
      error: () => {
        this.error = "No se pudo anular la venta";
        setTimeout(() => (this.error = ""), 3000);
      },
    });
  }

  exportarCsv(): void {
    this.ventasService
      .exportarCsv({
        desde: this.filtroDesde || undefined,
        hasta: this.filtroHasta || undefined,
      })
      .subscribe({
        next: (blob: Blob) => {
          const url = URL.createObjectURL(blob);
          const link = document.createElement("a");
          link.href = url;
          link.download = `ventas_${new Date().toISOString().slice(0, 19).replace(/:/g, "-")}.csv`;
          link.click();
          URL.revokeObjectURL(url);
        },
        error: () => {
          this.error = "No se pudo exportar el CSV";
          setTimeout(() => (this.error = ""), 3000);
        },
      });
  }
}
