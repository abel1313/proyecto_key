package com.ventas.key.hexagonal.precio.aplicacion.servicio;

import com.ventas.key.hexagonal.precio.dominio.excepcion.PrecioInvalidoException;
import com.ventas.key.hexagonal.precio.dominio.modelo.PreciosDeArticulo;
import com.ventas.key.hexagonal.precio.dominio.puerto.entrada.CambiarPrecioArticuloCasoUso;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.AvisarCambioCatalogoPort;
import com.ventas.key.hexagonal.precio.dominio.puerto.salida.PreciosArticuloPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** [Hexagonal: implementacion del puerto de entrada] [Clean: Use Case Interactor] */
@Slf4j
@Service
@RequiredArgsConstructor
public class CambiarPrecioArticuloService implements CambiarPrecioArticuloCasoUso {

    private final PreciosArticuloPort precios;
    private final AvisarCambioCatalogoPort catalogo;

    @Override
    @Transactional
    public PreciosDeArticulo cambiar(Integer varianteId, Double precioVenta, Double precioRebaja, boolean usarDescuento) {
        PreciosDeArticulo antes = buscar(varianteId);
        PreciosDeArticulo despues = antes.conPrecios(precioVenta, precioRebaja, usarDescuento);

        precios.guardar(despues);
        catalogo.catalogoCambio();

        log.info("Precio del articulo {} ('{}'): normal {} -> {}, descuento {} -> {}, vender con descuento {} -> {}",
                varianteId, antes.nombre(), antes.precioVenta(), despues.precioVenta(),
                antes.precioRebaja(), despues.precioRebaja(), antes.usarDescuento(), despues.usarDescuento());
        return despues;
    }

    @Override
    @Transactional
    public PreciosDeArticulo usarElDelProducto(Integer varianteId) {
        PreciosDeArticulo antes = buscar(varianteId);
        if (!antes.propio()) {
            return antes;
        }
        precios.guardar(antes.heredando());
        catalogo.catalogoCambio();

        PreciosDeArticulo despues = buscar(varianteId);
        log.info("Precio del articulo {} ('{}'): vuelve al del producto ({} / {})",
                varianteId, antes.nombre(), despues.precioVenta(), despues.precioRebaja());
        return despues;
    }

    private PreciosDeArticulo buscar(Integer varianteId) {
        return precios.buscar(varianteId)
                .orElseThrow(() -> PrecioInvalidoException.articuloNoExiste(varianteId));
    }
}
