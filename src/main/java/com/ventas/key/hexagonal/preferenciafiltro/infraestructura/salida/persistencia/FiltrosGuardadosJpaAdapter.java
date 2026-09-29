package com.ventas.key.hexagonal.preferenciafiltro.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.FiltrosGuardados;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo.Pantalla;
import com.ventas.key.hexagonal.preferenciafiltro.dominio.puerto.salida.FiltrosGuardadosPort;
import com.ventas.key.mis.productos.entity.PreferenciaFiltro;
import com.ventas.key.mis.productos.repository.IPreferenciaFiltroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers — la BD]
 */
@Component
@RequiredArgsConstructor
public class FiltrosGuardadosJpaAdapter implements FiltrosGuardadosPort {

    private final IPreferenciaFiltroRepository repository;

    @Override
    public Optional<FiltrosGuardados> buscar(int usuarioId, Pantalla pantalla) {
        return repository.findByUsuarioIdAndPantalla(usuarioId, pantalla.clave()).map(FiltrosGuardadosJpaAdapter::aDominio);
    }

    @Override
    public FiltrosGuardados guardar(FiltrosGuardados filtros) {
        PreferenciaFiltro fila = repository
                .findByUsuarioIdAndPantalla(filtros.usuarioId(), filtros.pantalla().clave())
                .orElseGet(PreferenciaFiltro::new);
        fila.setUsuarioId(filtros.usuarioId());
        fila.setPantalla(filtros.pantalla().clave());
        fila.setFiltros(filtros.filtrosJson());
        fila.setActualizado(filtros.actualizado());
        return aDominio(repository.save(fila));
    }

    @Override
    public void borrar(int usuarioId, Pantalla pantalla) {
        repository.deleteByUsuarioIdAndPantalla(usuarioId, pantalla.clave());
    }

    private static FiltrosGuardados aDominio(PreferenciaFiltro fila) {
        return new FiltrosGuardados(fila.getUsuarioId(), Pantalla.deClave(fila.getPantalla()),
                fila.getFiltros(), fila.getActualizado());
    }
}
