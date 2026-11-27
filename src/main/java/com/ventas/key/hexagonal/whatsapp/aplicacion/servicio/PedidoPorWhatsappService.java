package com.ventas.key.hexagonal.whatsapp.aplicacion.servicio;

import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ArticuloParaPedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.BorradorDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.Confirmacion;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.DatosConocidos;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.Falta;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.HorarioDeAtencion;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.LineaDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.LineaSolicitada;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.LugarDeEntrega;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.RespuestaDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ResultadoDeCreacion;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.SolicitudDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.Telefonos;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.TextosDePedido;
import com.ventas.key.hexagonal.whatsapp.dominio.modelo.ValidadorDeSolicitud;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.PedidoPorWhatsappCasoUso;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.BorradoresDePedidoPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.CatalogoParaPedidoPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.DatosConocidosPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.HorarioDeAtencionPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.LugaresDeEntregaPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.NotificacionesDePedidoPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.PedidosApartadoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El pedido por WhatsApp (reglas R16 a R21 de {@code whatsapp/README.md}).
 *
 * <p>El modelo de IA solo PROPONE ({@code ##PEDIDO{...}##}); aquí el sistema valida contra la base, arma él mismo el
 * resumen con los precios de la base y espera un «sí» limpio del cliente antes de crear el Apartado con el servicio
 * real de ventas. Así ni el precio, ni el artículo, ni la confirmación dependen de lo que escriba la IA.</p>
 *
 * <p>Quien lo llama serializa los mensajes de un mismo número (R8), así que un cliente nunca tiene dos de estos
 * métodos corriendo a la vez.</p>
 *
 * <p>[Hexagonal: Application Service] [Clean: Use Case Interactor]</p>
 */
public class PedidoPorWhatsappService implements PedidoPorWhatsappCasoUso {

    private static final Logger log = LoggerFactory.getLogger(PedidoPorWhatsappService.class);

    private static final int MAX_NOMBRE = 100;
    private static final int MAX_CORREO = 120;
    private static final int MAX_DIRECCION = 250;
    private static final int MAX_NOTA = 300;

    private final CatalogoParaPedidoPort catalogo;
    private final LugaresDeEntregaPort lugares;
    private final BorradoresDePedidoPort borradores;
    private final PedidosApartadoPort pedidos;
    private final DatosConocidosPort conocidos;
    private final NotificacionesDePedidoPort notificaciones;
    private final Clock reloj;
    private final Duration vigenciaDelResumen;
    private final HorarioDeAtencionPort horario;
    private final int maxApartadosAbiertos;
    /** Cuántas veces seguidas faltó el teléfono: a la segunda se pasa a una persona en vez de preguntar en bucle (hallazgo M7). */
    private final ConcurrentHashMap<Long, Integer> intentosDeTelefono = new ConcurrentHashMap<>();

    /** Un «sí» escrito menos de estos segundos después de que salió el resumen no pudo ser una respuesta a él (hallazgo C1). */
    static final Duration MARGEN_DEL_SI = Duration.ofSeconds(3);

    public PedidoPorWhatsappService(CatalogoParaPedidoPort catalogo, LugaresDeEntregaPort lugares,
                                    BorradoresDePedidoPort borradores, PedidosApartadoPort pedidos,
                                    DatosConocidosPort conocidos, NotificacionesDePedidoPort notificaciones,
                                    Clock reloj, Duration vigenciaDelResumen, HorarioDeAtencionPort horario,
                                    int maxApartadosAbiertos) {
        this.catalogo = catalogo;
        this.lugares = lugares;
        this.borradores = borradores;
        this.pedidos = pedidos;
        this.conocidos = conocidos;
        this.notificaciones = notificaciones;
        this.reloj = reloj;
        this.vigenciaDelResumen = vigenciaDelResumen;
        this.horario = horario;
        this.maxApartadosAbiertos = maxApartadosAbiertos;
    }

    @Override
    public Optional<RespuestaDePedido> alResponder(long clienteId, String telefonoWhatsapp, String nombrePerfil,
                                                   String mensajeDelCliente, Instant enviadoEn) {
        Optional<BorradorDePedido> pendiente = borradores.porCliente(clienteId);
        if (pendiente.isEmpty()) {
            return Optional.empty();
        }
        BorradorDePedido borrador = pendiente.get();
        Confirmacion.Resultado dijo = Confirmacion.evaluar(mensajeDelCliente);
        Instant cuando = enviadoEn != null ? enviadoEn : reloj.instant();
        Instant desde = borrador.resumenEnviadoEn() != null ? borrador.resumenEnviadoEn() : borrador.creadoEn();
        // C1: lo que el cliente escribió antes de poder leer el resumen (o a la vez) no es una respuesta a él. Un «sí» así
        // no cuenta; el resumen sigue esperando y se le pide que responda SÍ. Ejemplo: contestó «sí» a «¿este es tu correo?» y mandó «ok» enseguida.
        if (desde == null || cuando.isBefore(desde.plus(MARGEN_DEL_SI))) {
            log.info("WhatsApp cliente {}: mensaje escrito antes de poder leer el resumen, no cuenta", clienteId);
            // N1: un «sí» nunca se pierde en silencio: se le pide que lo repita (el resumen sigue vivo, no se descarta).
            return dijo == Confirmacion.Resultado.OTRA ? Optional.empty()
                    : Optional.of(new RespuestaDePedido(TextosDePedido.soloSi(), false));
        }
        if (!borrador.vigente(reloj.instant(), vigenciaDelResumen)) {
            borradores.descartar(clienteId);
            log.info("WhatsApp cliente {}: el resumen del pedido ya caducó", clienteId);
            return dijo == Confirmacion.Resultado.OTRA ? Optional.empty() : Optional.of(resumenVencido());
        }
        if (dijo == Confirmacion.Resultado.AMBIGUA) {
            // «ok?», «¿ya está listo?»: parece un sí pero es una pregunta. No se crea nada ni se descarta el resumen.
            return Optional.of(new RespuestaDePedido(TextosDePedido.soloSi(), false));
        }
        // El resumen se usa UNA sola vez: conteste lo que conteste el cliente, se descarta aquí. Así un reintento o un
        // mensaje repetido nunca puede crear dos pedidos.
        borradores.descartar(clienteId);
        if (dijo != Confirmacion.Resultado.SI) {
            return Optional.empty();
        }
        return Optional.of(crear(clienteId, borrador));
    }

    @Override
    public RespuestaDePedido proponer(long clienteId, String telefonoWhatsapp, SolicitudDePedido pedida) {
        SolicitudDePedido s = completarConDatosPrevios(pedida, telefonoWhatsapp);
        List<Falta> faltas = ValidadorDeSolicitud.faltas(s, telefonoWhatsapp);
        if (!faltas.isEmpty()) {
            if (faltas.contains(Falta.TELEFONO)) {
                int intentos = intentosDeTelefono.merge(clienteId, 1, Integer::sum);
                if (intentos >= 2) {
                    intentosDeTelefono.remove(clienteId);
                    return new RespuestaDePedido(TextosDePedido.sinTelefonoPasaAPersona(), true);
                }
            }
            return new RespuestaDePedido(TextosDePedido.faltan(faltas), false);
        }
        intentosDeTelefono.remove(clienteId);

        // I6: tope de Apartados abiertos por número (0 = sin tope).
        if (maxApartadosAbiertos > 0) {
            int abiertos = contarAbiertos(telefonoWhatsapp);
            if (abiertos >= maxApartadosAbiertos) {
                return topeAlcanzado(telefonoWhatsapp, abiertos, s.nombre());
            }
        }

        // Lugar: solo uno del catálogo de zonas (o recoger en el local).
        List<LugarDeEntrega> catalogoDeLugares = leerLugares();
        boolean recoger = s.recogerEnLocal();
        LugarDeEntrega lugar = null;
        if (recoger) {
            lugar = catalogoDeLugares.stream().filter(LugarDeEntrega::recogerEnLocal).findFirst().orElse(null);
        } else {
            lugar = buscarLugar(catalogoDeLugares, s.lugar());
            if (lugar == null) {
                List<String> zonas = catalogoDeLugares.stream().filter(l -> !l.recogerEnLocal())
                        .map(LugarDeEntrega::nombre).sorted(String.CASE_INSENSITIVE_ORDER).toList();
                boolean hayLocal = catalogoDeLugares.stream().anyMatch(LugarDeEntrega::recogerEnLocal);
                return new RespuestaDePedido(TextosDePedido.lugarNoValido(limpiar(s.lugar(), MAX_NOMBRE), zonas, hayLocal), false);
            }
            if (lugar.recogerEnLocal()) {
                recoger = true;   // dijo el nombre del local como si fuera un lugar de entrega
            }
        }

        // Artículos: el sistema decide si existen, si hay y a cuánto salen. El modelo solo dijo cuál.
        Map<Integer, Integer> pedidas = new LinkedHashMap<>();
        for (LineaSolicitada l : s.articulos()) {
            pedidas.merge(l.articuloId(), l.cantidad(), Integer::sum);
        }
        ArrayList<LineaDePedido> lineas = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : pedidas.entrySet()) {
            Optional<ArticuloParaPedido> articulo = leerArticulo(e.getKey());
            if (articulo.isEmpty() || articulo.get().esCatalogoInterno() || articulo.get().precioACobrar() <= 0) {
                return new RespuestaDePedido(TextosDePedido.articuloNoEncontrado(), false);
            }
            ArticuloParaPedido a = articulo.get();
            if (!a.habilitado() || !a.tieneImagen() || a.stock() < e.getValue()) {
                return new RespuestaDePedido(TextosDePedido.noDisponible(a.nombre(),
                        a.habilitado() && a.tieneImagen() && a.stock() > 0), false);
            }
            lineas.add(new LineaDePedido(a.id(), a.nombre(), e.getValue(), a.precioACobrar()));
        }

        BorradorDePedido borrador = new BorradorDePedido(lineas,
                limpiar(s.nombre(), MAX_NOMBRE), ValidadorDeSolicitud.telefonoElegido(s, telefonoWhatsapp),
                limpiar(s.correo(), MAX_CORREO).toLowerCase(), limpiar(s.recibe(), MAX_NOMBRE),
                recoger ? null : limpiar(s.direccion(), MAX_DIRECCION), lugar, recoger,
                limpiar(s.observaciones(), MAX_NOTA), s.irPagando(), digitos(telefonoWhatsapp),
                Telefonos.ligaDeWhatsapp(telefonoWhatsapp), reloj.instant(), null);
        // C1: el borrador NO se guarda aquí. Quien envía el resumen lo guarda solo si de verdad salió (resumenEnviado).
        return new RespuestaDePedido(TextosDePedido.resumen(borrador), false, borrador);
    }

    @Override
    public void resumenEnviado(long clienteId, BorradorDePedido borrador, Instant enviadoEn) {
        borradores.guardar(clienteId, borrador.conResumenEnviado(enviadoEn != null ? enviadoEn : reloj.instant()));
    }

    @Override
    public void descartar(long clienteId) {
        borradores.descartar(clienteId);
    }

    @Override
    public RespuestaDePedido resumenVencido() {
        return new RespuestaDePedido(TextosDePedido.resumenVencio(), false);
    }

    @Override
    public RespuestaDePedido solicitarCancelacion(String telefonoWhatsapp, String nombrePerfil, Integer numeroDePedido) {
        if (numeroDePedido == null || numeroDePedido <= 0) {
            return new RespuestaDePedido(TextosDePedido.cancelarSinNumero(), false);
        }
        try {
            notificaciones.cancelacionAlAdministrador(digitos(telefonoWhatsapp), limpiar(nombrePerfil, MAX_NOMBRE), numeroDePedido);
        } catch (RuntimeException e) {
            log.warn("WhatsApp: no se pudo avisar de la solicitud de cancelación del pedido {} ({})", numeroDePedido, e.toString());
        }
        // El bot no cancela ni dice nada del pedido: solo anota el número. La conversación queda marcada.
        return new RespuestaDePedido(TextosDePedido.pidioCancelar(numeroDePedido), true);
    }

    @Override
    public RespuestaDePedido marcaIlegible() {
        return new RespuestaDePedido(TextosDePedido.marcaRota(), false);
    }

    @Override
    public String contextoParaElModelo(String telefonoWhatsapp) {
        StringBuilder sb = new StringBuilder("DATOS DE ESTA CONVERSACIÓN (para tomar un pedido):\n");
        if (Telefonos.diezDigitos(telefonoWhatsapp) == null) {
            sb.append("- El número desde el que escribe el cliente NO sirve como teléfono de contacto (no es un número "
                    + "de México de 10 dígitos). NO le preguntes si usamos este número: pídele un teléfono de 10 dígitos "
                    + "una sola vez; si no lo tiene, pásalo a una persona con ##HUMANO##.\n");
        } else {
            sb.append("- Número desde el que escribe el cliente: +").append(digitos(telefonoWhatsapp))
                    .append(". Si le preguntas por su teléfono, puede usar este mismo (si dice que sí, usa ")
                    .append("\"telefonoDeWhatsapp\":true; si da otro, escríbelo en \"telefono\").\n");
        }
        Optional<DatosConocidos> previos = leerConocidos(telefonoWhatsapp);
        if (previos.isPresent()) {
            DatosConocidos d = previos.get();
            sb.append("- Este número ya hizo un pedido antes. Dio el nombre «").append(limpiar(d.nombre(), MAX_NOMBRE))
                    .append("», el correo ").append(d.correoEnmascarado());
            if (!d.direccionParcial().isEmpty()) {
                sb.append(" y una dirección que empieza con «").append(d.direccionParcial()).append("»");
            }
            sb.append(". Los datos completos los tiene el sistema, tú solo ves esto. Pregúntale si quiere usar los mismos "
                    + "(\"¿Sigues usando el correo ").append(d.correoEnmascarado()).append("?\"); si dice que sí, en la marca "
                    + "pon \"usarDatosPrevios\":true y deja vacío el campo que va a reutilizar (\"correo\" y, si quiere la "
                    + "misma dirección, \"direccion\"). Si dice que no, pídele el dato nuevo completo. Nunca los des por "
                    + "buenos sin su respuesta.\n");
        } else {
            sb.append("- Este número no nos ha dado datos antes: pídeselos todos.\n");
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------------------------------------------

    private RespuestaDePedido crear(long clienteId, BorradorDePedido borrador) {
        if (maxApartadosAbiertos > 0) {
            int abiertos = contarAbiertos(borrador.telefonoWhatsapp());
            if (abiertos >= maxApartadosAbiertos) {
                return topeAlcanzado(borrador.telefonoWhatsapp(), abiertos, borrador.nombre());
            }
        }
        // I1: entre el resumen y el «sí» el dueño pudo cambiar un precio o agotarse algo. Se revisa ANTES de crear: si cambió
        // el precio no es un error del sistema, se arma el resumen de nuevo con los precios de hoy y se vuelve a preguntar.
        ArrayList<LineaDePedido> deHoy = new ArrayList<>();
        boolean cambioElPrecio = false;
        for (LineaDePedido l : borrador.lineas()) {
            Optional<ArticuloParaPedido> a = leerArticulo(l.articuloId());
            if (a.isEmpty() || !a.get().seOfrece() || a.get().stock() < l.cantidad() || a.get().precioACobrar() <= 0) {
                return new RespuestaDePedido(TextosDePedido.yaNoSePudo(), false);
            }
            if (Math.abs(a.get().precioACobrar() - l.precioUnitario()) > 0.005) {
                cambioElPrecio = true;
            }
            deHoy.add(new LineaDePedido(l.articuloId(), a.get().nombre(), l.cantidad(), a.get().precioACobrar()));
        }
        if (cambioElPrecio) {
            BorradorDePedido nuevo = new BorradorDePedido(deHoy, borrador.nombre(), borrador.telefono(), borrador.correo(),
                    borrador.recibe(), borrador.direccion(), borrador.lugar(), borrador.recogerEnLocal(),
                    borrador.observacionesCliente(), borrador.irPagando(), borrador.telefonoWhatsapp(),
                    borrador.urlWhatsapp(), reloj.instant(), null);
            log.info("WhatsApp cliente {}: cambió el precio de algo del pedido, se vuelve a proponer", clienteId);
            return new RespuestaDePedido(TextosDePedido.precioCambio() + TextosDePedido.resumen(nuevo), false, nuevo);
        }

        ResultadoDeCreacion r;
        try {
            r = pedidos.crearApartado(borrador);
        } catch (RuntimeException e) {
            r = new ResultadoDeCreacion.Fallo(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        if (r instanceof ResultadoDeCreacion.Creado creado0) {
            // I1: ticket, correo y mensaje final con las líneas y el total REALES (pudo bajar por campaña o precio de grupo).
            ResultadoDeCreacion.Creado creado = creado0.lineas().isEmpty()
                    ? new ResultadoDeCreacion.Creado(creado0.pedidoId(), creado0.total(), lineasDelBorrador(borrador))
                    : creado0;
            boolean ticket = false;
            try {
                ticket = notificaciones.ticketAlCliente(borrador, creado);
            } catch (RuntimeException e) {
                log.warn("WhatsApp cliente {}: el ticket del pedido {} no salió ({})", clienteId, creado.pedidoId(), e.toString());
            }
            try {
                notificaciones.ventaAlAdministrador(borrador, creado);
            } catch (RuntimeException e) {
                log.warn("WhatsApp cliente {}: el correo de la venta {} no salió ({})", clienteId, creado.pedidoId(), e.toString());
            }
            log.info("WhatsApp cliente {}: pedido {} creado por el bot (Apartado)", clienteId, creado.pedidoId());
            return new RespuestaDePedido(TextosDePedido.creado(creado.pedidoId(), creado.total(), borrador, ticket, creado.lineas()), false);
        }
        if (r instanceof ResultadoDeCreacion.NoDisponible nd) {
            log.info("WhatsApp cliente {}: el pedido ya no se pudo hacer ({})", clienteId, nd.motivo());
            return new RespuestaDePedido(TextosDePedido.yaNoSePudo(), false);
        }
        String motivo = r instanceof ResultadoDeCreacion.Fallo f ? f.motivo() : "desconocido";
        log.warn("WhatsApp cliente {}: no se pudo crear el pedido ({})", clienteId, motivo);
        try {
            notificaciones.falloAlAdministrador(borrador, motivo);
        } catch (RuntimeException e) {
            log.warn("WhatsApp cliente {}: no se pudo avisar del fallo del pedido ({})", clienteId, e.toString());
        }
        return new RespuestaDePedido(TextosDePedido.falloAlCrear(horarioSiCerrado()), true);
    }

    private RespuestaDePedido topeAlcanzado(String telefonoWhatsapp, int abiertos, String nombre) {
        try {
            notificaciones.topeAlAdministrador(digitos(telefonoWhatsapp), limpiar(nombre, MAX_NOMBRE), abiertos);
        } catch (RuntimeException e) {
            log.warn("WhatsApp: no se pudo avisar del tope de apartados ({})", e.toString());
        }
        return new RespuestaDePedido(TextosDePedido.topeDeApartados(abiertos), true);
    }

    private static List<ResultadoDeCreacion.LineaCobrada> lineasDelBorrador(BorradorDePedido b) {
        return b.lineas().stream()
                .map(l -> new ResultadoDeCreacion.LineaCobrada(l.nombre(), l.cantidad(), l.precioUnitario(), l.precioUnitario()))
                .toList();
    }

    /** I7: el nombre y el correo (y la dirección) previos solo se usan si el cliente los confirmó, y vienen de ESTE número. */
    private SolicitudDePedido completarConDatosPrevios(SolicitudDePedido s, String telefonoWhatsapp) {
        if (!s.usarDatosPrevios()) {
            return s;
        }
        Optional<DatosConocidos> previos = leerConocidos(telefonoWhatsapp);
        if (previos.isEmpty()) {
            return s;
        }
        DatosConocidos d = previos.get();
        String nombre = s.nombre() == null || s.nombre().isBlank() ? d.nombre() : s.nombre();
        String correo = s.correo() == null || s.correo().isBlank() || s.correo().contains("*") ? d.correo() : s.correo();
        String direccion = (s.direccion() == null || s.direccion().isBlank() || s.direccion().contains("*"))
                && d.direccion() != null ? d.direccion() : s.direccion();
        return new SolicitudDePedido(s.articulos(), nombre, s.usarTelefonoDeWhatsapp(), s.telefono(), correo, s.recibe(),
                direccion, s.lugar(), s.recogerEnLocal(), s.observaciones(), s.irPagando(), s.usarDatosPrevios());
    }

    private int contarAbiertos(String telefonoWhatsapp) {
        try {
            return pedidos.contarAbiertosDesde(digitos(telefonoWhatsapp));
        } catch (RuntimeException e) {
            log.warn("WhatsApp: no se pudo contar los apartados abiertos ({})", e.toString());
            return 0;
        }
    }

    /** El horario SOLO si ahorita no hay nadie (para no prometer «muy pronto»); null si hay una persona o no hay horario. */
    private String horarioSiCerrado() {
        try {
            Optional<HorarioDeAtencion> h = horario.actual();
            return h.isPresent() && !h.get().abiertoEn(reloj.instant()) ? h.get().aTexto() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static LugarDeEntrega buscarLugar(List<LugarDeEntrega> todos, String dicho) {
        String clave = TextosDePedido.clave(dicho);
        if (clave.isEmpty()) {
            return null;
        }
        for (LugarDeEntrega l : todos) {
            if (TextosDePedido.clave(l.nombre()).equals(clave)) {
                return l;
            }
        }
        // «centro de Tejupilco» o «Tejupilco, Mex.»: sirve si apunta a UNA sola zona.
        List<LugarDeEntrega> parecidos = todos.stream().filter(l -> {
            String k = TextosDePedido.clave(l.nombre());
            return k.length() >= 4 && (clave.contains(k) || k.contains(clave) && clave.length() >= 4);
        }).toList();
        return parecidos.size() == 1 ? parecidos.get(0) : null;
    }

    private List<LugarDeEntrega> leerLugares() {
        try {
            List<LugarDeEntrega> l = lugares.todos();
            return l == null ? List.of() : l;
        } catch (RuntimeException e) {
            log.warn("WhatsApp: no se pudo leer el catálogo de lugares de entrega ({})", e.toString());
            return List.of();
        }
    }

    private Optional<ArticuloParaPedido> leerArticulo(int id) {
        try {
            return catalogo.articulo(id);
        } catch (RuntimeException e) {
            log.warn("WhatsApp: no se pudo leer el artículo {} ({})", id, e.toString());
            return Optional.empty();
        }
    }

    private Optional<DatosConocidos> leerConocidos(String numeroDeWhatsapp) {
        try {
            return conocidos.porNumeroDeWhatsapp(digitos(numeroDeWhatsapp));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static String digitos(String t) {
        return t == null ? "" : t.replaceAll("\\D", "");
    }

    /** Sin saltos de línea ni caracteres de control, espacios colapsados y con tope de largo (lo escribió un tercero). */
    static String limpiar(String t, int max) {
        if (t == null) {
            return "";
        }
        String s = t.replaceAll("[\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        return s.length() > max ? s.substring(0, max) : s;
    }
}
