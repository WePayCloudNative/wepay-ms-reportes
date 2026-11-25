
package cl.duoc.wepay_ms_reportes.controller;

import cl.duoc.wepay_ms_reportes.dto.VentasPorHoraDTO;
import cl.duoc.wepay_ms_reportes.dto.KpiResponseDTO;
import cl.duoc.wepay_ms_reportes.dto.ResumenReporteDTO;
import cl.duoc.wepay_ms_reportes.dto.TopProductoDTO;
import cl.duoc.wepay_ms_reportes.service.ReporteService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/report")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    // =========================================
    // REPORTE DE KPIs GENERALES
    // =========================================

    @GetMapping("/kpis")
    public KpiResponseDTO obtenerKpis(
            @RequestParam(defaultValue = "last24h") String range) {

        if (!"last24h".equals(range)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rango no soportado. Use last24h");
        }

        return reporteService.obtenerKpis(range);
    }

    // =========================================
    // REPORTE DE PRODUCTOS MÁS VENDIDOS
    // =========================================

    @GetMapping("/top-products")
    public List<TopProductoDTO> obtenerTopProductos(
            @RequestParam(defaultValue = "last7d") String range) {

        if (!"last7d".equals(range)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rango no soportado. Use last7d");
        }

        return reporteService.obtenerTopProductos(range);
    }

    @GetMapping("/sales-by-hour")
    public List<VentasPorHoraDTO> obtenerVentasPorHora(
            @RequestParam(defaultValue = "last24h") String range) {

        if (!"last24h".equals(range)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rango no soportado. Use last24h");
        }

        return reporteService.obtenerVentasPorHora(range);
    }


    // =========================================
    // RESUMEN DE UN PERÍODO
    // Ej: /api/report/resumen?desde=2026-11-24T03:00:00&hasta=2026-11-25T03:00:00&zona=America/Santiago
    // Las fechas van en UTC; la zona solo se usa para agrupar por hora o día local.
    // =========================================

    private static final Duration MAXIMO = Duration.ofDays(366);

    @GetMapping("/resumen")
    public ResumenReporteDTO obtenerResumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "America/Santiago") String zona) {

        if (!desde.isBefore(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "desde debe ser anterior a hasta");
        }
        if (Duration.between(desde, hasta).compareTo(MAXIMO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El período no puede superar un año");
        }

        ZoneId zonaId;
        try {
            zonaId = ZoneId.of(zona);
        } catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zona horaria no válida: " + zona);
        }

        return reporteService.resumen(desde, hasta, zonaId);
    }
}
