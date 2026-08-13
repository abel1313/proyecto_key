package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Crea/sincroniza un par Producto+Variante "sombra" para catalogos de flores eternas (TipoFlor,
// AccesorioRamo, FraseListonPredefinida, LugarEntrega) que necesitan poder aparecer como una
// linea real de DetallePedido -- asi savePedido() (PedidoServiceImpl) los vende, descuenta
// stock, calcula ganancia y valida el precio exactamente igual que a cualquier producto normal,
// sin que haya que tocar ese codigo. Estas variantes no se editan a mano, solo via este service.
@Service
public class ProductoSombraServiceImpl {

    // Stock "sin control de inventario" para catalogos que no trackean cantidad real
    // (accesorios, frases de liston, envios). Alto a proposito para que nunca bloquee una venta.
    public static final int STOCK_SIN_CONTROL = 999999;

    private final IProductosRepository iProductosRepository;
    private final IVarianteRepository iVarianteRepository;

    public ProductoSombraServiceImpl(IProductosRepository iProductosRepository, IVarianteRepository iVarianteRepository) {
        this.iProductosRepository = iProductosRepository;
        this.iVarianteRepository = iVarianteRepository;
    }

    @Transactional
    public Variantes crear(String nombre, Double precioVenta, Double precioCosto, int stock) {
        Producto producto = new Producto();
        aplicarDatos(producto, nombre, precioVenta, precioCosto, stock);
        Producto productoGuardado = iProductosRepository.save(producto);

        Variantes variante = new Variantes();
        variante.setProducto(productoGuardado);
        variante.setStock(stock);
        variante.setHabilitado('1');
        return iVarianteRepository.save(variante);
    }

    @Transactional
    public void sincronizar(Variantes variante, String nombre, Double precioVenta, Double precioCosto, int stock) {
        Producto producto = variante.getProducto();
        aplicarDatos(producto, nombre, precioVenta, precioCosto, stock);
        iProductosRepository.save(producto);

        variante.setStock(stock);
        iVarianteRepository.save(variante);
    }

    private void aplicarDatos(Producto producto, String nombre, Double precioVenta, Double precioCosto, int stock) {
        producto.setNombre("[Flores eternas] " + nombre);
        producto.setPrecioVenta(precioVenta);
        producto.setPrecioCosto(precioCosto != null ? precioCosto : 0.0);
        producto.setStock(stock);
        producto.setHabilitado('1');
        producto.setCodigoBarrasGenerado(false);
    }
}
