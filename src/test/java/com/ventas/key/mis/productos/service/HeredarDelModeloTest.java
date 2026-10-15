package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.PalabraClave;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeredarDelModeloTest {

    @Test
    @DisplayName("\"Crear articulos\" desde el modelo: nacen con su color, marca, descripcion, contenido y categoria")
    void heredaLosDatosCompartidos() {
        PalabraClave bolsas = new PalabraClave();
        Producto modelo = new Producto();
        modelo.setColor("Negro");
        modelo.setMarca("Zara");
        modelo.setDescripcion("Bolsa de piel");
        modelo.setContenido("1 pieza");
        modelo.setPalabraClave(bolsas);

        Variantes articulo = new Variantes();
        VarianteServiceImpl.heredarDelModelo(articulo, modelo);

        assertThat(articulo.getColor()).isEqualTo("Negro");
        assertThat(articulo.getMarca()).isEqualTo("Zara");
        assertThat(articulo.getDescripcion()).isEqualTo("Bolsa de piel");
        assertThat(articulo.getContenidoNeto()).isEqualTo("1 pieza");
        assertThat(articulo.getPalabraClave()).isSameAs(bolsas);
    }
}
