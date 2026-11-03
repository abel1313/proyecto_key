package com.ventas.key.hexagonal.escanercodigo.infraestructura.config;

import com.ventas.key.hexagonal.escanercodigo.aplicacion.servicio.EscanerRemotoService;
import com.ventas.key.hexagonal.escanercodigo.dominio.puerto.entrada.EscanerRemotoCasoUso;
import com.ventas.key.hexagonal.escanercodigo.dominio.puerto.salida.SesionesEscanerPort;
import com.ventas.key.hexagonal.escanercodigo.infraestructura.salida.memoria.SesionesEscanerEnMemoria;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;

/**
 * Arma el escáner remoto. El módulo usa SU PROPIO reloj ({@code Clock.systemUTC()}, sin publicarlo como bean): no depende de un {@code Clock} del resto de
 * la aplicación (producción no lo tiene: el de {@code PagoTarjetaConfig} solo existe donde está el módulo de tarjeta) y tampoco crea uno que choque con el de
 * ese módulo. El identificador son 16 bytes de {@link SecureRandom} (128 bits): no se puede adivinar.
 */
@Configuration
public class EscanerCodigoBeans {

    private final Clock reloj = Clock.systemUTC();

    @Bean
    public SesionesEscanerPort sesionesEscanerPort() {
        return new SesionesEscanerEnMemoria(reloj);
    }

    @Bean
    public EscanerRemotoCasoUso escanerRemotoCasoUso(SesionesEscanerPort sesiones) {
        SecureRandom azar = new SecureRandom();
        return new EscanerRemotoService(sesiones, reloj, () -> {
            byte[] bytes = new byte[16];
            azar.nextBytes(bytes);
            return HexFormat.of().formatHex(bytes);
        });
    }
}
