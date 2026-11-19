
package cl.duoc.wepay_ms_reportes.controller;

import cl.duoc.wepay_ms_reportes.dto.KpiResponseDTO;
import cl.duoc.wepay_ms_reportes.service.ReporteService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/report")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/kpis")
    public KpiResponseDTO obtenerKpis(
        @RequestParam(defaultValue = "last24h") String range
    ) {
        if (!"last24h".equals(range)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Rango no soportado"
            );
        }

        return reporteService.obtenerKpis(range);
    }
}
