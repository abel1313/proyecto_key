package com.ventas.key.hexagonal.escanercodigo.infraestructura.config;

import com.ventas.key.hexagonal.escanercodigo.aplicacion.servicio.EscanerRemotoService;
import com.ventas.key.hexagonal.escanercodigo.dominio.puerto.entrada.EscanerRemotoCasoUso;
import com.ventas.key.hexagonal.escanercodigo.dominio.puerto.salida.SesionesEscanerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;

/** Arma el caso de uso del escáner remoto. El identificador son 16 bytes de {@link SecureRandom} (128 bits): no se puede adivinar. */
@Configuration
public class EscanerCodigoBeans {

    @Bean
    public EscanerRemotoCasoUso escanerRemotoCasoUso(SesionesEscanerPort sesiones, Clock reloj) {
        SecureRandom azar = new SecureRandom();
        return new EscanerRemotoService(sesiones, reloj, () -> {
            byte[] bytes = new byte[16];
            azar.nextBytes(bytes);
            return HexFormat.of().formatHex(bytes);
        });
    }
}
