package com.ventas.key.mis.productos.redessociales;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RevisionConfigRedesSocialesTest {

    @Test
    void todoCargadoNoFaltaNada() {
        RevisionConfigRedesSociales r = new RevisionConfigRedesSociales("1", "t", "s", "v", "2", "k", "c");
        assertThat(r.faltantes()).isEmpty();
    }

    @Test
    void vaciasOSoloEspaciosCuentanComoFaltantes() {
        // El caso real de prod: la variable existía en el pod pero sin valor.
        RevisionConfigRedesSociales r = new RevisionConfigRedesSociales("1", "t", "s", "v", "2", "", "  ");
        assertThat(r.faltantes()).containsExactly("TIKTOK_CLIENT_KEY", "TIKTOK_CLIENT_SECRET");
    }

    @Test
    void nullCuentaComoFaltante() {
        RevisionConfigRedesSociales r = new RevisionConfigRedesSociales(null, "t", "s", "v", null, "k", "c");
        assertThat(r.faltantes()).containsExactly("FACEBOOK_PAGE_ID", "INSTAGRAM_ACCOUNT_ID");
    }
}
