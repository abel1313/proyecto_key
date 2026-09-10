package com.ventas.key.mis.productos.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ventas.key.mis.productos.models.ImagenUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Round-trip de Redis con el mismo ObjectMapper de {@link CacheTtlConfig}.
 *
 * <p>Existe por un bug que se comio dos veces: el valor raiz de un @Cacheable que sea una lista
 * inmutable (List.of, .toList(), List.copyOf) se guarda en Redis SIN el type id -- son clases
 * final y activateDefaultTyping esta en NON_FINAL -- y al leerlo de vuelta Jackson truena. El
 * sintoma es traicionero: la 1a llamada responde bien (cache fria, no pasa por el serializer) y
 * la 2a responde vacio. Tumbo el login el 2026-09-08 y el carrusel de premios el 2026-09-10.
 */
class RedisSerializacionImagenTest {

    private GenericJackson2JsonRedisSerializer serializerDelCache() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    private Object roundTrip(Object valor) {
        GenericJackson2JsonRedisSerializer s = serializerDelCache();
        return s.deserialize(s.serialize(valor));
    }

    private ImagenUpdateDto imagenComoLaArmaElServicio() {
        ImagenUpdateDto d = new ImagenUpdateDto(1116324125020157459L, (byte[]) null, "image/jpeg", "foto.jpeg");
        d.setUrlImagen("https://micro/mis-productos/v1/imagenes/file/1116324125020157459");
        d.setPrincipal(null);
        return d;
    }

    @Test
    void loQueDevuelveGetImagenesPorVarianteV2SobreviveElCache() {
        List<ImagenUpdateDto> devuelto = new ArrayList<>(List.of(imagenComoLaArmaElServicio()));

        Object leido = roundTrip(devuelto);

        List<?> lista = assertInstanceOf(List.class, leido);
        assertEquals(1, lista.size(), "el carrusel se queda vacio si la lista no vuelve entera");
        ImagenUpdateDto img = assertInstanceOf(ImagenUpdateDto.class, lista.get(0));
        assertEquals(1116324125020157459L, img.getId());
        assertEquals("https://micro/mis-productos/v1/imagenes/file/1116324125020157459", img.getUrlImagen());
    }

    @Test
    void laListaVaciaTambienSobrevive() {
        assertEquals(List.of(), roundTrip(new ArrayList<ImagenUpdateDto>()));
    }

    @Test
    void listaInmutableEnLaRaizRompeElCache() {
        assertThrows(SerializationException.class,
                () -> roundTrip(List.of(imagenComoLaArmaElServicio())),
                "si esto deja de tronar, revisar CacheTtlConfig: puede que ya se pueda usar .toList()");
    }

    @Test
    void optionalEnLaRaizRompeElCache() {
        // Por esto LogoService.obtenerActivo() y ProductosServiceImpl.getResumen() ya no llevan
        // @Cacheable: Jackson guarda el Optional como {"empty":true,"present":false} -- sin type
        // id y sin el contenido -- y truena al leerlo. Si esto deja de tronar se pueden recachear.
        assertThrows(SerializationException.class, () -> roundTrip(Optional.empty()));
    }
}
