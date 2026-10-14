package com.ventas.key.hexagonal.busquedapedido.infraestructura.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.FiltroPedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.puerto.salida.HoyPort;
import com.ventas.key.hexagonal.busquedapedido.infraestructura.dto.PedidosEncontradosResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.Supplier;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>R18: la respuesta de {@code GET /v1/pedidos/buscar} guardada en Redis por combinacion de filtros.
 * Prender y quitar el mismo filtro, o regresar a la vista de inicio, ya no vuelve a la base mientras no
 * haya cambiado ningun pedido. La llave lleva la {@link VersionDeListasDePedidos version} (cualquier
 * cambio la sube y todas las listas viejas dejan de usarse), el dia en Mexico (los filtros de entrega
 * dependen de "hoy") y todos los filtros. Vigencia corta como red de seguridad.
 *
 * <p>Se guarda el mismo JSON que se le manda a la pantalla: lo que sale del cache es identico.
 */
@Slf4j
@Component
public class ListasDePedidosEnCache {

    private static final String PREFIJO = "pedidos:lista:";

    private final VersionDeListasDePedidos version;
    private final ObjectProvider<StringRedisTemplate> redis;
    private final ObjectMapper objectMapper;
    private final HoyPort reloj;
    private final Duration vigencia;

    public ListasDePedidosEnCache(VersionDeListasDePedidos version, ObjectProvider<StringRedisTemplate> redis,
                                  ObjectMapper objectMapper, HoyPort reloj,
                                  @Value("${pedidos.lista.cache.segundos:120}") long segundos) {
        this.version = version;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.reloj = reloj;
        this.vigencia = Duration.ofSeconds(segundos);
    }

    public PedidosEncontradosResponse obtener(FiltroPedidos filtro, Supplier<PedidosEncontradosResponse> calcular) {
        String v = version.actual();
        StringRedisTemplate r = v == null ? null : redis.getIfAvailable();
        if (r == null) {
            return calcular.get();
        }
        String clave = PREFIJO + v + ":" + huella(filtro.toString() + "|" + reloj.hoy());
        try {
            String guardado = r.opsForValue().get(clave);
            if (guardado != null) {
                return objectMapper.readValue(guardado, PedidosEncontradosResponse.class);
            }
        } catch (Exception e) {
            log.warn("Cache de Mis pedidos: no se pudo leer {} ({}); se calcula", clave, e.getMessage());
        }
        PedidosEncontradosResponse respuesta = calcular.get();
        try {
            r.opsForValue().set(clave, objectMapper.writeValueAsString(respuesta), vigencia);
        } catch (Exception e) {
            log.warn("Cache de Mis pedidos: no se pudo guardar {} ({})", clave, e.getMessage());
        }
        return respuesta;
    }

    private static String huella(String texto) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
