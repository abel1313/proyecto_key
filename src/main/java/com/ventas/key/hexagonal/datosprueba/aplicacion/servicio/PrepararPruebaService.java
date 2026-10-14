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
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.NombresDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.PedidoPreparado;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.PruebaQuitada;
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
 * pantalla (R8). Solo en la base de QA (R1).
 *
 * <p>R18 (2026-10-13, pedido del dueño: "dar de alta lo que se necesita para las pruebas"): también
 * los modelos y artículos que pide cada prueba (Art-A, Art-200, M-OFF, un modelo agotado...), con el
 * generador del catálogo de prueba y dejados en el estado que pide la prueba. R19: "Preparar todas".
 *
 * <p>R20 (pedido del dueño: "en la venta se pone el nombre exacto de la prueba para que coincida en todos
 * lados"): el cliente de cada pedido se llama como la prueba y la clave ("Prueba 24 K4") y los modelos
 * como el documento ("QA Prueba 17 Art-A"); los nombres salen de {@link NombresDePrueba}.
 *
 * <p>R21 (pedido del dueño: "si sale mal, en el mismo código eliminar lo que se metió y volverlo a meter
 * para reiniciar la prueba"): preparar una prueba primero quita lo de la vez anterior. Los pedidos se
 * cancelan con el servicio real (el stock regresa) y los modelos y artículos se dan de baja; nada se borra,
 * como en el resto del sistema (hay historial y 13 tablas que apuntan a un artículo).
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
        EscenarioPreparado listo = reiniciar(escenario, usuarioId);
        escenarios.limpiarCache();
        return listo;
    }

    @Override
    public List<EscenarioPreparado> prepararTodas(int usuarioId) {
        exigirBaseDePruebas();
        List<EscenarioPreparado> todas = new ArrayList<>();
        for (EscenarioDePrueba escenario : EscenarioDePrueba.values()) {
            try {
                todas.add(reiniciar(escenario, usuarioId));
            } catch (RuntimeException e) {
                // R19: una que falla no detiene a las demás; se reporta con su motivo.
                log.warn("Prueba {} no se pudo preparar: {}", escenario.prueba(), e.getMessage(), e);
                todas.add(EscenarioPreparado.conError(escenario, e.getMessage()));
            }
        }
        escenarios.limpiarCache();
        return todas;
    }

    @Override
    public PruebaQuitada quitar(String prueba) {
        EscenarioDePrueba escenario = EscenarioDePrueba.de(prueba);
        exigirBaseDePruebas();
        PruebaQuitada quitada = quitarLoAnterior(escenario);
        escenarios.limpiarCache();
        return quitada;
    }

    @Override
    public List<PruebaQuitada> quitarTodas() {
        exigirBaseDePruebas();
        List<PruebaQuitada> todas = new ArrayList<>();
        for (EscenarioDePrueba escenario : EscenarioDePrueba.values()) {
            try {
                todas.add(quitarLoAnterior(escenario));
            } catch (RuntimeException e) {
                log.warn("Prueba {} no se pudo quitar: {}", escenario.prueba(), e.getMessage(), e);
                todas.add(PruebaQuitada.conError(escenario, e.getMessage()));
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

    /** R21: quita lo de la vez anterior y vuelve a crear la prueba. */
    private EscenarioPreparado reiniciar(EscenarioDePrueba escenario, int usuarioId) {
        PruebaQuitada quitada = quitarLoAnterior(escenario);
        return armar(escenario, usuarioId).conQueSeQuito(quitada.resumen());
    }

    /**
     * Cancela los pedidos de la prueba (primero: al cancelar regresa el stock a los artículos que todavía
     * existen) y da de baja sus modelos. Un pedido que no se puede cancelar no detiene a los demás.
     */
    private PruebaQuitada quitarLoAnterior(EscenarioDePrueba escenario) {
        int cancelados = 0;
        int sinCancelar = 0;
        for (int pedidoId : escenarios.pedidosDeLaPrueba(escenario.prueba())) {
            try {
                escenarios.cancelar(pedidoId);
                cancelados++;
            } catch (RuntimeException e) {
                sinCancelar++;
                log.warn("Prueba {}: no se pudo cancelar el pedido {}: {}", escenario.prueba(), pedidoId, e.getMessage());
            }
        }
        int modelos = escenarios.darDeBajaCatalogoDeLaPrueba(escenario.prueba());
        PruebaQuitada quitada = new PruebaQuitada(escenario.prueba(), escenario.titulo(), cancelados, sinCancelar, modelos, null);
        log.info("Prueba {} quitada: {} pedidos cancelados, {} sin cancelar, {} modelos dados de baja",
                escenario.prueba(), cancelados, sinCancelar, modelos);
        return quitada;
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
            case P26 -> prueba26(lineaDeUnArticuloNuevo(escenario), usuarioId);
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
        int t1 = escenarios.ventaDirecta("NORMAL", e.prueba(), "T1", usuarioId, List.of(linea), null, true);
        int t2 = escenarios.ventaDirecta("FIADO", e.prueba(), "T2", usuarioId, List.of(linea), null, true);
        pedidos.abonar(t2, 50.0, usuarioId);
        return new EscenarioPreparado(e.prueba(), e.titulo(), List.of(
                pedido(e, "T1", t1, "🛒 Venta de contado de Art-200, ya cobrada: su ticket (12.7 paso 1)"),
                pedido(e, "T2", t2, "💳 Ir pagando de Art-200 con un abono de $50: ticket de abono (12.7 paso 2)")),
                List.of(dato("Art-200", CatalogoPreparado.ARTICULO, art200, "Artículo de $200 con stock 10, para Venta directa (12.6)")),
                "12.6: en Venta directa escribe \"Art-200\". 12.7: en Mis pedidos escribe \"Prueba 12\" e imprime el ticket de T1 y de T2 desde su tarjeta (🖨).",
                null);
    }

    /** 14-QA: un modelo con stock 3 y un artículo con stock 1 con foto: 🧩 Productos dice "Puedes crear: 2". */
    private EscenarioPreparado prueba14() {
        EscenarioDePrueba e = EscenarioDePrueba.P14;
        ArticuloGuardado a = modeloNuevo(e, "Productos", 0, 150.0, List.of(new ArticuloDePrueba("M", "Negro", 1))).get(0);
        escenarios.stockDelModelo(a.id(), 3);
        return new EscenarioPreparado(e.prueba(), e.titulo(), List.of(),
                List.of(dato("M3", CatalogoPreparado.MODELO, a, "Modelo con stock 3 y un artículo con stock 1 con foto")),
                "Catálogo → 🔍 Modelos → escribe \"QA Prueba 14\" → 🧩 Productos. Sigue la 14-QA desde el paso 1.", null);
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
                "Paso 3: Tienda → filtro No habilitadas → escribe \"QA Prueba 15\". Pasos 2b y 2c: Agregar producto con \"QA Prueba 15 M5\" y \"QA Prueba 15 M10\".", null);
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
                "17.1: Tienda → escribe \"QA Prueba 17 Filtros\". 17.2: precio $100 → salen Art-A y Descuento. 17.3: Venta directa → \"Stock-1\".", null);
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
                "Catálogo → 🧩 Agregar producto → escribe \"QA Prueba 18 M-OFF\" y \"QA Prueba 18 M-0\". Sigue la 18.1.", null);
    }

    /**
     * 16: en una zona, dos 🕓 Pendiente de clientes con cuenta (A y B), un Ir pagando con $50 que todavia
     * no se lleva (C) y un Contado que todavia no se lleva (D).
     */
    private EscenarioPreparado prueba16(LineaDePedido linea, int usuarioId) {
        EscenarioDePrueba e = EscenarioDePrueba.P16;
        int zona = escenarios.zonaDePrueba();
        int a = escenarios.pedidoDelCliente(e.prueba(), "A", List.of(linea), zona);
        int b = escenarios.pedidoDelCliente(e.prueba(), "B", List.of(linea), zona);
        int c = escenarios.ventaDirecta("FIADO", e.prueba(), "C", usuarioId, List.of(linea), zona, false);
        pedidos.abonar(c, 50.0, usuarioId);
        int d = escenarios.ventaDirecta("NORMAL", e.prueba(), "D", usuarioId, List.of(linea), zona, false);
        return new EscenarioPreparado(e.prueba(), e.titulo(), List.of(
                pedido(e, "A", a, "🕓 Pendiente, zona \"Zona Prueba QA\""),
                pedido(e, "B", b, "🕓 Pendiente, misma zona"),
                pedido(e, "C", c, "💳 Ir pagando con $50, todavía no se lo lleva, misma zona"),
                pedido(e, "D", d, "🛒 Contado, todavía no se lo lleva, misma zona")),
                "Sigue la 16 desde el paso 1: en Mis pedidos escribe \"Prueba 16\". En el paso 8 elige la zona \"Zona Prueba QA\" (en vez de Tejupilco).");
    }

    /** 20.2: dos pedidos 🕓 Pendiente, para unirlos y cambiar su forma de cobro; y 20.3: un modelo agotado. */
    private EscenarioPreparado prueba20(LineaDePedido linea) {
        EscenarioDePrueba e = EscenarioDePrueba.P20;
        // Con zona: un 🕓 Pendiente con zona no tiene fecha para recoger y el cancelador automático no
        // lo toca (los que recogen en el local se cancelan solos a los 2 días de esa fecha).
        int zona = escenarios.zonaDePrueba();
        int p1 = escenarios.pedidoDelCliente(e.prueba(), "P1", List.of(linea), zona);
        int p2 = escenarios.pedidoDelCliente(e.prueba(), "P2", List.of(linea), zona);
        ArticuloGuardado agotado = modeloNuevo(e, "Agotado", 0, 150.0,
                List.of(new ArticuloDePrueba("M", "Negro", 0))).get(0);
        return new EscenarioPreparado(e.prueba(), e.titulo(), List.of(
                pedido(e, "P1", p1, "🕓 Pendiente, $150"),
                pedido(e, "P2", p2, "🕓 Pendiente, $150")),
                List.of(dato("Agotado", CatalogoPreparado.MODELO, agotado, "Modelo con stock 0 (agotado), para la 20.3")),
                "20.2: abre P1 → 🔗 Unir con otros pedidos → escribe \"Prueba 20 P2\" → Unir 2 pedidos. 20.3: Catálogo → 🔍 Modelos → \"QA Prueba 20 Agotado\".",
                null);
    }

    /**
     * 21: una zona con dos Apartados que deben (P1 y P3) y un Ir pagando ya pagado que falta entregar (P2), todos sin
     * fecha. Se venden como el dueño los vio fallar (2026-10-13): Venta directa sin registrar al cliente, con su
     * nombre escrito en "nombre del receptor". El pedido queda con la cuenta del administrador y ese nombre; la
     * columna Cliente de Entregas por zona tiene que enseñar el nombre escrito, no el de la cuenta. P3 es el
     * segundo Apartado: sirve para buscarlo por su nombre escrito desde 🔗 Unir de P1 (solo se unen de la misma forma de cobro).
     */
    private EscenarioPreparado prueba21(LineaDePedido linea, int usuarioId) {
        EscenarioDePrueba e = EscenarioDePrueba.P21;
        int zona = escenarios.zonaDePrueba();
        int p1 = escenarios.ventaDirectaConNombreEscrito("APARTADO", e.prueba(), "P1", usuarioId, List.of(linea), zona, false);
        int p2 = escenarios.ventaDirectaConNombreEscrito("FIADO", e.prueba(), "P2", usuarioId, List.of(linea), zona, false);
        int p3 = escenarios.ventaDirectaConNombreEscrito("APARTADO", e.prueba(), "P3", usuarioId, List.of(linea), zona, false);
        pedidos.abonar(p2, linea.subtotal(), usuarioId);
        String escrito = "; nombre escrito en la venta, cuenta del administrador";
        return new EscenarioPreparado(e.prueba(), e.titulo(), List.of(
                pedido(e, "P1", p1, "📦 Apartado que debe $150, zona \"Zona Prueba QA\", sin fecha" + escrito),
                pedido(e, "P2", p2, "💳 Ir pagando ya pagado completo que falta entregar, misma zona, sin fecha" + escrito),
                pedido(e, "P3", p3, "📦 Apartado que debe $150, misma zona, sin fecha" + escrito)),
                "En Entregas por zona elige la zona \"Zona Prueba QA\" y toca \"Esta semana\": la columna Cliente debe decir \"Prueba 21 P1\", \"Prueba 21 P2\" y \"Prueba 21 P3\", no tu nombre. En Mis pedidos escribe \"Prueba 21\".");
    }

    /** 22: un 🕓 Pendiente (PE), un Apartado que debe (AP) y uno cancelado (CA). */
    private EscenarioPreparado prueba22(LineaDePedido linea, int usuarioId) {
        EscenarioDePrueba e = EscenarioDePrueba.P22;
        int pe = escenarios.pedidoDelCliente(e.prueba(), "PE", List.of(linea), escenarios.zonaDePrueba());
        int ap = escenarios.ventaDirecta("APARTADO", e.prueba(), "AP", usuarioId, List.of(linea), null, false);
        int ca = escenarios.ventaDirecta("APARTADO", e.prueba(), "CA", usuarioId, List.of(linea), null, false);
        escenarios.cancelar(ca);
        return new EscenarioPreparado(e.prueba(), e.titulo(), List.of(
                pedido(e, "PE", pe, "🕓 Pendiente"),
                pedido(e, "AP", ap, "📦 Apartado que debe $150"),
                pedido(e, "CA", ca, "❌ Apartado cancelado (el cliente avisó)")),
                "Abre Mis pedidos, escribe \"Prueba 22\" y sigue la 22.1 (CA sale con ❌ Cancelado).");
    }

    /**
     * 24 (lo de K4/K5/K6 del 2026-10-13): dos Ir pagando que ya se lo llevaron, uno sin dinero (K4) y otro
     * con $40 (K6). Desde el detalle de K4, "Unir" tiene que encontrar a K6.
     */
    private EscenarioPreparado prueba24(LineaDePedido linea, int usuarioId) {
        EscenarioDePrueba e = EscenarioDePrueba.P24;
        int k4 = escenarios.ventaDirecta("FIADO", e.prueba(), "K4", usuarioId, List.of(linea), null, true);
        int k6 = escenarios.ventaDirecta("FIADO", e.prueba(), "K6", usuarioId, List.of(linea), null, true);
        pedidos.abonar(k6, 40.0, usuarioId);
        return new EscenarioPreparado(e.prueba(), e.titulo(), List.of(
                pedido(e, "K4", k4, "💳 Ir pagando sin dinero, debe $150"),
                pedido(e, "K6", k6, "💳 Ir pagando que ya dio $40, debe $110")),
                "Abre K4 → 🔗 Unir con otros pedidos → escribe \"Prueba 24 K6\". Tiene que salir.");
    }

    /**
     * 26 (caché de Mis pedidos, R18 de busquedapedido): un pedido de cada movimiento que tiene que verse al
     * instante en la lista. IP se abona, PE cambia de forma de cobro, PF se entrega (SQL directo) y AP se cancela.
     */
    private EscenarioPreparado prueba26(LineaDePedido linea, int usuarioId) {
        EscenarioDePrueba e = EscenarioDePrueba.P26;
        int zona = escenarios.zonaDePrueba();
        int ip = escenarios.ventaDirecta("FIADO", e.prueba(), "IP", usuarioId, List.of(linea), zona, false);
        pedidos.abonar(ip, 50.0, usuarioId);
        int pe = escenarios.pedidoDelCliente(e.prueba(), "PE", List.of(linea), zona);
        int pf = escenarios.ventaDirecta("NORMAL", e.prueba(), "PF", usuarioId, List.of(linea), zona, false);
        int ap = escenarios.ventaDirecta("APARTADO", e.prueba(), "AP", usuarioId, List.of(linea), zona, false);
        return new EscenarioPreparado(e.prueba(), e.titulo(), List.of(
                pedido(e, "IP", ip, "💳 Ir pagando con $50, debe $100"),
                pedido(e, "PE", pe, "🕓 Pendiente, $150"),
                pedido(e, "PF", pf, "✅ Pagado de contado, falta entregar"),
                pedido(e, "AP", ap, "📦 Apartado que debe $150")),
                "Abre Mis pedidos y escribe \"Prueba 26\": salen las 4 tarjetas. Sigue la 26.1.");
    }

    /** Un modelo de prueba con un solo articulo y stock suficiente para todos los pedidos de la prueba. */
    private LineaDePedido lineaDeUnArticuloNuevo(EscenarioDePrueba escenario) {
        ArticuloGuardado articulo = modeloNuevo(escenario, "", 0, PRECIO,
                List.of(new ArticuloDePrueba("M", "Negro", STOCK))).get(0);
        return new LineaDePedido(articulo.id(), 1, articulo.precio());
    }

    /**
     * Un modelo de prueba nuevo con el nombre exacto de la prueba ("QA Prueba 17 Art-A", R20; marca "Prueba QA",
     * foto real de QA si hay, R7) con sus artículos, en el orden en que se piden. Regresa los artículos guardados.
     */
    private List<ArticuloGuardado> modeloNuevo(EscenarioDePrueba escenario, String etiqueta, int categoria,
                                               double precio, List<ArticuloDePrueba> articulos) {
        List<String> categorias = CatalogoAleatorio.categorias();
        catalogo.asegurarCategorias(categorias);
        long numero = catalogo.siguienteNumero();
        ModeloDePrueba modelo = new ModeloDePrueba(
                CatalogoAleatorio.codigoBarras(numero), NombresDePrueba.modelo(escenario.prueba(), etiqueta),
                "Para la prueba " + escenario.prueba() + (etiqueta.isBlank() ? "" : " (" + etiqueta + ")") + " — datos de prueba",
                categorias.get(categoria % categorias.size()), "Negro", Math.round(precio * 0.4), precio, 0.0, articulos);
        return catalogo.guardarLote(List.of(modelo));
    }

    private PedidoPreparado pedido(EscenarioDePrueba escenario, String clave, int pedidoId, String queEs) {
        return new PedidoPreparado(clave, pedidoId, queEs, NombresDePrueba.cliente(escenario.prueba(), clave));
    }

    private CatalogoPreparado dato(String clave, String tipo, ArticuloGuardado articulo, String queEs) {
        DatosDelModelo m = escenarios.datosDelModelo(articulo.id());
        return new CatalogoPreparado(clave, tipo, m.modeloId(), articulo.id(), m.nombre(), m.codigoBarras(), queEs);
    }
}
