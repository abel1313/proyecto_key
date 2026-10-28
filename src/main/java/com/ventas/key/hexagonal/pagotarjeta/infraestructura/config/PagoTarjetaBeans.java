package com.ventas.key.hexagonal.pagotarjeta.infraestructura.config;

import com.ventas.key.hexagonal.pagotarjeta.aplicacion.servicio.CobroConTarjetaService;
import com.ventas.key.hexagonal.pagotarjeta.aplicacion.servicio.ConfigurarAceptaMsiService;
import com.ventas.key.hexagonal.pagotarjeta.aplicacion.servicio.DevolverCobroService;
import com.ventas.key.hexagonal.pagotarjeta.aplicacion.servicio.OfrecerPlanesService;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.AceptaMsiPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.ActualizarPedidoPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.DevolucionesPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.IntentosDePagoPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PasarelaDePagoPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PedidoACobrarPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.PlanesMsiPort;
import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.TransaccionPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Arma los casos de uso del cobro con tarjeta con sus adaptadores.
 *
 * <p>[Hexagonal: composicion] Los servicios son clases de Java puro (sin Spring) y aqui se conectan.
 * TODO el modulo (servicios, controller, trabajos programados) existe solo con
 * {@code pagos.tarjeta.habilitado=true}: sin esa propiedad no se crea nada, asi que llevar el codigo a un
 * ambiente donde todavia no esta la migracion o la pasarela (produccion) no cambia nada ni puede tronar el arranque. Las revisiones que hace PedidoServiceImpl
 * (cancelar o cambiar de forma de cobro un pedido con tarjeta) tambien dependen de esta propiedad, para no consultar una tabla
 * que todavia no existe.
 */
@Configuration
@ConditionalOnProperty(name = "pagos.tarjeta.habilitado", havingValue = "true")
public class PagoTarjetaBeans {

    @Bean
    public CobroConTarjetaService cobroConTarjetaService(PedidoACobrarPort pedidos, IntentosDePagoPort intentos,
                                                         PlanesMsiPort planes, PasarelaDePagoPort pasarela,
                                                         ActualizarPedidoPort actualizarPedido,
                                                         TransaccionPort transaccion, Clock reloj) {
        return new CobroConTarjetaService(pedidos, intentos, planes, pasarela, actualizarPedido, transaccion, reloj);
    }

    @Bean
    public DevolverCobroService devolverCobroService(IntentosDePagoPort intentos, DevolucionesPort devoluciones,
                                                     PasarelaDePagoPort pasarela, ActualizarPedidoPort actualizarPedido,
                                                     TransaccionPort transaccion, Clock reloj) {
        return new DevolverCobroService(intentos, devoluciones, pasarela, actualizarPedido, transaccion, reloj);
    }

    @Bean
    public OfrecerPlanesService ofrecerPlanesService(PedidoACobrarPort pedidos, PlanesMsiPort planes) {
        return new OfrecerPlanesService(pedidos, planes);
    }

    @Bean
    public ConfigurarAceptaMsiService configurarAceptaMsiService(AceptaMsiPort casillas) {
        return new ConfigurarAceptaMsiService(casillas);
    }
}
