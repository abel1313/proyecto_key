package com.ventas.key.hexagonal.pedidoarticulo.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pedidoarticulo.dominio.puerto.salida.RepreciarPorCantidadPort;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.LineaDePrecio;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.LineaPorCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.PrecioDeGrupo;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.PrecioRepreciado;
import com.ventas.key.hexagonal.preciocantidad.dominio.modelo.ReglasPrecioCantidad;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.entrada.CalcularPreciosPorCantidadCasoUso;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.salida.GruposDePrecioPort;
import com.ventas.key.hexagonal.preciocantidad.dominio.puerto.salida.PreciosBaseDeLineaPort;
import com.ventas.key.hexagonal.preciocantidad.infraestructura.salida.persistencia.TablasDisponibles;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.repository.IDetallePedidoRepository;
import com.ventas.key.mis.productos.repository.IPedidoRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter] Vuelve a calcular el precio por cantidad de las líneas de un pedido guardado, en la
 * misma transacción de la edición. Trabaja sobre las MISMAS entidades que ya trae el pedido en esta petición (la lista de líneas en memoria
 * se mantiene a mano; ver {@code PedidoArticuloJpaAdapter}), así el total que se relee después ya las ve cambiadas.
 */
@Component
@Slf4j
public class RepreciarPorCantidadAdapter implements RepreciarPorCantidadPort {

    private final IPedidoRepository pedidoRepository;
    private final IDetallePedidoRepository detalleRepository;
    private final IVarianteRepository varianteRepository;
    private final CalcularPreciosPorCantidadCasoUso calculo;
    private final PreciosBaseDeLineaPort preciosBase;
    private final GruposDePrecioPort grupos;
    private final TablasDisponibles tablas;

    public RepreciarPorCantidadAdapter(IPedidoRepository pedidoRepository, IDetallePedidoRepository detalleRepository,
                                       IVarianteRepository varianteRepository, CalcularPreciosPorCantidadCasoUso calculo,
                                       PreciosBaseDeLineaPort preciosBase, GruposDePrecioPort grupos, TablasDisponibles tablas) {
        this.pedidoRepository = pedidoRepository;
        this.detalleRepository = detalleRepository;
        this.varianteRepository = varianteRepository;
        this.calculo = calculo;
        this.preciosBase = preciosBase;
        this.grupos = grupos;
        this.tablas = tablas;
    }

    @Override
    @Transactional
    public void repreciar(Integer pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido == null || pedido.getDetalles() == null || pedido.getDetalles().isEmpty()) {
            return;
        }
        List<DetallePedido> lineas = new ArrayList<>(pedido.getDetalles());

        // Las piezas del grupo se cuentan con TODO lo que lleva el pedido ahora, sin las lineas de un combo.
        List<LineaPorCantidad> piezas = new ArrayList<>();
        for (DetallePedido d : lineas) {
            if (d.getPromocion() == null && d.getCantidad() != null && d.getCantidad() > 0) {
                piezas.add(new LineaPorCantidad(productoId(d), d.getCantidad()));
            }
        }
        Map<Integer, PrecioDeGrupo> deGrupo = calculo.calcular(piezas);
        Map<Integer, BigDecimal> recordados = preciosBase.leer(lineas.stream().map(DetallePedido::getId).toList());
        // Una linea con precio de campana no la toca el grupo (al pasar a Ir pagando es la campana la que la regresa): se trata como un combo.
        Map<Integer, BigDecimal> deCampana = preciosBase.leer(lineas.stream().map(DetallePedido::getId).toList(),
                com.ventas.key.hexagonal.preciocantidad.dominio.modelo.OrigenDePrecio.CAMPANA);
        if (deGrupo.isEmpty() && recordados.isEmpty()) {
            return;   // sin grupos y sin lineas cobradas con precio de grupo: nada que recalcular
        }

        List<LineaDePrecio> entrada = new ArrayList<>();
        for (DetallePedido d : lineas) {
            entrada.add(new LineaDePrecio(d.getId(), productoId(d), dinero(d.getPrecioUnitario()), recordados.get(d.getId()), d.getPromocion() != null || deCampana.containsKey(d.getId())));
        }
        Map<Integer, DetallePedido> porId = new HashMap<>();
        lineas.forEach(d -> porId.put(d.getId(), d));

        Map<Integer, BigDecimal> aGuardar = new LinkedHashMap<>();
        List<Integer> aBorrar = new ArrayList<>();
        Set<Integer> variantesCambiadas = new TreeSet<>();
        for (PrecioRepreciado r : ReglasPrecioCantidad.repreciar(entrada, deGrupo)) {
            DetallePedido d = porId.get(r.detalleId());
            if (Math.abs(dinero(d.getPrecioUnitario()).subtract(r.precioNuevo()).doubleValue()) > 0.004) {
                log.info("Pedido {}: la linea {} paso de ${} a ${} por el precio por cantidad", pedidoId, d.getId(), d.getPrecioUnitario(), r.precioNuevo());
                d.setPrecioUnitario(r.precioNuevo().doubleValue());
                d.setSubTotal(r.precioNuevo().doubleValue() * d.getCantidad());
                detalleRepository.save(d);
                if (d.getVariante() != null) {
                    variantesCambiadas.add(d.getVariante().getId());
                }
            }
            if (r.precioSinGrupoNuevo() != null) {
                aGuardar.put(d.getId(), r.precioSinGrupoNuevo());
            } else if (recordados.containsKey(d.getId())) {
                aBorrar.add(d.getId());
            }
        }
        preciosBase.guardar(aGuardar);
        preciosBase.borrar(aBorrar);
        fusionarLineasIguales(pedido, variantesCambiadas);
    }

    /**
     * Si tras recalcular quedaron dos lineas normales del MISMO artículo al MISMO precio (una ya estaba con precio de grupo y la agregada
     * entró a precio normal), se juntan en una: R6, "convertirla dejaba dos líneas de lo mismo". Solo se miran los artículos cuyo precio cambió.
     */
    private void fusionarLineasIguales(Pedido pedido, Set<Integer> variantes) {
        for (Integer varianteId : variantes) {
            List<DetallePedido> mismas = pedido.getDetalles().stream()
                    .filter(d -> d.getPromocion() == null && d.getVariante() != null && varianteId.equals(d.getVariante().getId()))
                    .toList();
            if (mismas.size() < 2) {
                continue;
            }
            DetallePedido primera = mismas.get(0);
            for (DetallePedido otra : mismas.subList(1, mismas.size())) {
                if (Math.abs(otra.getPrecioUnitario() - primera.getPrecioUnitario()) > 0.01) {
                    continue;
                }
                primera.setCantidad(primera.getCantidad() + otra.getCantidad());
                primera.setSubTotal(primera.getPrecioUnitario() * primera.getCantidad());
                detalleRepository.save(primera);
                pedido.getDetalles().remove(otra);
                preciosBase.borrar(List.of(otra.getId()));
                detalleRepository.delete(otra);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esPrecioDeGrupo(Integer varianteId, double precio) {
        if (!tablas.existe("grupo_precio_cantidad_producto")) {
            return false;
        }
        var variante = varianteRepository.findById(varianteId).orElse(null);
        if (variante == null || variante.getProducto() == null) {
            return false;
        }
        int productoId = variante.getProducto().getId();
        return grupos.grupoDelProducto(productoId)
                .filter(g -> g.vigenteEn(LocalDate.now()))
                .flatMap(g -> g.miembro(productoId))
                .map(m -> Math.abs(m.precioPromocion().doubleValue() - precio) <= 0.01)
                .orElse(false);
    }

    @Override
    @Transactional
    public void olvidarPrecioDeAntes(Integer detalleId) {
        preciosBase.borrar(List.of(detalleId));
    }

    private static Integer productoId(DetallePedido d) {
        if (d.getProducto() != null) {
            return d.getProducto().getId();
        }
        return d.getVariante() != null && d.getVariante().getProducto() != null ? d.getVariante().getProducto().getId() : 0;
    }

    private static BigDecimal dinero(Double valor) {
        return BigDecimal.valueOf(valor == null ? 0.0 : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
