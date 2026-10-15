package com.ventas.key.hexagonal.codigopostal.infraestructura.salida.persistencia;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventas.key.hexagonal.codigopostal.dominio.modelo.CodigoPostalInfo;
import com.ventas.key.hexagonal.codigopostal.dominio.puerto.salida.CodigoPostalGuardadoPort;
import com.ventas.key.mis.productos.entity.CodigoPostalGuardado;
import com.ventas.key.mis.productos.repository.ICodigoPostalGuardadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers — la BD]
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodigoPostalJpaAdapter implements CodigoPostalGuardadoPort {

    private static final TypeReference<List<String>> LISTA_DE_TEXTOS = new TypeReference<>() { };

    private final ICodigoPostalGuardadoRepository repository;
    private final ObjectMapper json;

    @Override
    public Optional<CodigoPostalInfo> buscar(String codigo) {
        return repository.findByCodigo(codigo).map(this::aDominio);
    }

    @Override
    public void guardar(CodigoPostalInfo info) {
        CodigoPostalGuardado fila = repository.findByCodigo(info.codigo()).orElseGet(CodigoPostalGuardado::new);
        fila.setCodigo(info.codigo());
        fila.setMunicipio(info.municipio());
        fila.setEstado(info.estado());
        fila.setColonias(coloniasATexto(info.colonias()));
        fila.setConsultado(info.consultado());
        try {
            repository.save(fila);
        } catch (DataIntegrityViolationException e) {
            // R6: otra petición guardó el mismo código al mismo tiempo; con una fila basta.
            log.debug("El código postal {} ya lo había guardado otra petición", info.codigo());
        }
    }

    private CodigoPostalInfo aDominio(CodigoPostalGuardado fila) {
        return new CodigoPostalInfo(fila.getCodigo(), fila.getMunicipio(), fila.getEstado(),
                textoAColonias(fila.getColonias()), fila.getConsultado());
    }

    private String coloniasATexto(List<String> colonias) {
        try {
            return json.writeValueAsString(colonias);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron convertir las colonias a texto", e);
        }
    }

    private List<String> textoAColonias(String texto) {
        try {
            return texto == null || texto.isBlank() ? List.of() : json.readValue(texto, LISTA_DE_TEXTOS);
        } catch (JsonProcessingException e) {
            log.warn("Colonias ilegibles en la tabla codigo_postal: {}", e.getMessage());
            return List.of();
        }
    }
}
