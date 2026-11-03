package com.ventas.key.hexagonal.escanercodigo.infraestructura.entrada.rest.dto;

import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.CodigoEscaneado;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.EstadoDeSesion;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.LecturaDeCodigos;
import com.ventas.key.hexagonal.escanercodigo.dominio.modelo.SesionAbierta;

import java.time.Instant;
import java.util.List;

/** El JSON del escáner remoto. */
public final class EscanerResponse {

    private EscanerResponse() {
    }

    public record Sesion(String id, Instant venceEn) {
        public static Sesion de(SesionAbierta s) {
            return new Sesion(s.id(), s.venceEn());
        }
    }

    public record Codigo(long secuencia, String codigo) {
        static Codigo de(CodigoEscaneado c) {
            return new Codigo(c.secuencia(), c.codigo());
        }
    }

    public record Lectura(long ultimaSecuencia, boolean celularConectado, int epoca, List<Codigo> codigos) {
        public static Lectura de(LecturaDeCodigos l) {
            return new Lectura(l.ultimaSecuencia(), l.celularConectado(), l.epoca(), l.codigos().stream().map(Codigo::de).toList());
        }
    }

    public record Estado(boolean vigente, boolean celularConectado, long ultimaSecuencia, Instant venceEn, int epoca) {
        public static Estado de(EstadoDeSesion e) {
            return new Estado(e.vigente(), e.celularConectado(), e.ultimaSecuencia(), e.venceEn(), e.epoca());
        }
    }

    public record EnviarCodigo(String codigo) {
    }
}
