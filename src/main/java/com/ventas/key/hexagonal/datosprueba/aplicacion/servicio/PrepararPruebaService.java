package com.ventas.key.hexagonal.datosprueba.aplicacion.servicio;

import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.AmbienteNoPermitidoException;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ArticuloDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ArticuloGuardado;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.CatalogoAleatorio;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.EscenarioDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.EscenarioPreparado;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.LineaDePedido;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ModeloDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.PedidoPreparado;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.entrada.PrepararPruebaCasoUso;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.AmbientePort;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.CatalogoPruebaPort;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.EscenariosPruebaPort;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.PedidosPruebaPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * [Hexagonal: Application Service] [Clean: Use Cases]
 *
 * <p>R16 (2026-10-13): cada prueba que necesita pedidos se prepara con un boton. Se crea un articulo
 * de prueba propio (marca "Prueba QA", stock 30) y los pedidos salen de los mismos servicios que la
 * pantalla (R8). Solo en la base de QA (R1). Cada vez que se toca el boton se crean pedidos nuevos:
 * una prueba no depende de lo que otra ya cancelo o pago (lo que paso con K5 y K6 en QA).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrepararPruebaService implements PrepararPruebaCasoUso {

    private static final double PRECIO = 150.0;
    private static final int STOCK = 30;

    private final AmbientePort ambiente;
    private final CatalogoPruebaPort catalogo;
    private final PedidosPruebaPort pedidos;
    private final EscenariosPruebaPort escenarios;

    @Override
    public List<EscenarioDePrueba> disponibles() {
        return Arrays.asList(EscenarioDePrueba.values());
    }

    @Override
    public EscenarioPreparado preparar(String prueba, int usuarioId) {
        EscenarioDePrueba escenario = EscenarioDePrueba.de(prueba);
        String base = ambiente.nombreDeLaBase();
        if (!AmbientePort.BASE_DE_PRUEBAS.equals(base)) {
            throw new AmbienteNoPermitidoException(base);
        }
        LineaDePedido linea = lineaDeUnArticuloNuevo(escenario);
        EscenarioPreparado listo = switch (escenario) {
            case P16 -> prueba16(linea, usuarioId);
            case P20 -> prueba20(linea);
            case P21 -> prueba21(linea, usuarioId);
            case P22 -> prueba22(linea, usuarioId);
            case P24 -> prueba24(linea, usuarioId);
        };
        log.info("Prueba {} preparada: {}", escenario.prueba(), listo.pedidos());
        return listo;
    }

    /**
     * 16: en una zona, dos 🕓 Pendiente del cliente con cuenta (A y B), un Ir pagando con $50 que todavia
     * no se lleva (C) y un Contado que todavia no se lleva (D).
     */
    private EscenarioPreparado prueba16(LineaDePedido linea, int usuarioId) {
        int zona = escenarios.zonaDePrueba();
        int clienteConCuenta = escenarios.clienteConCuentaDePrueba();
        int a = escenarios.pedidoDelCliente(clienteConCuenta, List.of(linea), zona);
        int b = escenarios.pedidoDelCliente(clienteConCuenta, List.of(linea), zona);
        int cliente = pedidos.clientesDePrueba(1).get(0);
        int c = escenarios.ventaDirecta("FIADO", cliente, usuarioId, List.of(linea), zona, false);
        pedidos.abonar(c, 50.0, usuarioId);
        int d = escenarios.ventaDirecta("NORMAL", cliente, usuarioId, List.of(linea), zona, false);
        return new EscenarioPreparado(EscenarioDePrueba.P16.prueba(), EscenarioDePrueba.P16.titulo(), List.of(
                new PedidoPreparado("A", a, "🕓 Pendiente de \"Cliente Prueba QA\", zona \"Zona Prueba QA\""),
                new PedidoPreparado("B", b, "🕓 Pendiente del mismo cliente, misma zona"),
                new PedidoPreparado("C", c, "💳 Ir pagando con $50, todavía no se lo lleva, misma zona"),
                new PedidoPreparado("D", d, "🛒 Contado, todavía no se lo lleva, misma zona")),
                "Sigue la 16 desde el paso 1. En el paso 8 elige la zona \"Zona Prueba QA\" (en vez de Tejupilco).");
    }

    /** 20.2: dos pedidos 🕓 Pendiente del mismo cliente con cuenta, para unirlos y cambiar su forma de cobro. */
    private EscenarioPreparado prueba20(LineaDePedido linea) {
        // Con zona: un 🕓 Pendiente con zona no tiene fecha para recoger y el cancelador automático no
        // lo toca (los que recogen en el local se cancelan solos a los 2 días de esa fecha).
        int zona = escenarios.zonaDePrueba();
        int cliente = escenarios.clienteConCuentaDePrueba();
        int p1 = escenarios.pedidoDelCliente(cliente, List.of(linea), zona);
        int p2 = escenarios.pedidoDelCliente(cliente, List.of(linea), zona);
        return new EscenarioPreparado(EscenarioDePrueba.P20.prueba(), EscenarioDePrueba.P20.titulo(), List.of(
                new PedidoPreparado("P1", p1, "🕓 Pendiente de \"Cliente Prueba QA\", $150"),
                new PedidoPreparado("P2", p2, "🕓 Pendiente del mismo cliente, $150")),
                "Abre P1 → 🔗 Unir con otros pedidos → busca P2 → Unir 2 pedidos. Después sigue la 20.2 desde el paso 1.");
    }

    /** 21: una zona con un pedido que debe (P1) y uno ya pagado que falta entregar (P2), los dos sin fecha. */
    private EscenarioPreparado prueba21(LineaDePedido linea, int usuarioId) {
        int zona = escenarios.zonaDePrueba();
        int cliente = pedidos.clientesDePrueba(1).get(0);
        int p1 = escenarios.ventaDirecta("APARTADO", cliente, usuarioId, List.of(linea), zona, false);
        int p2 = escenarios.ventaDirecta("FIADO", cliente, usuarioId, List.of(linea), zona, false);
        pedidos.abonar(p2, linea.subtotal(), usuarioId);
        return new EscenarioPreparado(EscenarioDePrueba.P21.prueba(), EscenarioDePrueba.P21.titulo(), List.of(
                new PedidoPreparado("P1", p1, "📦 Apartado que debe $150, zona \"Zona Prueba QA\", sin fecha"),
                new PedidoPreparado("P2", p2, "💳 Ir pagando ya pagado completo que falta entregar, misma zona, sin fecha")),
                "En Entregas por zona elige \"Zona Prueba QA\" y el rango de esta semana.");
    }

    /** 22: un 🕓 Pendiente (PE), un Apartado que debe (AP) y uno cancelado (CA). */
    private EscenarioPreparado prueba22(LineaDePedido linea, int usuarioId) {
        int clienteConCuenta = escenarios.clienteConCuentaDePrueba();
        int pe = escenarios.pedidoDelCliente(clienteConCuenta, List.of(linea), escenarios.zonaDePrueba());
        int cliente = pedidos.clientesDePrueba(1).get(0);
        int ap = escenarios.ventaDirecta("APARTADO", cliente, usuarioId, List.of(linea), null, false);
        int ca = escenarios.ventaDirecta("APARTADO", cliente, usuarioId, List.of(linea), null, false);
        escenarios.cancelar(ca);
        return new EscenarioPreparado(EscenarioDePrueba.P22.prueba(), EscenarioDePrueba.P22.titulo(), List.of(
                new PedidoPreparado("PE", pe, "🕓 Pendiente de \"Cliente Prueba QA\""),
                new PedidoPreparado("AP", ap, "📦 Apartado que debe $150"),
                new PedidoPreparado("CA", ca, "❌ Apartado cancelado (el cliente avisó)")),
                "Abre Mis pedidos y sigue la 22.1.");
    }

    /**
     * 24 (lo de K4/K5/K6 del 2026-10-13): dos Ir pagando del mismo cliente que ya se lo llevaron, uno
     * sin dinero (K4) y otro con $40 (K6). Desde el detalle de K4, "Unir" tiene que encontrar a K6.
     */
    private EscenarioPreparado prueba24(LineaDePedido linea, int usuarioId) {
        int cliente = pedidos.clientesDePrueba(1).get(0);
        int k4 = escenarios.ventaDirecta("FIADO", cliente, usuarioId, List.of(linea), null, true);
        int k6 = escenarios.ventaDirecta("FIADO", cliente, usuarioId, List.of(linea), null, true);
        pedidos.abonar(k6, 40.0, usuarioId);
        return new EscenarioPreparado(EscenarioDePrueba.P24.prueba(), EscenarioDePrueba.P24.titulo(), List.of(
                new PedidoPreparado("K4", k4, "💳 Ir pagando sin dinero, debe $150"),
                new PedidoPreparado("K6", k6, "💳 Ir pagando que ya dio $40, debe $110")),
                "Abre K4 → 🔗 Unir con otros pedidos → escribe el número de K6. Tiene que salir.");
    }

    /** Un modelo de prueba con un solo articulo y stock suficiente para todos los pedidos de la prueba. */
    private LineaDePedido lineaDeUnArticuloNuevo(EscenarioDePrueba escenario) {
        List<String> categorias = CatalogoAleatorio.categorias();
        catalogo.asegurarCategorias(categorias);
        long numero = catalogo.siguienteNumero();
        ModeloDePrueba modelo = new ModeloDePrueba(
                CatalogoAleatorio.codigoBarras(numero),
                "QA Prueba " + escenario.prueba() + " #" + numero,
                "Artículo para preparar la prueba " + escenario.prueba() + " (datos de prueba)",
                categorias.get(0), "Negro", 60.0, PRECIO, 0.0,
                List.of(new ArticuloDePrueba("M", "Negro", STOCK)));
        ArticuloGuardado articulo = catalogo.guardarLote(List.of(modelo)).get(0);
        return new LineaDePedido(articulo.id(), 1, articulo.precio());
    }
}
