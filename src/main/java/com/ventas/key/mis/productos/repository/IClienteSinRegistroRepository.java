package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.ClienteSinRegistro;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface IClienteSinRegistroRepository extends BaseRepository<ClienteSinRegistro,Integer>{

    // Huerfanos: creados antes del cutoff (margen de seguridad, ver
    // ClienteSinRegistroLimpiezaScheduler) y sin ningun Pedido que los referencie -- el admin
    // agrego el cliente en el modal y luego lo reemplazo/cancelo antes de generar la venta.
    @Modifying
    @Query(value = """
        DELETE FROM clientes_sin_registro
        WHERE creado_en IS NOT NULL AND creado_en < :cutoff
          AND id NOT IN (
              SELECT DISTINCT cliente_sin_registro_id FROM pedidos
              WHERE cliente_sin_registro_id IS NOT NULL
          )
        """, nativeQuery = true)
    int eliminarHuerfanosAntesDe(@Param("cutoff") LocalDateTime cutoff);
}
