package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.CodigoBarra;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.ICodigoBarrasRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Crea/sincroniza un par Producto+Variante "sombra" para catalogos de flores eternas (TipoFlor,
// ColorFlor, AccesorioRamo, FraseListonPredefinida, LugarEntrega) que necesitan poder aparecer
// como una linea real de DetallePedido -- asi savePedido() (PedidoServiceImpl) los vende,
// descuenta stock, calcula ganancia y valida el precio exactamente igual que a cualquier
// producto normal, sin que haya que tocar ese codigo. Estas variantes no se editan a mano, solo
// via este service. Se marcan con Producto.esCatalogoInterno=true para que el resto del sistema
// (buscadores, selectores de promocion/rifa, reportes) las excluya explicitamente.
@Service
public class ProductoSombraServiceImpl {

    // Stock "sin control de inventario" para catalogos que no trackean cantidad real
    // (accesorios, frases de liston, envios). Alto a proposito para que nunca bloquee una venta.
    public static final int STOCK_SIN_CONTROL = 999999;

    private final IProductosRepository iProductosRepository;
    private final IVarianteRepository iVarianteRepository;
    private final ICodigoBarrasRepository iCodigoBarrasRepository;

    public ProductoSombraServiceImpl(IProductosRepository iProductosRepository, IVarianteRepository iVarianteRepository,
                                      ICodigoBarrasRepository iCodigoBarrasRepository) {
        this.iProductosRepository = iProductosRepository;
        this.iVarianteRepository = iVarianteRepository;
        this.iCodigoBarrasRepository = iCodigoBarrasRepository;
    }

    public Variantes crear(String nombre, Double precioVenta, Double precioCosto, int stock) {
        return crear(nombre, precioVenta, precioCosto, stock, null);
    }

    @Transactional
    public Variantes crear(String nombre, Double precioVenta, Double precioCosto, int stock, String color) {
        Producto producto = new Producto();
        aplicarDatos(producto, nombre, precioVenta, precioCosto, stock);
        Producto productoGuardado = iProductosRepository.save(producto);

        Variantes variante = new Variantes();
        variante.setProducto(productoGuardado);
        variante.setStock(stock);
        variante.setColor(color);
        variante.setHabilitado('1');
        return iVarianteRepository.save(variante);
    }

    public void sincronizar(Variantes variante, String nombre, Double precioVenta, Double precioCosto, int stock) {
        sincronizar(variante, nombre, precioVenta, precioCosto, stock, variante.getColor());
    }

    @Transactional
    public void sincronizar(Variantes variante, String nombre, Double precioVenta, Double precioCosto, int stock, String color) {
        Producto producto = variante.getProducto();
        aplicarDatos(producto, nombre, precioVenta, precioCosto, stock);
        iProductosRepository.save(producto);

        variante.setStock(stock);
        variante.setColor(color);
        iVarianteRepository.save(variante);
    }

    private void aplicarDatos(Producto producto, String nombre, Double precioVenta, Double precioCosto, int stock) {
        producto.setNombre("[Flores eternas] " + nombre);
        producto.setPrecioVenta(precioVenta);
        producto.setPrecioCosto(precioCosto != null ? precioCosto : 0.0);
        producto.setStock(stock);
        producto.setHabilitado('1');
        producto.setCodigoBarrasGenerado(false);
        producto.setEsCatalogoInterno(true);
        // piezas y precio_rebaja son columnas NOT NULL heredadas (preexistentes a flores
        // eternas) que no aplican a un producto sombra -- mismo placeholder que ya usa
        // CargaImagenesServiceImpl.getProducto() para estas mismas columnas.
        producto.setPiezas(0.0);
        producto.setPrecioRebaja(0.0);
        // producto.codigo_barras_id es NOT NULL en BD -- los productos "sombra" no tienen un
        // codigo de barras real (no se venden fisicamente escaneados), asi que se les asigna
        // uno placeholder para poder guardarse, igual que hace CargaImagenesServiceImpl.
        if (producto.getCodigoBarras() == null) {
            CodigoBarra codigoBarras = new CodigoBarra();
            codigoBarras.setCodigoBarras(generarCodigoBarrasPlaceholder());
            producto.setCodigoBarras(iCodigoBarrasRepository.save(codigoBarras));
        }
    }

    private String generarCodigoBarrasPlaceholder() {
        return "SOMBRA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
