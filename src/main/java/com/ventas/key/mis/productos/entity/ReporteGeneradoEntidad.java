package com.ventas.key.mis.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tabla {@code reporte_generado} (migration_reporte_categoria.sql): un reporte pedido por el administrador que se arma en segundo plano.
 * Guarda el archivo (Excel o PDF) ya terminado y se borra solo cuando vence. Las reglas viven en {@code hexagonal/reportecategoria}.
 */
@Entity
@Table(name = "reporte_generado")
@Getter
@Setter
@NoArgsConstructor
public class ReporteGeneradoEntidad extends BaseId {

    @Column(nullable = false, length = 100)
    private String usuario;

    /** PENDIENTE, GENERANDO, LISTO o ERROR. */
    @Column(nullable = false, length = 15)
    private String estado;

    /** XLSX o PDF. */
    @Column(nullable = false, length = 5)
    private String formato;

    /** Los filtros pedidos, en texto legible (para la lista de reportes). */
    @Column(length = 500)
    private String descripcion;

    @Column(name = "nombre_archivo", length = 150)
    private String nombreArchivo;

    @Lob
    @Column(name = "contenido", columnDefinition = "LONGBLOB")
    private byte[] contenido;

    /** Cuánto pesa el archivo (bytes); la lista lo muestra sin traer el archivo. */
    @Column
    private Long tamano;

    @Column(length = 500)
    private String error;

    @Column(nullable = false)
    private LocalDateTime creado;

    @Column
    private LocalDateTime terminado;

    @Column(nullable = false)
    private LocalDateTime expira;
}
