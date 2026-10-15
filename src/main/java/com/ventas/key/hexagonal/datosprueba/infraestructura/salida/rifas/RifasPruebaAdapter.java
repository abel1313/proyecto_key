package com.ventas.key.hexagonal.datosprueba.infraestructura.salida.rifas;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.NombresDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.RifasPruebaPort;
import com.ventas.key.mis.productos.entity.ConfigurarRifa;
import com.ventas.key.mis.productos.entity.ConfigurarRifaVariante;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.models.ConfigurarRifaVarianteRequest;
import com.ventas.key.mis.productos.repository.IConfigurarRifaRepository;
import com.ventas.key.mis.productos.repository.IConfigurarRifaVarianteRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.service.ConfiguracionRifaServiceImpl;
import com.ventas.key.mis.productos.service.ConfigurarRifaVarianteService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>R23: la rifa de una prueba sale de los mismos servicios que Rifas → Agregar rifa: la configuración con
 * {@code ConfiguracionRifaServiceImpl.save} y el premio con {@code ConfigurarRifaVarianteService.agregar}, que
 * aparta 1 pieza del artículo. Siempre "es prueba". Al quitarla se hace lo mismo que cuando una rifa de prueba
 * vence ({@code desactivarVencidas}): se desactiva y la pieza apartada regresa al artículo. Nunca se borra.
 */
@Component
@RequiredArgsConstructor
public class RifasPruebaAdapter implements RifasPruebaPort {

    private final ConfiguracionRifaServiceImpl rifas;
    private final ConfigurarRifaVarianteService premios;
    private final IConfigurarRifaRepository rifaRepository;
    private final IConfigurarRifaVarianteRepository premioRepository;
    private final IVarianteRepository articulos;
    private final TransactionTemplate transaccion;
    private final JdbcTemplate jdbc;

    @Override
    public int rifaConPremio(String nombre, int articuloId, String palabraClave, LocalDateTime plazoDeRegistro) {
        ConfigurarRifa rifa = new ConfigurarRifa();
        rifa.setDescripcion(nombre);
        rifa.setTipo(ConfigurarRifa.TipoRifa.DIARIA);
        rifa.setEsPrueba(true);
        rifa.setActiva(true);
        rifa.setPublica(false);
        rifa.setFechaHoraLimite(plazoDeRegistro);
        ConfigurarRifa guardada = rifas.save(rifa);

        ConfigurarRifaVarianteRequest premio = new ConfigurarRifaVarianteRequest();
        premio.setConfigurarRifaId(guardada.getId());
        premio.setVarianteId(articuloId);
        premio.setPalabraClave(palabraClave);
        premio.setGiroGanador(1);
        premio.setOrden(1);
        premios.agregar(premio);
        return guardada.getId();
    }

    @Override
    public int quitarRifasDeLaPrueba(String prueba) {
        String prefijo = NombresDePrueba.prefijoModelos(prueba);
        Integer cuantas = transaccion.execute(estado -> {
            List<Integer> ids = jdbc.queryForList("SELECT id FROM configurar_rifa "
                    + "WHERE es_prueba = 1 AND activa = 1 AND descripcion LIKE ?", Integer.class, prefijo + "%");
            List<ConfigurarRifa> deLaPrueba = rifaRepository.findAllById(ids);
            for (ConfigurarRifa r : deLaPrueba) {
                for (ConfigurarRifaVariante crv : premioRepository.findByConfigurarRifaIdOrderByOrdenAsc(r.getId())) {
                    Variantes articulo = crv.getVariante();
                    articulo.setStock(articulo.getStock() + crv.getStockReservado());
                    articulos.save(articulo);
                }
                r.setActiva(false);
                r.setPublica(false);
            }
            rifaRepository.saveAll(deLaPrueba);
            return deLaPrueba.size();
        });
        return cuantas == null ? 0 : cuantas;
    }
}
