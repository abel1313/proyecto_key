package com.ventas.key.hexagonal.whatsapp.infraestructura.config;

import com.ventas.key.hexagonal.whatsapp.aplicacion.servicio.AdministrarConversacionesService;
import com.ventas.key.hexagonal.whatsapp.aplicacion.servicio.EnvioConVentanaService;
import com.ventas.key.hexagonal.whatsapp.aplicacion.servicio.RecibirMensajeWhatsappService;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.AdministrarConversacionesWhatsappCasoUso;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.entrada.RecibirMensajeWhatsappCasoUso;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.CerebroWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ClientesWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.ControlDeAbusoWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.EnvioWhatsappPort;
import com.ventas.key.hexagonal.whatsapp.dominio.puerto.salida.MensajesWhatsappPort;
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
            @Value("${whatsapp.bot.pausa-minutos:30}") long pausaMinutos) {
        return new RecibirMensajeWhatsappService(clientes, mensajes, cerebro, abuso, envio, RELOJ,
                Duration.ofMinutes(pausaMinutos));
    }

    @Bean
    public AdministrarConversacionesWhatsappCasoUso administrarConversacionesWhatsappCasoUso(
            ClientesWhatsappPort clientes, MensajesWhatsappPort mensajes, EnvioConVentanaService envio,
            @Value("${whatsapp.bot.pausa-minutos:30}") long pausaMinutos) {
        return new AdministrarConversacionesService(clientes, mensajes, envio, RELOJ, Duration.ofMinutes(pausaMinutos));
    }
}
