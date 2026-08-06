package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.Resena;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IResenaRepository extends BaseRepository<Resena, Integer> {

    Optional<Resena> findByCliente_IdAndVariante_Id(Integer clienteId, Integer varianteId);

    boolean existsByCliente_IdAndVariante_Id(Integer clienteId, Integer varianteId);

    Page<Resena> findByVariante_IdOrderByFechaCreacionDesc(Integer varianteId, Pageable pageable);

    Page<Resena> findByCliente_IdOrderByFechaCreacionDesc(Integer clienteId, Pageable pageable);

    // Object[] como tipo de retorno directo hace que Spring Data trate el metodo como
    // "collection query" y anide la fila real en result[0] en vez de venir aplanada
    // (ver mismo patron/comentario en IVentaRepository.sumVentasRaw). Por eso se expone
    // como List<Object[]> y se aplana en el metodo default.
    @Query("SELECT AVG(r.calificacion), COUNT(r) FROM Resena r WHERE r.variante.id = :varianteId")
    List<Object[]> resumenPorVarianteRaw(@Param("varianteId") Integer varianteId);

    default Object[] resumenPorVariante(Integer varianteId) {
        List<Object[]> filas = resumenPorVarianteRaw(varianteId);
        return filas.isEmpty() ? new Object[]{null, null} : filas.get(0);
    }

    @Query("SELECT r.calificacion, COUNT(r) FROM Resena r WHERE r.variante.id = :varianteId GROUP BY r.calificacion")
    List<Object[]> conteoPorEstrella(@Param("varianteId") Integer varianteId);
}
