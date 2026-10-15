package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * La llave de DipoMex ya no está escrita en el código (2026-10-15): llega por DIPOMEX_API_KEY.
 * Sin ella el servicio debe arrancar igual (el código postal se pide a mano), no reventar.
 */
class DopoMexServiceTest {

    @Test
    void sinLlave_elServicioArranca() {
        assertDoesNotThrow(() -> new DopoMexService(WebClient.builder(), ""));
        assertDoesNotThrow(() -> new DopoMexService(WebClient.builder(), null));
    }

    @Test
    void codigoQueNoSonCincoDigitos_dice404SinLlamarAlServicioExterno() {
        DopoMexService servicio = new DopoMexService(WebClient.builder(), "");
        assertThrows(ExceptionDataNotFound.class, () -> servicio.getCodigoPostal("123").block());
    }
}
