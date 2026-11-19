
package cl.duoc.wepay_ms_reportes.service;

import cl.duoc.wepay_ms_reportes.dto.KpiResponseDTO;
import cl.duoc.wepay_ms_reportes.dto.TopProductoDTO;
import cl.duoc.wepay_ms_reportes.entity.DetallePedidoReporte;
import cl.duoc.wepay_ms_reportes.entity.PedidoReporte;
import cl.duoc.wepay_ms_reportes.repository.PedidoReporteRepository;
import cl.duoc.wepay_ms_reportes.dto.VentasPorHoraDTO;
import java.util.TreeMap;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    private final PedidoReporteRepository repository;

    public ReporteService(PedidoReporteRepository repository) {
        this.repository = repository;
    }

    // =========================================
    // REPORTE DE KPIs
    // =========================================

    @Transactional(readOnly = true)
    public KpiResponseDTO obtenerKpis(String range) {

        if (!"last24h".equals(range)) {
            throw new IllegalArgumentException(
                    "Rango no soportado: " + range);
        }

        LocalDateTime desde = LocalDateTime.now().minusHours(24);

        List<PedidoReporte> pedidos = repository.findAll()
                .stream()
                .filter(p -> p.getFechaCreacion() != null)
                .filter(p -> !p.getFechaCreacion().isBefore(desde))
                .toList();

        long totalPedidos = pedidos.size();

        long entregados = pedidos.stream()
                .filter(p -> "ENTREGADO".equals(p.getEstado()))
                .count();

        long activos = pedidos.stream()
                .filter(p -> !"ENTREGADO".equals(p.getEstado()))
                .filter(p -> !"CANCELADO".equals(p.getEstado()))
                .count();

        double ventasTotales = pedidos.stream()
                .filter(p -> "ENTREGADO".equals(p.getEstado()))
                .mapToDouble(p -> p.getTotal() != null ? p.getTotal() : 0.0)
                .sum();

        double leadTimePromedio = pedidos.stream()
                .filter(p -> "ENTREGADO".equals(p.getEstado()))
                .filter(p -> p.getFechaEntrega() != null)
                .mapToLong(p -> Duration.between(
                        p.getFechaCreacion(),
                        p.getFechaEntrega()).toMinutes())
                .filter(minutos -> minutos >= 0)
                .average()
                .orElse(0.0);

        Map<String, Long> porEstado = pedidos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getEstado() != null
                                ? p.getEstado()
                                : "DESCONOCIDO",
                        Collectors.counting()));

        return new KpiResponseDTO(
                totalPedidos,
                entregados,
                activos,
                ventasTotales,
                leadTimePromedio,
                porEstado);
    }

    // =========================================
    // REPORTE DE PRODUCTOS MÁS VENDIDOS
    // =========================================

    @Transactional(readOnly = true)
    public List<TopProductoDTO> obtenerTopProductos(String range) {

        if (!"last7d".equals(range)) {
            throw new IllegalArgumentException(
                    "Rango no soportado: " + range);
        }

        LocalDateTime desde = LocalDateTime.now().minusDays(7);

        List<PedidoReporte> pedidos = repository.findAll()
                .stream()
                .filter(p -> "ENTREGADO".equals(p.getEstado()))
                .filter(p -> p.getFechaCreacion() != null)
                .filter(p -> !p.getFechaCreacion().isBefore(desde))
                .toList();

        Map<Long, Long> cantidades = new HashMap<>();
        Map<Long, Double> ingresos = new HashMap<>();
        Map<Long, String> nombres = new HashMap<>();

        for (PedidoReporte pedido : pedidos) {

            for (DetallePedidoReporte detalle : pedido.getDetalles()) {

                Long productoId = detalle.getProductoId();

                if (productoId == null
                        || detalle.getCantidad() == null
                        || detalle.getPrecioUnitario() == null) {
                    continue;
                }

                long cantidad = detalle.getCantidad();

                double subtotal = detalle.getPrecioUnitario() * cantidad;

                cantidades.merge(
                        productoId,
                        cantidad,
                        Long::sum);

                ingresos.merge(
                        productoId,
                        subtotal,
                        Double::sum);

                nombres.put(
                        productoId,
                        detalle.getNombreProducto());
            }
        }

        return cantidades.entrySet()
                .stream()
                .map(entry -> new TopProductoDTO(
                        entry.getKey(),
                        nombres.get(entry.getKey()),
                        entry.getValue(),
                        ingresos.get(entry.getKey())))
                .sorted(
                        Comparator.comparingLong(
                                TopProductoDTO::cantidadVendida).reversed())
                .toList();

    }

    @Transactional(readOnly = true)
    public List<VentasPorHoraDTO> obtenerVentasPorHora(String range) {

        if (!"last24h".equals(range)) {
            throw new IllegalArgumentException(
                    "Rango no soportado: " + range);
        }

        LocalDateTime desde = LocalDateTime.now().minusHours(24);

        // Agrupar pedidos por hora de entrega
        Map<LocalDateTime, List<PedidoReporte>> ventas = repository.findAll()
                .stream()
                .filter(p -> "ENTREGADO".equals(p.getEstado()))
                .filter(p -> p.getFechaEntrega() != null)
                .filter(p -> !p.getFechaEntrega().isBefore(desde))
                .collect(Collectors.groupingBy(
                        p -> p.getFechaEntrega()
                                .truncatedTo(ChronoUnit.HOURS),
                        TreeMap::new,
                        Collectors.toList()));

        return ventas.entrySet()
                .stream()
                .map(entry -> {

                    long cantidadPedidos = entry.getValue().size();

                    double totalVentas = entry.getValue()
                            .stream()
                            .mapToDouble(p -> p.getTotal() != null
                                    ? p.getTotal()
                                    : 0.0)
                            .sum();

                    return new VentasPorHoraDTO(
                            entry.getKey().toString(),
                            cantidadPedidos,
                            totalVentas);
                })
                .toList();
    }

}
