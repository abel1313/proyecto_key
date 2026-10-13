package com.ventas.key.hexagonal.datosprueba.aplicacion.servicio;

import com.ventas.key.hexagonal.datosprueba.dominio.excepcion.AmbienteNoPermitidoException;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ArticuloDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ArticuloGuardado;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.CatalogoAleatorio;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.CatalogoPreparado;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.DatosDelModelo;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * [Hexagonal: Application Service] [Clean: Use Cases]
 *
 * <p>R16 (2026-10-13): cada prueba que necesita pedidos se prepara con un boton. Se crea un articulo
 * de prueba propio (marca "Prueba QA", stock 30) y los pedidos salen de los mismos servicios que la
 * pantalla (R8). Solo en la base de QA (R1). Cada vez que se toca el boton se crean pedidos nuevos:
 * una prueba no depende de lo que otra ya cancelo o pago (lo que paso con K5 y K6 en QA).
 *
 * <p>R18 (2026-10-13, pedido del dueño: "dar de alta lo que se necesita para las pruebas"): también
 * los modelos y artículos que pide cada prueba (Art-A, Art-200, M-OFF, un modelo agotado...), con el
 * generador del catálogo de prueba y dejados en el estado que pide la prueba. R19: "Preparar todas".
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
        exigirBaseDePruebas();
        EscenarioPreparado listo = armar(escenario, usuarioId);
        escenarios.limpiarCache();
        return listo;
    }

    @Override
    public List<EscenarioPreparado> prepararTodas(int usuarioId) {
        exigirBaseDePruebas();
        List<EscenarioPreparado> todas = new ArrayList<>();
        for (EscenarioDePrueba escenario : EscenarioDePrueba.values()) {
            try {
                todas.add(armar(escenario, usuarioId));
            } catch (RuntimeException e) {
                // R19: una que falla no detiene a las demás; se reporta con su motivo.
                log.warn("Prueba {} no se pudo preparar: {}", escenario.prueba(), e.getMessage(), e);
                todas.add(EscenarioPreparado.conError(escenario, e.getMessage()));
            }
        }
        escenarios.limpiarCache();
        return todas;
    }

    private void exigirBaseDePruebas() {
        String base = ambiente.nombreDeLaBase();
        if (!AmbientePort.BASE_DE_PRUEBAS.equals(base)) {
            throw new AmbienteNoPermitidoException(base);
        }
    }

    private EscenarioPreparado armar(EscenarioDePrueba escenario, int usuarioId) {
        EscenarioPreparado listo = switch (escenario) {
            case P12 -> prueba12(usuarioId);
            case P14 -> prueba14();
            case P15 -> prueba15();
            case P16 -> prueba16(lineaDeUnArticuloNuevo(escenario), usuarioId);
            case P17 -> prueba17();
            case P18 -> prueba18();
            case P20 -> prueba20(lineaDeUnArticuloNuevo(escenario));
            case P21 -> prueba21(lineaDeUnArticuloNuevo(escenario), usuarioId);
            case P22 -> prueba22(lineaDeUnArticuloNuevo(escenario), usuarioId);
            case P24 -> prueba24(lineaDeUnArticuloNuevo(escenario), usuarioId);
        };
        log.info("Prueba {} preparada: pedidos {} catálogo {}", escenario.prueba(), listo.pedidos(), listo.catalogo());
        return listo;
    }

    // ── Pruebas que necesitan catálogo (R18) ───────────────────────────────────

    /** 12.6 y 12.7: Art-200 ($200) para Venta directa, y una venta de contado y un Ir pagando con abono para los tickets. */
    private EscenarioPreparado prueba12(int usuarioId) {
        EscenarioDePrueba e = EscenarioDePrueba.P12;
        ArticuloGuardado art200 = modeloNuevo(e, "Art-200", 0, 200.0, List.of(new ArticuloDePrueba("M", "Negro", 12))).get(0);
        LineaDePedido linea = new LineaDePedido(art200.id(), 1, art200.precio());
        int cliente = pedidos.clientesDePrueba(1).get(0);
        int t1 = escenarios.ventaDirecta("NORMAL", cliente, usuarioId, List.of(linea), null, true);
        int t2 = escenarios.ventaDirecta("FIADO", cliente, usuarioId, List.of(linea), null, true);
        pedidos.abonar(t2, 50.0, usuarioId);
        return new EscenarioPreparado(e.prueba(), e.titulo(), List.of(
                new PedidoPreparado("T1", t1, "🛒 Venta de contado de Art-200, ya cobrada: su ticket (12.7 paso 1)"),
                new PedidoPreparado("T2", t2, "💳 Ir pagando de Art-200 con un abono de $50: ticket de abono (12.7 paso 2)")),
                List.of(dato("Art-200", CatalogoPreparado.ARTICULO, art200, "Artículo de $200 con stock 10, para Venta directa (12.6)")),
                "12.6: en Venta directa busca \"Art-200\". 12.7: imprime el ticket de T1 y de T2 desde su tarjeta (🖨).", null);
    }

    /** 14-QA: un modelo con stock 3 y un artículo con stock 1 con foto: 🧩 Productos dice "Puedes crear: 2". */
    private EscenarioPreparado prueba14() {
        EscenarioDePrueba e = EscenarioDePrueba.P14;
        ArticuloGuardado a = modeloNuevo(e, "Productos", 0, 150.0, List.of(new ArticuloDePrueba("M", "Negro", 1))).get(0);
        escenarios.stockDelModelo(a.id(), 3);
        return new EscenarioPreparado(e.prueba(), e.titulo(), List.of(),
                List.of(dato("M3", CatalogoPreparado.MODELO, a, "Modelo con stock 3 y un artículo con stock 1 con foto")),
                "Catálogo → 🔍 Modelos → busca \"QA Prueba 14\" → 🧩 Productos. Sigue la 14-QA desde el paso 1.", null);
    }

    /**
     * 15 paso 3 y 4: un artículo habilitado con 4 y uno viejo deshabilitado con 3, en un modelo con stock
     * 5 (al habilitarlo baja a 1) y en otro con stock 10 (se queda con sus 3). De categorías distintas (2b y 2c).
     */
    private EscenarioPreparado prueba15() {
        EscenarioDePrueba e = EscenarioDePrueba.P15;
        List<ArticuloGuardado> m5 = modeloNuevo(e, "M5", 0, 150.0,
                List.of(new ArticuloDePrueba("CH", "Negro", 4), new ArticuloDePrueba("M", "Negro", 3)));
        escenarios.articuloDeshabilitadoConStock(m5.get(1).id(), 3);
        escenarios.stockDelModelo(m5.get(0).id(), 5);
        List<ArticuloGuardado> m10 = modeloNuevo(e, "M10", 1, 150.0,
                List.of(new ArticuloDePrueba("CH", "Negro", 4), new ArticuloDePrueba("M", "Negro", 3)));
        escenarios.articuloDeshabilitadoConStock(m10.get(1).id(), 3);
        escenarios.stockDelModelo(m10.get(0).id(), 10);
        return new EscenarioPreparado(e.prueba(), e.titulo(), List.of(), List.of(
                dato("M5", CatalogoPreparado.MODELO, m5.get(0), "Modelo con stock 5: talla CH habilitada con 4"),
                dato("M5-baja", CatalogoPreparado.ARTICULO, m5.get(1), "Talla M deshabilitada que guarda 3 (al habilitarla debe quedar en 1)"),
                dato("M10", CatalogoPreparado.MODELO, m10.get(0), "Modelo con stock 10 y otra categoría: talla CH habilitada con 4"),
                dato("M10-baja", CatalogoPreparado.ARTICULO, m10.get(1), "Talla M deshabilitada que guarda 3 (al habilitarla se queda con 3)")),
                "Paso 3: Tienda → filtro No habilitadas → busca \"QA Prueba 15\". Pasos 2b y 2c: Agregar producto con M5 y M10.", null);
    }

    /**
     * 17: un modelo con 3 artículos habilitados con stock y uno deshabilitado con stock (filtros), Art-A
     * de $100, uno de $130 que se vende al descuento de $100 (precio mín/máx) y uno con stock 1 (Venta directa).
     */
    private EscenarioPreparado prueba17() {
        EscenarioDePrueba e = EscenarioDePrueba.P17;
        List<ArticuloGuardado> filtros = modeloNuevo(e, "Filtros", 0, 150.0, List.of(
                new ArticuloDePrueba("CH", "Negro", 5), new ArticuloDePrueba("M", "Negro", 5),
                new ArticuloDePrueba("G", "Negro", 5), new ArticuloDePrueba("XL", "Negro", 5)));
        escenarios.articuloDeshabilitadoConStock(filtros.get(3).id(), 5);
        ArticuloGuardado artA = modeloNuevo(e, "Art-A", 0, 100.0, List.of(new ArticuloDePrueba("M", "Negro", 10))).get(0);
        ArticuloGuardado descuento = modeloNuevo(e, "Descuento", 0, 130.0, List.of(new ArticuloDePrueba("M", "Negro", 10))).get(0);
        escenarios.precioDelArticulo(descuento.id(), 130.0, 100.0, true);
        ArticuloGuardado stock1 = modeloNuevo(e, "Stock-1", 0, 120.0, List.of(new ArticuloDePrueba("M", "Negro", 1))).get(0);
        return new EscenarioPreparado(e.prueba(), e.titulo(), List.of(), List.of(
                dato("Filtros", CatalogoPreparado.MODELO, filtros.get(0), "Tallas CH, M y G habilitadas con 5; XL deshabilitada (borde rojo) con 5"),
                dato("Art-A", CatalogoPreparado.ARTICULO, artA, "Artículo de $100 con stock 10"),
                dato("Descuento", CatalogoPreparado.ARTICULO, descuento, "Artículo de $130 que se vende al descuento: la tarjeta dice $100"),
                dato("Stock-1", CatalogoPreparado.ARTICULO, stock1, "Artículo con stock 1, para Venta directa (17.3)")),
                "17.1: Tienda → busca \"QA Prueba 17 Filtros\". 17.2: precio $100 → salen Art-A y Descuento. 17.3: Venta directa → \"Stock-1\".", null);
    }

    /** 18: M-OFF (modelo deshabilitado) y M-0 (habilitado, todo su stock repartido: libre 0). */
    private EscenarioPreparado prueba18() {
        EscenarioDePrueba e = EscenarioDePrueba.P18;
        ArticuloGuardado off = modeloNuevo(e, "M-OFF", 0, 150.0, List.of(new ArticuloDePrueba("M", "Negro", 2))).get(0);
        escenarios.habilitarModelo(off.id(), false);
        ArticuloGuardado cero = modeloNuevo(e, "M-0", 0, 150.0, List.of(new ArticuloDePrueba("M", "Negro", 4))).get(0);
        return new EscenarioPreparado(e.prueba(), e.titulo(), List.of(), List.of(
                dato("M-OFF", CatalogoPreparado.MODELO, off, "Modelo deshabilitado (su artículo sigue habilitado)"),
                dato("M-0", CatalogoPreparado.MODELO, cero, "Modelo habilitado con stock 4, todo repartido en su artículo: libre 0")),
                "Catálogo → 🧩 Agregar producto → busca \"M-OFF\" y \"M-0\". Sigue la 18.1.", null);
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
        ArticuloGuardado agotado = modeloNuevo(EscenarioDePrueba.P20, "Agotado", 0, 150.0,
                List.of(new ArticuloDePrueba("M", "Negro", 0))).get(0);
        return new EscenarioPreparado(EscenarioDePrueba.P20.prueba(), EscenarioDePrueba.P20.titulo(), List.of(
                new PedidoPreparado("P1", p1, "🕓 Pendiente de \"Cliente Prueba QA\", $150"),
                new PedidoPreparado("P2", p2, "🕓 Pendiente del mismo cliente, $150")),
                List.of(dato("Agotado", CatalogoPreparado.MODELO, agotado, "Modelo con stock 0 (agotado), para la 20.3")),
                "20.2: abre P1 → 🔗 Unir con otros pedidos → escribe el número de P2 → Unir 2 pedidos. 20.3: Catálogo → 🔍 Modelos → \"QA Prueba 20 Agotado\".",
                null);
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
        ArticuloGuardado articulo = modeloNuevo(escenario, "", 0, PRECIO,
                List.of(new ArticuloDePrueba("M", "Negro", STOCK))).get(0);
        return new LineaDePedido(articulo.id(), 1, articulo.precio());
    }

    /**
     * Un modelo de prueba nuevo ("QA Prueba 17 Art-A #2098…", marca "Prueba QA", foto real de QA si hay,
     * R7) con sus artículos, en el orden en que se piden. Regresa los artículos guardados.
     */
    private List<ArticuloGuardado> modeloNuevo(EscenarioDePrueba escenario, String etiqueta, int categoria,
                                               double precio, List<ArticuloDePrueba> articulos) {
        List<String> categorias = CatalogoAleatorio.categorias();
        catalogo.asegurarCategorias(categorias);
        long numero = catalogo.siguienteNumero();
        String nombre = "QA Prueba " + escenario.prueba() + (etiqueta.isBlank() ? "" : " " + etiqueta) + " #" + numero;
        ModeloDePrueba modelo = new ModeloDePrueba(
                CatalogoAleatorio.codigoBarras(numero), nombre,
                "Para la prueba " + escenario.prueba() + (etiqueta.isBlank() ? "" : " (" + etiqueta + ")") + " — datos de prueba",
                categorias.get(categoria % categorias.size()), "Negro", Math.round(precio * 0.4), precio, 0.0, articulos);
        return catalogo.guardarLote(List.of(modelo));
    }

    private CatalogoPreparado dato(String clave, String tipo, ArticuloGuardado articulo, String queEs) {
        DatosDelModelo m = escenarios.datosDelModelo(articulo.id());
        return new CatalogoPreparado(clave, tipo, m.modeloId(), articulo.id(), m.nombre(), m.codigoBarras(), queEs);
    }
}
