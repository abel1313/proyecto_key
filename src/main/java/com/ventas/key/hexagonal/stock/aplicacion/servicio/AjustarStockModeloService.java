package com.ventas.key.hexagonal.stock.aplicacion.servicio;

import com.ventas.key.hexagonal.stock.dominio.excepcion.ProductoSinStockConocidoException;
import com.ventas.key.hexagonal.stock.dominio.modelo.DisponibilidadStock;
import com.ventas.key.hexagonal.stock.dominio.puerto.entrada.AjustarStockModeloCasoUso;
import com.ventas.key.hexagonal.stock.dominio.puerto.salida.AvisarCambioStockPort;
import com.ventas.key.hexagonal.stock.dominio.puerto.salida.ConsultarStockPort;
import com.ventas.key.hexagonal.stock.dominio.puerto.salida.GuardarStockModeloPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [Hexagonal: implementacion del puerto de entrada] [Clean: Use Case Interactor]
 *
 * <p>Orquesta: lee como esta el modelo, el modelo de dominio decide si el ajuste vale
 * ({@link DisponibilidadStock#conAjuste(int)}), guarda y avisa. Sin reglas propias.
 */
@Service
@RequiredArgsConstructor
public class AjustarStockModeloService implements AjustarStockModeloCasoUso {

    private final ConsultarStockPort consultarStock;
    private final GuardarStockModeloPort guardarStock;
    private final AvisarCambioStockPort avisarCambio;

    @Override
    @Transactional
    public DisponibilidadStock ajustar(Integer productoId, int ajuste) {
        DisponibilidadStock actual = consultarStock.disponibilidadDe(productoId)
                .orElseThrow(() -> new ProductoSinStockConocidoException(productoId));
        DisponibilidadStock ajustado = actual.conAjuste(ajuste);
        guardarStock.guardarStockTotal(productoId, ajustado.stockTotal());
        avisarCambio.stockCambio(productoId);
        return ajustado;
    }
}
