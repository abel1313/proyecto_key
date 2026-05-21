package com.ventas.key.mis.productos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class CacheTtlConfig {



    @Value("${redis.dns}")
    private String dnsRedis;
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RedisSerializationContext.SerializationPair<Object> jsonSerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(mapper));

        // Configuración por defecto (ejemplo: 10 minutos)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(jsonSerializer)
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();

        // Configuración específica por cache
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        // Productos
        cacheConfigs.put("detalleImagen", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("detalle", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("buscarImagenIdCache", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("obtenerProductosCache", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("buscarNombreOrCodigoBarrasCache", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put("findByIdCache", defaultConfig.entryTtl(Duration.ofHours(6)));
        // Variantes
        cacheConfigs.put("variantesProductoCache", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("variantesNombreCache", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put("variantesCodigoBarrasCache", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put("variantesImagenesCache", defaultConfig.entryTtl(Duration.ofHours(1)));
        // Clientes
        cacheConfigs.put("clienteCache", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        // Catálogo de pagos (datos estáticos, TTL más largo)
        cacheConfigs.put("tiposPagoCache", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigs.put("tarifasTerminalCache", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigs.put("ivaCache", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigs.put("opcionesPagoCache", defaultConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigs.put("opcionesPorTipoCache", defaultConfig.entryTtl(Duration.ofHours(6)));
        // Presentación (imágenes de login/registro)
        cacheConfigs.put("presentacion-imagenes", defaultConfig.entryTtl(Duration.ofHours(6)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }


    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        log.info("se agregi redis {}",dnsRedis);
        return new LettuceConnectionFactory(dnsRedis, 6379);
    }

}
