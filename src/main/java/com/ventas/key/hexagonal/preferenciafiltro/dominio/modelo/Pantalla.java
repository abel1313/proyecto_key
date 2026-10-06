package com.ventas.key.hexagonal.preferenciafiltro.dominio.modelo;

import com.ventas.key.hexagonal.preferenciafiltro.dominio.excepcion.PreferenciaFiltroException;

/**
 * [Hexagonal: dentro del hexagono] [Clean: Entities]
 *
 * <p>Pantallas que guardan filtros (R2). La clave viaja en la URL; la ruta es la de
 * {@code submenu.ruta}, la misma que usa SecurityConfig para el permiso.
 */
public enum Pantalla {

    TIENDA_BUSCAR("tienda-buscar", "tienda/buscar"),
    PRODUCTOS_BUSCAR("productos-buscar", "productos/buscar"),
    /** Lista de pedidos del administrador (2026-10-06). */
    PEDIDOS_MIS_PEDIDOS("pedidos-mis-pedidos", "pedidos/mis-pedidos");

    private final String clave;
    private final String ruta;

    Pantalla(String clave, String ruta) {
        this.clave = clave;
        this.ruta = ruta;
    }

    public String clave() {
        return clave;
    }

    public String ruta() {
        return ruta;
    }

    public static Pantalla deClave(String clave) {
        for (Pantalla p : values()) {
            if (p.clave.equals(clave)) {
                return p;
            }
        }
        throw PreferenciaFiltroException.pantallaDesconocida(clave);
    }
}
