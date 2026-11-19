
package cl.duoc.wepay_ms_reportes.repository;

import cl.duoc.wepay_ms_reportes.entity.EventoProcesadoReporte;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoProcesadoReporteRepository
        extends JpaRepository<EventoProcesadoReporte, String> {
}
