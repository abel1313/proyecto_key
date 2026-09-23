package com.ventas.key.hexagonal.precio.aplicacion.servicio;

import com.ventas.key.hexagonal.precio.dominio.excepcion.PrecioInvalidoException;
import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeProducto;
import com.ventas.key.hexagonal.precio.dominio.puerto.entrada.CambiarPrecioCasoUso;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.AvisarCambioCatalogoPort;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.PreciosProductoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** [Hexagonal: implementacion del puerto de entrada] [Clean: Use Case Interactor] */
@Slf4j
@Service
@RequiredArgsConstructor
public class CambiarPrecioService implements CambiarPrecioCasoUso {

    private final PreciosProductoPort precios;
    private final AvisarCambioCatalogoPort catalogo;

    @Override
    @Transactional
    public PreciosDeProducto cambiar(Integer productoId, Double precioVenta, Double precioRebaja) {
        PreciosDeProducto antes = precios.buscar(productoId)
                .orElseThrow(() -> PrecioInvalidoException.productoNoExiste(productoId));
        PreciosDeProducto despues = antes.conPrecios(precioVenta, precioRebaja);

        precios.guardar(despues);
        catalogo.catalogoCambio();

        log.info("Precio del producto {} ('{}'): normal {} -> {}, descuento {} -> {}",
                productoId, antes.nombre(), antes.precioVenta(), despues.precioVenta(),
                antes.precioRebaja(), despues.precioRebaja());
        return despues;
    }
}
