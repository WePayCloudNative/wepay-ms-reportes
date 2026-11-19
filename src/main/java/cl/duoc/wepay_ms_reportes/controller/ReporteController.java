
package cl.duoc.wepay_ms_reportes.controller;

import cl.duoc.wepay_ms_reportes.dto.VentasPorHoraDTO;
import cl.duoc.wepay_ms_reportes.dto.KpiResponseDTO;
import cl.duoc.wepay_ms_reportes.dto.TopProductoDTO;
import cl.duoc.wepay_ms_reportes.service.ReporteService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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

}
