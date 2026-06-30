package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.models.abonos.*;

import java.util.List;

public interface IAbonoService {

    AbonoResponse registrarAbono(int pedidoId, AbonoRequest request);

    List<AbonoResponse> obtenerAbonos(int pedidoId);

    List<EstadoCuentaDto> reporteEstadoCuenta();

    List<ReportePagadosDto> reportePagados();

    List<ReporteCanceladosDto> reporteCancelados();

    CancelarAbonoResponse cancelarPedido(int pedidoId, CancelarAbonoRequest request);

    TransferirAbonoResponse transferirAbono(int pedidoIdOrigen, TransferirAbonoRequest request);
}
