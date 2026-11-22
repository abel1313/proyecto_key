package com.ventas.key.hexagonal.whatsapp.dominio.modelo;

import com.ventas.key.hexagonal.whatsapp.dominio.excepcion.PublicacionWhatsappException;

import java.time.LocalDateTime;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Una publicacion de WhatsApp y sus reglas de estado (PLAN_WHATSAPP_PUBLICACIONES.md, seccion 3):
 * <ul>
 *   <li>R1. PENDIENTE: lista para que el bot la recoja cuando llegue su hora.</li>
 *   <li>R2. El bot la recoge UNA sola vez: pasa a EN_PROCESO (evita publicar duplicado).</li>
 *   <li>R3. Publicada solo desde EN_PROCESO. Guarda la fecha de publicacion.</li>
 *   <li>R4. Error desde EN_PROCESO: suma un intento; con menos de {@value #MAX_INTENTOS} intentos vuelve a PENDIENTE, y al llegar al
 *       maximo queda en ERROR (el panel permite reintentar).</li>
 *   <li>R5. Cancelar solo si sigue PENDIENTE (o ERROR). Una ya publicada o en proceso no se cancela.</li>
 *   <li>R6. El codigo de referencia (J + id) identifica la publicacion; un mensaje del grupo solo cuenta si trae ese codigo.</li>
 *   <li>R7. Nace PENDIENTE con grupo y texto obligatorios; sin hora de salida sale de inmediato; el codigo y el texto final (con el
 *       codigo al inicio) se arman al guardarla por primera vez, porque el codigo depende del id.</li>
 *   <li>R8. Reintentar solo desde ERROR: vuelve a PENDIENTE con los intentos en cero.</li>
 * </ul>
 */
public class PublicacionWhatsapp {

    public static final int MAX_INTENTOS = 3;
    public static final String PREFIJO_CODIGO = "J";
    public static final int MAX_GRUPO = 200;
    /** Solo el cuerpo: la cabecera y la invitacion con el codigo se suman despues. */
    public static final int MAX_TEXTO = 1000;

    private Integer id;
    private final Integer varianteId;
    private final String grupo;
    private String texto;
    private String codigoRef;
    private final ModoVenta modoVenta;
    private final Long imagenId;
    private final LocalDateTime programadaPara;
    private EstadoPublicacion estado;
    private int intentos;
    private String ultimoError;
    private LocalDateTime fechaPublicacion;

    public PublicacionWhatsapp(Integer id, Integer varianteId, String grupo, String texto, String codigoRef, ModoVenta modoVenta,
                               Long imagenId, LocalDateTime programadaPara, EstadoPublicacion estado, int intentos,
                               String ultimoError, LocalDateTime fechaPublicacion) {
        this.id = id;
        this.varianteId = varianteId;
        this.grupo = grupo;
        this.texto = texto;
        this.codigoRef = codigoRef;
        this.modoVenta = modoVenta;
        this.imagenId = imagenId;
        this.programadaPara = programadaPara;
        this.estado = estado;
        this.intentos = intentos;
        this.ultimoError = ultimoError;
        this.fechaPublicacion = fechaPublicacion;
    }

    /** R7: una publicacion nueva, todavia sin id ni codigo. {@code cuerpo} es solo el contenido (sin la cabecera del codigo). */
    public static PublicacionWhatsapp nueva(Integer varianteId, String grupo, String cuerpo, ModoVenta modoVenta, Long imagenId,
                                            LocalDateTime programadaPara, LocalDateTime ahora) {
        if (varianteId == null) {
            throw PublicacionWhatsappException.invalida("Elige el artículo que se va a publicar");
        }
        if (grupo == null || grupo.isBlank()) {
            throw PublicacionWhatsappException.invalida("Escribe el nombre exacto del grupo de WhatsApp donde se publica");
        }
        if (grupo.trim().length() > MAX_GRUPO) {
            throw PublicacionWhatsappException.invalida("El nombre del grupo es demasiado largo (máximo " + MAX_GRUPO + " letras)");
        }
        if (cuerpo == null || cuerpo.isBlank()) {
            throw PublicacionWhatsappException.invalida("La publicación no puede ir sin texto");
        }
        if (cuerpo.trim().length() > MAX_TEXTO) {
            throw PublicacionWhatsappException.invalida("El texto es demasiado largo (máximo " + MAX_TEXTO + " letras)");
        }
        return new PublicacionWhatsapp(null, varianteId, grupo.trim(), cuerpo.trim(), null,
                modoVenta == null ? ModoVenta.PRIVADO : modoVenta, imagenId, programadaPara == null ? ahora : programadaPara,
                EstadoPublicacion.PENDIENTE, 0, null, null);
    }

    /** R7: al recibir su id (J42) la publicacion arma su codigo y su texto final, con el codigo al inicio. Una sola vez. */
    public void asignarIdentidad(int idGenerado) {
        if (codigoRef != null) {
            throw new IllegalStateException("La publicacion " + id + " ya tiene codigo");
        }
        this.id = idGenerado;
        this.codigoRef = codigoPara(idGenerado);
        this.texto = PlantillaDeTextoWhatsapp.armar(codigoRef, texto);
    }

    /** R8 */
    public void reintentar() {
        if (estado != EstadoPublicacion.ERROR) {
            throw PublicacionWhatsappException.estadoNoPermite("reintentar", estado.name());
        }
        estado = EstadoPublicacion.PENDIENTE;
        intentos = 0;
        ultimoError = null;
    }

    /** R6: el codigo que identifica una publicacion a partir de su id (J42). */
    public static String codigoPara(int id) {
        return PREFIJO_CODIGO + id;
    }

    /** R2 */
    public void tomarParaPublicar() {
        if (estado != EstadoPublicacion.PENDIENTE) {
            throw PublicacionWhatsappException.estadoNoPermite("recoger", estado.name());
        }
        estado = EstadoPublicacion.EN_PROCESO;
    }

    /** R3 */
    public void marcarPublicada(LocalDateTime ahora) {
        if (estado != EstadoPublicacion.EN_PROCESO) {
            throw PublicacionWhatsappException.estadoNoPermite("marcar como publicada", estado.name());
        }
        estado = EstadoPublicacion.PUBLICADA;
        fechaPublicacion = ahora;
        ultimoError = null;
    }

    /** R4 */
    public void registrarError(String detalle) {
        if (estado != EstadoPublicacion.EN_PROCESO) {
            throw PublicacionWhatsappException.estadoNoPermite("registrar un error en", estado.name());
        }
        intentos++;
        ultimoError = detalle == null || detalle.isBlank() ? "El bot no dio el motivo" : recortar(detalle);
        estado = intentos >= MAX_INTENTOS ? EstadoPublicacion.ERROR : EstadoPublicacion.PENDIENTE;
    }

    /** R5 */
    public void cancelar() {
        if (estado != EstadoPublicacion.PENDIENTE && estado != EstadoPublicacion.ERROR) {
            throw PublicacionWhatsappException.estadoNoPermite("cancelar", estado.name());
        }
        estado = EstadoPublicacion.CANCELADA;
    }

    private static String recortar(String detalle) {
        String limpio = detalle.trim();
        return limpio.length() > 1000 ? limpio.substring(0, 1000) : limpio;
    }

    public Integer id() { return id; }
    public Integer varianteId() { return varianteId; }
    public String grupo() { return grupo; }
    public String texto() { return texto; }
    public String codigoRef() { return codigoRef; }
    public ModoVenta modoVenta() { return modoVenta; }
    public Long imagenId() { return imagenId; }
    public LocalDateTime programadaPara() { return programadaPara; }
    public EstadoPublicacion estado() { return estado; }
    public int intentos() { return intentos; }
    public String ultimoError() { return ultimoError; }
    public LocalDateTime fechaPublicacion() { return fechaPublicacion; }
}
