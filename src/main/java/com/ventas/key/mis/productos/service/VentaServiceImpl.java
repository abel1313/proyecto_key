package com.ventas.key.mis.productos.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ventas.key.mis.productos.entity.DetalleVenta;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.Venta;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.DetalleVentaDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.TotalDetalle;
import com.ventas.key.mis.productos.repository.IDetalleVentaRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVentaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class VentaServiceImpl extends CrudAbstractServiceImpl< Venta,List<Venta>,Optional<Venta>, Integer, PginaDto<List<Venta>>> {

    @PersistenceContext
    private EntityManager entityManager;

    private final IVentaRepository iVentaRepository;
    private final IProductosRepository iRepository;
    private final IDetalleVentaRepository iDetalleVentaRepository;
    private final ErrorGenerico errorGenerico;
    public VentaServiceImpl(
        final IVentaRepository iVentaRepository,
        final IProductosRepository iRepository,
        final IDetalleVentaRepository iDetalleVentaRepository,
        final ErrorGenerico errorGenerico
    ){
        super(iVentaRepository, errorGenerico);
        this.iVentaRepository = iVentaRepository;
        this.iRepository = iRepository;
        this.iDetalleVentaRepository = iDetalleVentaRepository;
        this.errorGenerico = errorGenerico;
    }

    
    Venta saveVenta(final Venta venta){
        return iVentaRepository.save(venta);
    }


    @Transactional
    public Venta saveVentaDetalle(List<DetalleVentaDto> detall){

        
        Double tot = detall.stream().mapToDouble(DetalleVentaDto::getSubTotal).sum();
        Venta venta = new Venta();
        //venta.setUsuarioId(1);
        venta.setEstadoVenta("null");
        venta.setFormaPago("null");
        venta.setTotalVenta(tot);

        Venta ve = this.iVentaRepository.save(venta); 

        List<DetalleVenta> detalleVenta = detall.stream().map(m->{
            DetalleVenta det = new DetalleVenta();
            det.setCantidad(m.getCantidad());
            det.setPrecioUnitario(m.getPrecioVenta());
            Producto pro;
            pro = this.iRepository.findByCodigoBarras_CodigoBarrasAndNombre(m.getCodigoBarras(),m.getNombre()).orElse(new Producto());
            det.setVenta(ve);
            det.setProducto(pro);
            det.setSubTotal(m.getSubTotal());
            return det;
        }).collect(Collectors.toList());

        try {
            List<DetalleVenta> dtVenta = this.iDetalleVentaRepository.saveAll(detalleVenta);
        } catch (Exception e) {
            // TODO: handle exception
        }
        return ve;

    }

        
    @Transactional
    @SuppressWarnings("unchecked")
    public List<TotalDetalle> getTotalDetalle() {
        return  entityManager.createNativeQuery("CALL inventario_key.TOTAL_DETALLE()", TotalDetalle.class)
                            .getResultList();

    }
    
}
