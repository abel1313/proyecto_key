package com.ventas.key.mis.productos.repository;

import com.ventas.key.mis.productos.entity.QrDestino;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IQrDestinoRepository extends JpaRepository<QrDestino, Integer> {

    List<QrDestino> findByActivoTrueOrderByOrdenAscIdAsc();

    List<QrDestino> findAllByOrderByOrdenAscIdAsc();
}
