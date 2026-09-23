package com.ventas.key.hexagonal.rifa.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.rifa.dominio.modelo.GrupoDeBoletos;
import com.ventas.key.hexagonal.rifa.dominio.modelo.Participacion;
import com.ventas.key.hexagonal.rifa.dominio.modelo.PerfilEnRed;
import com.ventas.key.hexagonal.rifa.dominio.modelo.Plataforma;
import com.ventas.key.hexagonal.rifa.dominio.puerto.entrada.GestionarBoletosCasoUso;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoletoAgrupadoControllerTest {

    @Test
    @DisplayName("la lista de grupos viaja en 'data', que es lo que lee el front")
    void verAgrupadosEnData() {
        GestionarBoletosCasoUso casoUso = mock(GestionarBoletosCasoUso.class);
        when(casoUso.verAgrupados(3)).thenReturn(List.of(new GrupoDeBoletos(7, "Juan Perez",
                new PerfilEnRed(Plataforma.FACEBOOK, "facebook.com/juan"),
                List.of(new Participacion(1, "facebook.com/post/1", "compartio", LocalDate.now())))));

        ResponseGeneric<?> body = (ResponseGeneric<?>) new BoletoAgrupadoController(casoUso)
                .verAgrupados(3).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getData()).isInstanceOf(List.class);
        assertThat((List<?>) body.getData()).hasSize(1);
        assertThat(body.getLista()).isNull();
    }
}
