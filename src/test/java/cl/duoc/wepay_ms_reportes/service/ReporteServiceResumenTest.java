package cl.duoc.wepay_ms_reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.wepay_ms_reportes.dto.ResumenReporteDTO;
import cl.duoc.wepay_ms_reportes.entity.PedidoReporte;
import cl.duoc.wepay_ms_reportes.repository.PedidoReporteRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReporteServiceResumenTest {

    private static final ZoneId CHILE = ZoneId.of("America/Santiago");

    @Test
    void comparaConElPeriodoAnteriorDelMismoLargo() {
        PedidoReporteRepository repositorio = mock(PedidoReporteRepository.class);
        LocalDateTime desde = LocalDateTime.parse("2026-11-24T03:00:00");
        LocalDateTime hasta = LocalDateTime.parse("2026-11-25T03:00:00");
        when(repositorio.creadosEntre(desde, hasta)).thenReturn(List.of());
        when(repositorio.creadosEntre(LocalDateTime.parse("2026-11-23T03:00:00"), desde))
                .thenReturn(List.of(new PedidoReporte()));

        ResumenReporteDTO resumen = new ReporteService(repositorio).resumen(desde, hasta, CHILE);

        assertEquals(0, resumen.actual().totalPedidos());
        assertEquals(1, resumen.anterior().totalPedidos());
        verify(repositorio).creadosEntre(LocalDateTime.parse("2026-11-23T03:00:00"), desde);
    }

    @Test
    void unDiaSeAgrupaPorHoraYUnaSemanaPorDia() {
        PedidoReporteRepository repositorio = mock(PedidoReporteRepository.class);
        ReporteService servicio = new ReporteService(repositorio);
        LocalDateTime desde = LocalDateTime.parse("2026-11-17T03:00:00");

        ResumenReporteDTO dia = servicio.resumen(desde, desde.plusDays(1), CHILE);
        ResumenReporteDTO semana = servicio.resumen(desde, desde.plusDays(7), CHILE);

        assertEquals("hora", dia.agrupacion());
        assertEquals(24, dia.ventasPorTramo().size());
        assertEquals("dia", semana.agrupacion());
        assertEquals(7, semana.ventasPorTramo().size());
    }
}
