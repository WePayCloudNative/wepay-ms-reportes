
package cl.duoc.wepay_ms_reportes.service;

import cl.duoc.wepay_ms_reportes.dto.KpiResponseDTO;
import cl.duoc.wepay_ms_reportes.entity.PedidoReporte;
import cl.duoc.wepay_ms_reportes.repository.PedidoReporteRepository;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    private final PedidoReporteRepository repository;

    public ReporteService(PedidoReporteRepository repository) {
        this.repository = repository;
    }

    public KpiResponseDTO obtenerKpis(String range) {

        if (!"last24h".equals(range)) {
            throw new IllegalArgumentException(
                "Rango no soportado: " + range
            );
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
            .mapToDouble(p -> p.getTotal() != null
                ? p.getTotal() : 0.0)
            .sum();

        double leadTimePromedio = pedidos.stream()
            .filter(p -> "ENTREGADO".equals(p.getEstado()))
            .filter(p -> p.getFechaEntrega() != null)
            .mapToLong(p -> Duration.between(
                p.getFechaCreacion(),
                p.getFechaEntrega()
            ).toMinutes())
            .filter(minutos -> minutos >= 0)
            .average()
            .orElse(0.0);

        Map<String, Long> porEstado = pedidos.stream()
            .collect(Collectors.groupingBy(
                p -> p.getEstado() != null
                    ? p.getEstado() : "DESCONOCIDO",
                Collectors.counting()
            ));

        return new KpiResponseDTO(
            totalPedidos,
            entregados,
            activos,
            ventasTotales,
            leadTimePromedio,
            porEstado
        );
    }
}
