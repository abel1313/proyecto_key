package com.ventas.key.mis.productos.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ventas.key.mis.productos.entity.DetalleVenta;
import com.ventas.key.mis.productos.entity.DetallePago;
import com.ventas.key.mis.productos.entity.DetallePagoId;
import com.ventas.key.mis.productos.entity.MesesIntereses;
import com.ventas.key.mis.productos.entity.PagosYMeses;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.entity.Venta;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.TotalDetalle;
import com.ventas.key.mis.productos.models.VentaDirectaRequest;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IDetallePagoRepository;
import com.ventas.key.mis.productos.repository.IDetalleVentaRepository;
import com.ventas.key.mis.productos.repository.IPagosYMesesRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import com.ventas.key.mis.productos.repository.IVentaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class VentaServiceImpl extends CrudAbstractServiceImpl<Venta, List<Venta>, Optional<Venta>, Integer, PginaDto<List<Venta>>> {

    @PersistenceContext
    private EntityManager entityManager;

    private final IVentaRepository iVentaRepository;
    private final IProductosRepository iRepository;
    private final IDetalleVentaRepository iDetalleVentaRepository;
    private final IUsuarioRepository iUsuarioRepository;
    private final IPagosYMesesRepository iPagosYMesesRepository;
    private final IDetallePagoRepository iDetallePagoRepository;
    private final IVarianteRepository iVarianteRepository;
    private final ErrorGenerico errorGenerico;

    public VentaServiceImpl(
            final IVentaRepository iVentaRepository,
            final IProductosRepository iRepository,
            final IDetalleVentaRepository iDetalleVentaRepository,
            final IUsuarioRepository iUsuarioRepository,
            final IPagosYMesesRepository iPagosYMesesRepository,
            final IDetallePagoRepository iDetallePagoRepository,
            final IVarianteRepository iVarianteRepository,
            final ErrorGenerico errorGenerico) {
        super(iVentaRepository, errorGenerico);
        this.iVentaRepository = iVentaRepository;
        this.iRepository = iRepository;
        this.iDetalleVentaRepository = iDetalleVentaRepository;
        this.iUsuarioRepository = iUsuarioRepository;
        this.iPagosYMesesRepository = iPagosYMesesRepository;
        this.iDetallePagoRepository = iDetallePagoRepository;
        this.iVarianteRepository = iVarianteRepository;
        this.errorGenerico = errorGenerico;
    }

    @Transactional
    public Venta saveVentaDetalle(VentaDirectaRequest request) throws ExceptionErrorInesperado, ExceptionDataNotFound {

        Usuario usuario = iUsuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));

        PagosYMeses pagosYMeses = iPagosYMesesRepository.findById(request.getPagosYMesesId())
                .orElseThrow(() -> new ExceptionErrorInesperado("Opción de pago no válida"));

        MesesIntereses mesesIntereses = pagosYMeses.getMesesIntereses();

        double tasaTarifa = mesesIntereses.getTarifaTerminal() != null
                ? mesesIntereses.getTarifaTerminal().getTarifa() / 100.0 : 0.0;
        double tasaIva = mesesIntereses.getIvaTerminal() != null
                ? mesesIntereses.getIvaTerminal().getIva() / 100.0 : 0.0;

        DetallePago detallePago = null;
        if (mesesIntereses.getTarifaTerminal() != null && mesesIntereses.getIvaTerminal() != null) {
            DetallePagoId detallePagoId = new DetallePagoId(
                    pagosYMeses.getTipoPago().getId(),
                    mesesIntereses.getTarifaTerminal().getId(),
                    mesesIntereses.getIvaTerminal().getId()
            );
            detallePago = iDetallePagoRepository.findById(detallePagoId).orElse(null);
        }

        Venta venta = new Venta();
        venta.setEstadoVenta("Entregado");
        venta.setFechaVenta(LocalDateTime.now());
        venta.setUsuario(usuario);
        venta.setPagosYMeses(pagosYMeses);
        venta.setDetallePago(detallePago);

        List<DetalleVenta> detalles = new ArrayList<>();
        for (var item : request.getDetalles()) {
            Producto prod = iRepository.findByCodigoBarras_CodigoBarrasAndNombre(item.getCodigoBarras(), item.getNombre())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + item.getNombre()));

            if (prod.getStock() < item.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + prod.getNombre()
                        + ". Disponible: " + prod.getStock() + ", solicitado: " + item.getCantidad());
            }
            prod.setStock(prod.getStock() - item.getCantidad());
            iRepository.save(prod);

            if (item.getVarianteId() != null) {
                Variantes variante = iVarianteRepository.findById(item.getVarianteId())
                        .orElseThrow(() -> new ExceptionDataNotFound("Variante no encontrada: " + item.getVarianteId()));
                if (variante.getStock() < item.getCantidad()) {
                    throw new RuntimeException("Stock insuficiente en variante id " + item.getVarianteId()
                            + ". Disponible: " + variante.getStock() + ", solicitado: " + item.getCantidad());
                }
                variante.setStock(variante.getStock() - item.getCantidad());
                iVarianteRepository.save(variante);
            }

            double precioCosto = prod.getPrecioCosto();
            double subTotal    = item.getSubTotal();
            double costoTotal  = precioCosto * item.getCantidad();
            double comision    = subTotal * (tasaTarifa + tasaIva);
            double ganancia    = subTotal - costoTotal - comision;

            DetalleVenta det = new DetalleVenta();
            det.setCantidad(item.getCantidad());
            det.setPrecioUnitario(item.getPrecioVenta());
            det.setSubTotal(subTotal);
            det.setPrecioCosto(precioCosto);
            det.setGanancia(ganancia);
            det.setFechaVenta(LocalDate.now());
            det.setProducto(prod);
            det.setVenta(venta);
            detalles.add(det);
        }

        double totalVenta    = detalles.stream().mapToDouble(DetalleVenta::getSubTotal).sum();
        double gananciaTotal = detalles.stream().mapToDouble(DetalleVenta::getGanancia).sum();

        venta.setTotalVenta(totalVenta);
        venta.setGananciaTotal(gananciaTotal);
        venta.setDetalles(detalles);

        return iVentaRepository.save(venta);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public List<TotalDetalle> getTotalDetalle() {
        return entityManager.createNativeQuery("CALL inventario_key.TOTAL_DETALLE()", TotalDetalle.class)
                .getResultList();
    }
}