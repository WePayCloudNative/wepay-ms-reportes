
package cl.duoc.wepay_ms_reportes.repository;

import cl.duoc.wepay_ms_reportes.entity.PedidoReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoReporteRepository
        extends JpaRepository<PedidoReporte, Long> {

}
