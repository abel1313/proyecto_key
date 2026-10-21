package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.PagoTarjetaDevolucion;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IPagoTarjetaDevolucionRepository extends BaseRepository<PagoTarjetaDevolucion, Integer> {

    List<PagoTarjetaDevolucion> findByIntentoIdAndEstadoOrderByIdAsc(Integer intentoId, String estado);
}
