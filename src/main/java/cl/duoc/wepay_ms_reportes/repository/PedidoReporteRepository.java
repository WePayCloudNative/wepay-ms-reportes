package cl.duoc.wepay_ms_reportes.repository;

import cl.duoc.wepay_ms_reportes.entity.PedidoReporte;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoReporteRepository
        extends JpaRepository<PedidoReporte, Long> {

    /**
     * Pedidos creados en [desde, hasta), con sus productos en la misma consulta.
     * El filtro lo hace Oracle: no se cargan todos los pedidos en memoria.
     */
    @Query("""
            select distinct p from PedidoReporte p
            left join fetch p.detalles
            where p.fechaCreacion >= :desde and p.fechaCreacion < :hasta
            """)
    List<PedidoReporte> creadosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
