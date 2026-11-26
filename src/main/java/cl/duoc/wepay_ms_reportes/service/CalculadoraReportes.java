package cl.duoc.wepay_ms_reportes.service;

import cl.duoc.wepay_ms_reportes.dto.IndicadoresDTO;
import cl.duoc.wepay_ms_reportes.dto.TopProductoDTO;
import cl.duoc.wepay_ms_reportes.dto.VentasPorTramoDTO;
import cl.duoc.wepay_ms_reportes.entity.DetallePedidoReporte;
import cl.duoc.wepay_ms_reportes.entity.PedidoReporte;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Cálculos de reportes sin Spring ni base de datos: recibe pedidos y devuelve números.
 * Así se puede probar con tests unitarios simples.
 *
 * Las fechas guardadas están en UTC (LocalDateTime sin zona). Para agrupar por día
 * se convierten a la zona del negocio: un pedido a las 22:00 de Chile es "hoy",
 * aunque en UTC ya sea mañana.
 */
public final class CalculadoraReportes {

    /** Estados con pago confirmado: desde aquí se cuenta como venta. */
    public static final Set<String> CONFIRMADOS = Set.of("ACEPTADO", "EN_PREPARACION", "DESPACHADO", "ENTREGADO");

    private CalculadoraReportes() {
    }

    public static IndicadoresDTO indicadores(List<PedidoReporte> pedidos) {
        long total = pedidos.size();
        List<PedidoReporte> confirmados = filtrar(pedidos, CONFIRMADOS);
        List<PedidoReporte> entregados = filtrar(pedidos, Set.of("ENTREGADO"));
        List<PedidoReporte> cancelados = filtrar(pedidos, Set.of("CANCELADO"));
        long activos = total - entregados.size() - cancelados.size();

        double ventasConfirmadas = sumar(confirmados);
        double ticketPromedio = confirmados.isEmpty() ? 0 : ventasConfirmadas / confirmados.size();
        double tasaCancelacion = total == 0 ? 0 : redondear(cancelados.size() * 100.0 / total);

        double leadTime = entregados.stream()
                .filter(p -> p.getFechaCreacion() != null && p.getFechaEntrega() != null)
                .mapToLong(p -> Duration.between(p.getFechaCreacion(), p.getFechaEntrega()).toMinutes())
                .filter(minutos -> minutos >= 0)
                .average()
                .orElse(0);

        return new IndicadoresDTO(
                total,
                confirmados.size(),
                entregados.size(),
                activos,
                cancelados.size(),
                ventasConfirmadas,
                sumar(entregados),
                sumar(cancelados),
                redondear(ticketPromedio),
                tasaCancelacion,
                redondear(leadTime));
    }

    public static Map<String, Long> porEstado(List<PedidoReporte> pedidos) {
        return pedidos.stream().collect(Collectors.groupingBy(
                p -> p.getEstado() != null ? p.getEstado() : "DESCONOCIDO",
                TreeMap::new,
                Collectors.counting()));
    }

    /** Más vendidos entre los pedidos confirmados, ordenados por unidades. */
    public static List<TopProductoDTO> topProductos(List<PedidoReporte> pedidos, int limite) {
        Map<Long, Long> cantidades = new HashMap<>();
        Map<Long, Double> ingresos = new HashMap<>();
        Map<Long, String> nombres = new HashMap<>();

        for (PedidoReporte pedido : filtrar(pedidos, CONFIRMADOS)) {
            for (DetallePedidoReporte d : pedido.getDetalles()) {
                if (d.getProductoId() == null || d.getCantidad() == null || d.getPrecioUnitario() == null) {
                    continue;
                }
                cantidades.merge(d.getProductoId(), (long) d.getCantidad(), Long::sum);
                ingresos.merge(d.getProductoId(), d.getPrecioUnitario() * d.getCantidad(), Double::sum);
                nombres.put(d.getProductoId(), d.getNombreProducto());
            }
        }

        return cantidades.entrySet().stream()
                .map(e -> new TopProductoDTO(e.getKey(), nombres.get(e.getKey()), e.getValue(),
                        ingresos.get(e.getKey())))
                .sorted(Comparator.comparingLong(TopProductoDTO::cantidadVendida).reversed()
                        .thenComparing(TopProductoDTO::productoId))
                .limit(limite)
                .toList();
    }

    /**
     * Ventas confirmadas agrupadas por hora o por día (en la zona del negocio).
     * Devuelve todos los tramos del período, con ceros donde no hubo ventas.
     */
    public static List<VentasPorTramoDTO> ventasPorTramo(List<PedidoReporte> pedidos,
            LocalDateTime desde, LocalDateTime hasta, ZoneId zona, boolean porDia) {
        Map<Instant, long[]> pedidosPorTramo = new LinkedHashMap<>();
        Map<Instant, Double> ventasPorTramo = new LinkedHashMap<>();

        ZonedDateTime fin = enZona(hasta, zona);
        for (ZonedDateTime t = inicioDeTramo(enZona(desde, zona), porDia); t.isBefore(fin);
                t = porDia ? t.plusDays(1) : t.plusHours(1)) {
            pedidosPorTramo.put(t.toInstant(), new long[] { 0 });
            ventasPorTramo.put(t.toInstant(), 0.0);
        }

        for (PedidoReporte p : filtrar(pedidos, CONFIRMADOS)) {
            if (p.getFechaCreacion() == null) {
                continue;
            }
            Instant tramo = inicioDeTramo(enZona(p.getFechaCreacion(), zona), porDia).toInstant();
            if (pedidosPorTramo.containsKey(tramo)) {
                pedidosPorTramo.get(tramo)[0]++;
                ventasPorTramo.merge(tramo, p.getTotal() != null ? p.getTotal() : 0.0, Double::sum);
            }
        }

        List<VentasPorTramoDTO> resultado = new ArrayList<>();
        for (Instant tramo : pedidosPorTramo.keySet()) {
            resultado.add(new VentasPorTramoDTO(
                    LocalDateTime.ofInstant(tramo, ZoneOffset.UTC).toString(),
                    pedidosPorTramo.get(tramo)[0],
                    ventasPorTramo.get(tramo)));
        }
        return resultado;
    }

    private static ZonedDateTime enZona(LocalDateTime utc, ZoneId zona) {
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(zona);
    }

    private static ZonedDateTime inicioDeTramo(ZonedDateTime fecha, boolean porDia) {
        return porDia ? fecha.toLocalDate().atStartOfDay(fecha.getZone()) : fecha.truncatedTo(ChronoUnit.HOURS);
    }

    private static List<PedidoReporte> filtrar(List<PedidoReporte> pedidos, Set<String> estados) {
        return pedidos.stream().filter(p -> p.getEstado() != null && estados.contains(p.getEstado())).toList();
    }

    private static double sumar(List<PedidoReporte> pedidos) {
        return pedidos.stream().mapToDouble(p -> p.getTotal() != null ? p.getTotal() : 0.0).sum();
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
