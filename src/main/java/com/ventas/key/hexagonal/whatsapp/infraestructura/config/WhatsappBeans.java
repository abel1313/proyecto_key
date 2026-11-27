package com.ventas.key.hexagonal.whatsapp.infraestructura.config;

import com.ventas.key.hexagonal.whatsapp.aplicacion.servicio.AdministrarConversacionesService;
import com.ventas.key.hexagonal.whatsapp.aplicacion.servicio.EnvioConVentanaService;
import com.ventas.key.hexagonal.whatsapp.aplicacion.servicio.PedidoPorWhatsappService;
import com.ventas.key.hexagonal.whatsapp.aplicacion.servicio.RecibirMensajeWhatsappService;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.AdministrarConversacionesWhatsappCasoUso;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.PedidoPorWhatsappCasoUso;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.RecibirMensajeWhatsappCasoUso;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.AvisoAlAdministradorPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.BorradoresDePedidoPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.CatalogoParaPedidoPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.CerebroWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ClientesWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ControlDeAbusoWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.DatosConocidosPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.EnvioWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.HorarioDeAtencionPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.LugaresDeEntregaPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.MensajesWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.NotificacionesDePedidoPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.PedidosApartadoPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executor;

/** Arma los casos de uso de WhatsApp con sus adaptadores. Los servicios son Java puro. */
@Configuration
public class WhatsappBeans {

    // Su propio reloj, en UTC: la ventana de 24 h se compara con la hora de Meta (Instant), sin zonas.
    private static final Clock RELOJ = Clock.systemUTC();

    /**
     * Procesa el webhook fuera del hilo de la petición. Pool chico y cola corta: si se llena, el
     * webhook responde 503 y Meta reintenta (ver {@code WhatsappProcesador}); nunca se pierde un mensaje.
     */
    @Bean("whatsappExecutor")
    public Executor whatsappExecutor() {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(2);
        ejecutor.setMaxPoolSize(4);
        ejecutor.setQueueCapacity(100);
        ejecutor.setThreadNamePrefix("whatsapp-");
        // Política por defecto (rechazar): el controlador lo convierte en 503.
        ejecutor.initialize();
        return ejecutor;
    }

    @Bean
    public EnvioConVentanaService envioConVentanaService(
            ClientesWhatsappPort clientes, MensajesWhatsappPort mensajes, EnvioWhatsappPort envio,
            @Value("${whatsapp.cloud.metodo-pago-activo:false}") boolean metodoDePagoActivo) {
        return new EnvioConVentanaService(clientes, mensajes, envio, RELOJ, metodoDePagoActivo);
    }

    @Bean
    public RecibirMensajeWhatsappCasoUso recibirMensajeWhatsappCasoUso(
            ClientesWhatsappPort clientes, MensajesWhatsappPort mensajes, CerebroWhatsappPort cerebro,
            ControlDeAbusoWhatsappPort abuso, EnvioConVentanaService envio,
            HorarioDeAtencionPort horario, AvisoAlAdministradorPort aviso, PedidoPorWhatsappCasoUso pedidos,
            @Value("${whatsapp.bot.pausa-minutos:30}") long pausaMinutos,
            @Value("${whatsapp.bot.pedidos-habilitado:false}") boolean pedidosHabilitado) {
        // R16: con el interruptor apagado (lo normal hasta probarlo en QA) el bot no toma pedidos y ni lo menciona.
        return new RecibirMensajeWhatsappService(clientes, mensajes, cerebro, abuso, envio, RELOJ,
                Duration.ofMinutes(pausaMinutos), horario, aviso, pedidosHabilitado ? pedidos : null);
    }

    /** El resumen espera el «sí» del cliente {@code whatsapp.bot.pedido-vigencia-minutos} minutos (30 por defecto). */
    @Bean
    public PedidoPorWhatsappCasoUso pedidoPorWhatsappCasoUso(
            CatalogoParaPedidoPort catalogo, LugaresDeEntregaPort lugares, BorradoresDePedidoPort borradores,
            PedidosApartadoPort pedidos, DatosConocidosPort conocidos, NotificacionesDePedidoPort notificaciones,
            HorarioDeAtencionPort horario,
            @Value("${whatsapp.bot.pedido-vigencia-minutos:30}") long vigenciaMinutos,
            @Value("${whatsapp.bot.max-apartados-abiertos:0}") int maxApartadosAbiertos) {
        // max-apartados-abiertos: 0 = sin tope (el dueño aún no decide el valor; se recomienda 3).
        return new PedidoPorWhatsappService(catalogo, lugares, borradores, pedidos, conocidos, notificaciones, RELOJ,
                Duration.ofMinutes(vigenciaMinutos), horario, Math.max(0, maxApartadosAbiertos));
    }

    @Bean
    public AdministrarConversacionesWhatsappCasoUso administrarConversacionesWhatsappCasoUso(
            ClientesWhatsappPort clientes, MensajesWhatsappPort mensajes, EnvioConVentanaService envio,
            BorradoresDePedidoPort borradores,
            @Value("${whatsapp.bot.pausa-minutos:30}") long pausaMinutos) {
        return new AdministrarConversacionesService(clientes, mensajes, envio, RELOJ, Duration.ofMinutes(pausaMinutos), borradores);
    }
}
