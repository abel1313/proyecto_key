package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.entity.ClienteSinRegistro;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;

import java.util.List;
import java.util.Optional;

public interface IClienteSinRegistro extends ICrud<
        ClienteSinRegistro,
        List<ClienteSinRegistro>,
        Optional<ClienteSinRegistro>,
        Integer,
        PginaDto<List<ClienteSinRegistro>>> {
}
