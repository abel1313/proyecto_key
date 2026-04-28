package com.ventas.key.mis.productos.models;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImagenProductoResult {

    private Integer productoId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long imagenId;

}
