package cl.duoc.wepay_ms_reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cl.duoc.wepay_ms_reportes.dto.IndicadoresDTO;
import cl.duoc.wepay_ms_reportes.dto.TopProductoDTO;
import cl.duoc.wepay_ms_reportes.dto.VentasPorTramoDTO;
import cl.duoc.wepay_ms_reportes.entity.DetallePedidoReporte;
import cl.duoc.wepay_ms_reportes.entity.PedidoReporte;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests de los cálculos: sin Spring ni base de datos, solo pedidos armados a mano. */
class CalculadoraReportesTest {

    private static final ZoneId CHILE = ZoneId.of("America/Santiago");

    private static long siguienteId = 1;

    /** fecha en UTC, como se guarda en la base. */
    private static PedidoReporte pedido(String estado, double total, String fechaUtc) {
        PedidoReporte p = new PedidoReporte();
        p.setPedidoId(siguienteId++);
        p.setEstado(estado);
        p.setTotal(total);
        p.setFechaCreacion(LocalDateTime.parse(fechaUtc));
        return p;
    }

    private static PedidoReporte conProducto(PedidoReporte p, long productoId, String nombre, double precio, int cantidad) {
        DetallePedidoReporte d = new DetallePedidoReporte();
        d.setProductoId(productoId);
        d.setNombreProducto(nombre);
        d.setPrecioUnitario(precio);
        d.setCantidad(cantidad);
        p.agregarDetalle(d);
        return p;
    }

    @Test
    void laVentaSeCuentaDesdeQueSeConfirmaElPagoNoAlEntregar() {
        List<PedidoReporte> pedidos = List.of(
                pedido("CREADO", 10_000, "2026-11-24T12:00:00"),
                pedido("ACEPTADO", 20_000, "2026-11-24T12:00:00"),
                pedido("DESPACHADO", 30_000, "2026-11-24T12:00:00"),
                pedido("ENTREGADO", 40_000, "2026-11-24T12:00:00"));

        IndicadoresDTO r = CalculadoraReportes.indicadores(pedidos);

        assertEquals(90_000, r.ventasConfirmadas()); // sin el CREADO
        assertEquals(40_000, r.ventasEntregadas());
        assertEquals(3, r.pedidosConfirmados());
        assertEquals(30_000, r.ticketPromedio());
    }

    @Test
    void calculaTasaYMontoDeCancelaciones() {
        List<PedidoReporte> pedidos = List.of(
                pedido("CANCELADO", 15_000, "2026-11-24T12:00:00"),
                pedido("ENTREGADO", 10_000, "2026-11-24T12:00:00"),
                pedido("ACEPTADO", 10_000, "2026-11-24T12:00:00"));

        IndicadoresDTO r = CalculadoraReportes.indicadores(pedidos);

        assertEquals(1, r.pedidosCancelados());
        assertEquals(15_000, r.montoCancelado());
        assertEquals(33.3, r.tasaCancelacion());
        assertEquals(1, r.pedidosActivos()); // solo el ACEPTADO
    }

    @Test
    void sinPedidosTodoEsCeroYNoDividepor0() {
        IndicadoresDTO r = CalculadoraReportes.indicadores(List.of());

        assertEquals(0, r.totalPedidos());
        assertEquals(0, r.ticketPromedio());
        assertEquals(0, r.tasaCancelacion());
    }

    @Test
    void tiempoDeEntregaPromedioEnMinutos() {
        PedidoReporte a = pedido("ENTREGADO", 1, "2026-11-24T10:00:00");
        a.setFechaEntrega(LocalDateTime.parse("2026-11-24T11:00:00"));
        PedidoReporte b = pedido("ENTREGADO", 1, "2026-11-24T10:00:00");
        b.setFechaEntrega(LocalDateTime.parse("2026-11-24T12:00:00"));

        assertEquals(90, CalculadoraReportes.indicadores(List.of(a, b)).leadTimePromedioMinutos());
    }

    @Test
    void masVendidosSoloCuentaPedidosConfirmadosYOrdenaPorUnidades() {
        List<PedidoReporte> pedidos = List.of(
                conProducto(pedido("ACEPTADO", 0, "2026-11-24T12:00:00"), 1, "Teclado", 1000, 1),
                conProducto(pedido("ENTREGADO", 0, "2026-11-24T12:00:00"), 2, "Mouse", 500, 3),
                conProducto(pedido("CANCELADO", 0, "2026-11-24T12:00:00"), 1, "Teclado", 1000, 10),
                conProducto(pedido("CREADO", 0, "2026-11-24T12:00:00"), 1, "Teclado", 1000, 10));

        List<TopProductoDTO> top = CalculadoraReportes.topProductos(pedidos, 5);

        assertEquals(List.of("Mouse", "Teclado"), top.stream().map(TopProductoDTO::nombreProducto).toList());
        assertEquals(3, top.get(0).cantidadVendida());
        assertEquals(1500, top.get(0).ingresosTotales());
        assertEquals(1, top.get(1).cantidadVendida()); // el cancelado y el creado no suman
    }

    @Test
    void cuentaPedidosPorEstado() {
        Map<String, Long> estados = CalculadoraReportes.porEstado(List.of(
                pedido("CREADO", 1, "2026-11-24T12:00:00"),
                pedido("CREADO", 1, "2026-11-24T12:00:00"),
                pedido("ENTREGADO", 1, "2026-11-24T12:00:00")));

        assertEquals(Map.of("CREADO", 2L, "ENTREGADO", 1L), estados);
    }

    @Test
    void porHoraDevuelveTodasLasHorasDelPeriodoConCeros() {
        List<VentasPorTramoDTO> tramos = CalculadoraReportes.ventasPorTramo(
                List.of(pedido("ACEPTADO", 5000, "2026-11-24T13:20:00")),
                LocalDateTime.parse("2026-11-24T12:00:00"),
                LocalDateTime.parse("2026-11-24T15:00:00"),
                CHILE, false);

        assertEquals(3, tramos.size());
        assertEquals("2026-11-24T13:00", tramos.get(1).inicio());
        assertEquals(1, tramos.get(1).pedidos());
        assertEquals(5000, tramos.get(1).ventas());
        assertEquals(0, tramos.get(0).ventas());
    }

    @Test
    void porDiaAgrupaSegunElDiaEnChileNoEnUtc() {
        // 24-nov 23:30 en Chile (UTC-3) = 25-nov 02:30 en UTC: debe contar el 24
        List<VentasPorTramoDTO> tramos = CalculadoraReportes.ventasPorTramo(
                List.of(pedido("ENTREGADO", 7000, "2026-11-25T02:30:00")),
                LocalDateTime.parse("2026-11-24T03:00:00"), // 24-nov 00:00 en Chile
                LocalDateTime.parse("2026-11-26T03:00:00"), // 26-nov 00:00 en Chile
                CHILE, true);

        assertEquals(2, tramos.size());
        assertEquals("2026-11-24T03:00", tramos.get(0).inicio()); // medianoche de Chile, en UTC
        assertEquals(7000, tramos.get(0).ventas());
        assertEquals(0, tramos.get(1).ventas());
    }
}
