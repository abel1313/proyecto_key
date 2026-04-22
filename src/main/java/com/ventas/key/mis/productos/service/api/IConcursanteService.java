package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.entity.Concursante;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;

import java.util.List;
import java.util.Optional;

public interface IConcursanteService extends ICrud<
        Concursante,
        List<Concursante>,
        Optional<Concursante>,
        Integer,
        PginaDto<List<Concursante>>> {

    Concursante registrar(Concursante concursante, boolean forzar) throws Exception;

    List<Concursante> buscarPorConfiguracion(Integer configurarRifaId);

    List<Concursante> buscarElegibles(Integer configurarRifaId);

    Concursante descartar(Integer concursanteId) throws Exception;
}