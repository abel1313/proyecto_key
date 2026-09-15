package com.ventas.key.mis.productos.dto;

import com.ventas.key.mis.productos.entity.EstadoCargaImagen;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Estado de la carga rapida de imagenes, leido directo de producto/variante — no hay
// tabla de seguimiento aparte. Se usa como response de subir-imagen, estado, fallidas
// y reintentar-imagen en CargaImagenesController.
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EstadoCargaProductoDto {

    private Integer productoId;
    private Integer varianteId;
    private EstadoCargaImagen estadoImagen;
    private Long imagenId;
    private String urlImagen;
    private String mensajeErrorImagen;

    // Datos ya capturados del borrador. Van aqui porque la pantalla de Carga rapida es el
    // unico lugar donde se completa un borrador y necesita repintar el formulario con lo
    // que ya se guardo: sin estos campos, "Guardar avance" persistia bien en la base pero
    // al reabrir la tarjeta el formulario salia en blanco y se leia como que no habia
    // guardado nada.
    private String nombre;
    private Double precioCosto;
    private Double piezas;
    private String color;
    private Double precioVenta;
    private Double precioRebaja;
    private String descripcion;
    private String marca;
    private String contenido;
    private Integer palabraClaveId;
    private String palabraClaveNombre;

    // Solo el codigo REAL. El placeholder autogenerado (BRD-...) viaja como null a
    // proposito: el front nunca lo muestra ni lo precarga en el campo.
    private String codigoBarras;

    // Estado base, sin datos capturados: es lo que hay cuando el borrador acaba de nacer
    // (subir-imagen) o se reintenta la foto. Los campos capturados se llenan con setters
    // desde construirEstados(), que es el unico que lee el producto ya completado.
    public EstadoCargaProductoDto(Integer productoId, Integer varianteId, EstadoCargaImagen estadoImagen,
                                  Long imagenId, String urlImagen, String mensajeErrorImagen) {
        this.productoId = productoId;
        this.varianteId = varianteId;
        this.estadoImagen = estadoImagen;
        this.imagenId = imagenId;
        this.urlImagen = urlImagen;
        this.mensajeErrorImagen = mensajeErrorImagen;
    }
}
