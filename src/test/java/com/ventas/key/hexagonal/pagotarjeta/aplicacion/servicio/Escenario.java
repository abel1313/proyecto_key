package com.ventas.key.hexagonal.pagotarjeta.aplicacion.servicio;

import com.ventas.key.hexagonal.pagotarjeta.dominio.excepcion.PagoConTarjetaException;
import com.ventas.key.hexagonal.pagotarjeta.dominio.modelo.*;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Adaptadores falsos en memoria para probar las reglas sin Mercado Pago ni base de datos.
 *
 * <p>Los falsos IMITAN a los reales en lo que importa: el repositorio aplica el candado de version y el
 * "un activo por pedido" como lo hace la base, devuelve el activo MAS RECIENTE y se le puede pedir que falle;
 * la pasarela puede crear el cobro y luego perder la respuesta, y recuerda las claves de idempotencia.
 */
class Escenario {

    static final Instant T0 = Instant.parse("2026-10-21T10:00:00.000Z");

    final RelojFalso reloj = new RelojFalso(T0);
    final PedidosFalsos pedidos = new PedidosFalsos();
    final IntentosFalsos intentos = new IntentosFalsos();
    final PlanesFalsos planes = new PlanesFalsos();
    final PasarelaFalsa pasarela = new PasarelaFalsa();
    final PedidoActualizadoFalso pedidoActualizado = new PedidoActualizadoFalso(pedidos);
    final DevolucionesFalsas devoluciones = new DevolucionesFalsas();
    final TransaccionFalsa transaccion = new TransaccionFalsa();

    final CobroConTarjetaService cobro =
            new CobroConTarjetaService(pedidos, intentos, planes, pasarela, pedidoActualizado, transaccion, reloj);
    final DevolverCobroService devolucion =
            new DevolverCobroService(intentos, devoluciones, pasarela, pedidoActualizado, transaccion, reloj);
    final OfrecerPlanesService ofertas = new OfrecerPlanesService(pedidos, planes);

    static PedidoACobrar pedido(int id, TipoDeCobro tipo, String total, boolean... aceptaMsi) {
        List<ArticuloACobrar> arts = new ArrayList<>();
        for (int i = 0; i < aceptaMsi.length; i++) {
            arts.add(new ArticuloACobrar(100 + i, "Artículo " + i, aceptaMsi[i]));
        }
        return new PedidoACobrar(id, 50, tipo, new BigDecimal(total), false, false, false, null, arts);
    }

    static class RelojFalso extends Clock {
        Instant ahora;
        RelojFalso(Instant ahora) { this.ahora = ahora; }
        void avanzarMinutos(long m) { ahora = ahora.plusSeconds(m * 60); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    static class PedidosFalsos implements PedidoACobrarPort {
        final Map<Integer, PedidoACobrar> porId = new HashMap<>();
        final java.util.Set<Integer> conCobroDelCaminoAnterior = new java.util.HashSet<>();
        void agregar(PedidoACobrar p) { porId.put(p.pedidoId(), p); }
        @Override public Optional<PedidoACobrar> buscar(Integer id) { return Optional.ofNullable(porId.get(id)); }
        @Override public boolean hayCobroEnProcesoPorElCaminoAnterior(Integer id) { return conCobroDelCaminoAnterior.contains(id); }
    }

    /** Imita la base: version optimista, un solo activo por pedido, activo mas reciente primero. */
    static class IntentosFalsos implements IntentosDePagoPort {
        final Map<Long, IntentoDePago> porId = new LinkedHashMap<>();
        long siguiente = 1;
        /** Cuantos guardados siguientes deben fallar (simula caida de la base). 0 = ninguno. */
        int fallarProximosGuardados = 0;
        /** Hace fallar el guardado numero N (contando desde 1) de toda la prueba; -1 = ninguno. */
        int fallarEnElGuardadoNumero = -1;
        int llamadasAGuardar = 0;
        /** Antes del proximo guardado, otro proceso "se adelanta" y cambia el mismo intento. */
        java.util.function.Consumer<IntentoDePago> otroProcesoSeAdelanta = null;

        @Override public IntentoDePago guardar(IntentoDePago i) {
            llamadasAGuardar++;
            if (llamadasAGuardar == fallarEnElGuardadoNumero) {
                throw new IllegalStateException("la base se cayo");
            }
            if (fallarProximosGuardados > 0) {
                fallarProximosGuardados--;
                throw new IllegalStateException("la base se cayo");
            }
            if (otroProcesoSeAdelanta != null && i.id() != null) {
                var accion = otroProcesoSeAdelanta;
                otroProcesoSeAdelanta = null;
                accion.accept(i);
            }
            if (i.id() == null) {
                if (i.estado().estaActivo() && activoDelPedido(i.pedidoId()).isPresent()) {
                    throw PagoConTarjetaException.intentoActivo(i.pedidoId());          // el indice unico de la base
                }
                IntentoDePago g = i.conId(siguiente++);
                porId.put(g.id(), g);
                return g;
            }
            IntentoDePago actual = porId.get(i.id());
            if (actual.version() != i.version()) {
                throw new PagoConTarjetaException.ConflictoDeConcurrencia();          // @Version
            }
            IntentoDePago g = IntentoDePago.reconstruir(i.id(), i.pedidoId(), i.clienteId(), i.canal(), i.monto(),
                    i.cuotas(), i.estado(), i.idExterno(), i.iniciadoPor(), i.creadoEn(), i.actualizadoEn(),
                    i.devuelto(), i.version() + 1);
            porId.put(g.id(), g);
            return g;
        }
        @Override public Optional<IntentoDePago> buscar(Long id) { return Optional.ofNullable(porId.get(id)); }
        @Override public Optional<IntentoDePago> activoDelPedido(Integer pedidoId) {
            return porId.values().stream()                                              // el MAS RECIENTE, como la base
                    .filter(i -> i.pedidoId().equals(pedidoId) && i.estado().estaActivo())
                    .reduce((a, b) -> b);
        }
        @Override public long rechazadosDesde(Integer pedidoId, Instant desde) {
            return porId.values().stream()
                    .filter(i -> i.pedidoId().equals(pedidoId) && i.estado() == EstadoIntento.RECHAZADO
                            && !i.actualizadoEn().isBefore(desde)).count();
        }
        @Override public List<IntentoDePago> perdidosDesde(Instant desde) {
            return porId.values().stream()
                    .filter(i -> i.estado().admiteAprobacionTardia() && !i.actualizadoEn().isBefore(desde)).toList();
        }
        @Override public List<IntentoDePago> activosCreadosAntesDe(Instant limite) {
            return porId.values().stream()
                    .filter(i -> i.estado().estaActivo() && i.creadoEn().isBefore(limite)).toList();
        }
        IntentoDePago ultimo() { return porId.values().stream().reduce((a, b) -> b).orElseThrow(); }
        long contar(EstadoIntento e) { return porId.values().stream().filter(i -> i.estado() == e).count(); }
    }

    static class PlanesFalsos implements PlanesMsiPort {
        List<PlanMsi> planes = List.of(new PlanMsi(3, true, new BigDecimal("3.5")), new PlanMsi(6, true, new BigDecimal("5.5")));
        BigDecimal minimo = new BigDecimal("300");
        @Override public List<PlanMsi> planes() { return planes; }
        @Override public BigDecimal minimoParaMsi() { return minimo; }
    }

    static class PasarelaFalsa implements PasarelaDePagoPort {
        boolean caida = false;
        /** El cobro SE CREA en la pasarela pero la respuesta se pierde (tiempo agotado). */
        boolean creaYPierdeLaRespuesta = false;
        /** La pasarela garantiza que no creo nada (rechazo claro). */
        boolean rechazaAlCrear = false;
        /** cancelar() falla (por ejemplo porque el cobro ya esta aprobado). */
        boolean cancelarFalla = false;
        EstadoExterno respuesta = EstadoExterno.PENDIENTE;
        int cobrosCreados = 0, cancelaciones = 0, consultas = 0;
        final Map<Long, CobroEnPasarela> porReferencia = new HashMap<>();
        final Set<String> clavesDevolucion = new HashSet<>();
        final List<BigDecimal> devoluciones = new ArrayList<>();
        final List<String> clavesUsadas = new ArrayList<>();
        BigDecimal montoCobrado;

        @Override public String crearCobro(IntentoDePago i) {
            if (caida) throw new IllegalStateException("sin red");
            if (rechazaAlCrear) throw new PagoConTarjetaException.CobroNoCreado("Monto inválido");
            cobrosCreados++;
            montoCobrado = i.monto();
            String id = "ext-" + cobrosCreados;
            porReferencia.put(i.id(), new CobroEnPasarela(id, EstadoExterno.PENDIENTE));
            if (creaYPierdeLaRespuesta) throw new IllegalStateException("tiempo agotado leyendo la respuesta");
            return id;
        }
        @Override public EstadoExterno consultar(String idExterno) {
            if (caida) throw new IllegalStateException("sin red");
            consultas++;
            return respuesta;
        }
        @Override public Optional<CobroEnPasarela> buscarPorReferencia(Long intentoId) {
            if (caida) throw new IllegalStateException("sin red");
            CobroEnPasarela c = porReferencia.get(intentoId);
            return c == null ? Optional.empty() : Optional.of(new CobroEnPasarela(c.idExterno(), respuesta));
        }
        @Override public void cancelar(String idExterno) {
            if (caida || cancelarFalla) throw new IllegalStateException("no se pudo cancelar");
            cancelaciones++;
        }
        @Override public void devolver(String idExterno, BigDecimal monto, String clave) {
            if (caida) throw new IllegalStateException("sin red");
            clavesUsadas.add(clave);
            if (clavesDevolucion.add(clave)) devoluciones.add(monto);                    // misma clave: no repite
        }
    }

    /** Contrato: ambos metodos son idempotentes. Al marcar pagado, el pedido falso tambien queda pagado. */
    static class PedidoActualizadoFalso implements ActualizarPedidoPort {
        final PedidosFalsos pedidos;
        final Set<Long> intentosPagados = new LinkedHashSet<>();
        int llamadasMarcarPagado = 0;
        boolean marcarPagadoFalla = false;
        /** Faltan datos para registrar la venta (problema de datos, no pasajero). */
        boolean faltanDatos = false;
        /** El pedido, ya bloqueado, no admite el pago (cancelado, abonado...). */
        boolean noAplicable = false;
        final Map<String, BigDecimal> devueltos = new LinkedHashMap<>();
        PedidoActualizadoFalso(PedidosFalsos pedidos) { this.pedidos = pedidos; }
        @Override public ResultadoAplicacion marcarPagado(Integer pedidoId, Long intentoId) {
            llamadasMarcarPagado++;
            if (marcarPagadoFalla) throw new IllegalStateException("el pedido no se pudo actualizar");
            if (faltanDatos) throw new PagoConTarjetaException.NoSePudoAplicar("No existe el usuario");
            PedidoACobrar p = pedidos.porId.get(pedidoId);
            if (noAplicable || p.pagado() || p.cancelado()) return ResultadoAplicacion.NO_APLICABLE;
            intentosPagados.add(intentoId);
            pedidos.porId.put(pedidoId, new PedidoACobrar(p.pedidoId(), p.clienteId(), p.tipo(), p.total(), true, false, false,
                    p.pagadoPorIntentoId() != null ? p.pagadoPorIntentoId() : intentoId, p.articulos()));
            return ResultadoAplicacion.APLICADO;
        }
        @Override public void registrarDevolucion(Integer pedidoId, Long intentoId, BigDecimal monto, String hechaPor, String clave) {
            devueltos.putIfAbsent(clave, monto);
        }
    }

    /** La base de devoluciones: clave unica, como uq_ptd_clave. */
    static class DevolucionesFalsas implements DevolucionesPort {
        final Map<Long, Devolucion> porId = new LinkedHashMap<>();
        long siguiente = 1;
        @Override public Devolucion reservar(Devolucion d) {
            if (porId.values().stream().anyMatch(x -> x.clave().equals(d.clave()))) {
                throw new PagoConTarjetaException.ConflictoDeConcurrencia();
            }
            Devolucion g = d.conId(siguiente++);
            porId.put(g.id(), g);
            return g;
        }
        @Override public Devolucion confirmar(Devolucion d) {
            Devolucion c = porId.get(d.id()).comoConfirmada();
            porId.put(c.id(), c);
            return c;
        }
        @Override public List<Devolucion> pendientesDelIntento(Long intentoId) {
            return porId.values().stream().filter(d -> d.intentoId().equals(intentoId) && !d.confirmada()).toList();
        }
    }

    /** Sin base real no hay rollback: solo se ejecuta la accion (el comportamiento atomico se prueba en el adaptador). */
    static class TransaccionFalsa implements TransaccionPort {
        int ejecuciones = 0;
        @Override public <T> T ejecutar(java.util.function.Supplier<T> accion) {
            ejecuciones++;
            return accion.get();
        }
    }
}
