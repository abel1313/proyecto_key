package com.ventas.key.hexagonal.campana;

import com.ventas.key.hexagonal.campana.aplicacion.servicio.ConsultarCampanaActivaService;
import com.ventas.key.hexagonal.campana.aplicacion.servicio.LimiteDeIntentos;
import com.ventas.key.hexagonal.campana.aplicacion.servicio.ParticiparEnCampanaService;
import com.ventas.key.hexagonal.campana.dominio.excepcion.CampanaInvalidaException;
import com.ventas.key.hexagonal.campana.dominio.modelo.Campana;
import com.ventas.key.hexagonal.campana.dominio.modelo.ContactoCliente;
import com.ventas.key.hexagonal.campana.dominio.modelo.DecisionDeCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.FormaDeCobro;
import com.ventas.key.hexagonal.campana.dominio.modelo.PersonaCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.PrecioDeCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.ReglasCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.SolicitudDeCampana;
import com.ventas.key.hexagonal.campana.dominio.modelo.SolicitudDeCampana.Linea;
import com.ventas.key.hexagonal.campana.dominio.modelo.VerificacionDeCodigo;
import com.ventas.key.hexagonal.campana.dominio.puerto.salida.PersonasCampanaPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Quién es recurrente y a qué precio se cobra, con puertos en memoria. Toy = producto 1 ($250 → campaña $200); bolsa = producto 2 ($500). */
class ParticiparEnCampanaTest {

    private static BigDecimal d(String s) { return new BigDecimal(s); }

    static class Personas implements PersonasCampanaPort {
        boolean disponible = true;
        final List<PersonaCampana> lista = new ArrayList<>();
        final List<Object[]> ligas = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        int sig = 1;

        public boolean disponible() { return disponible; }
        public Optional<PersonaCampana> porUuid(String uuid) { return lista.stream().filter(p -> p.uuid().equals(uuid)).findFirst(); }
        public Optional<PersonaCampana> porContacto(String correo, String tel) {
            return lista.stream().filter(p -> (correo != null && correo.equals(p.correo())) || (tel != null && !tel.isEmpty() && tel.equals(ContactoCliente.ultimos10(p.telefono())))).findFirst();
        }
        public PersonaCampana guardar(PersonaCampana p) {
            PersonaCampana g = new PersonaCampana(sig++, p.uuid(), p.nombre(), p.correo(), p.telefono(), p.consentimiento());
            lista.add(g);
            return g;
        }
        public BigDecimal acumulado(int c, int p) { return acumulado; }
        public void ligarPedido(int c, int p, int ped, BigDecimal m) { ligas.add(new Object[]{c, p, ped, m}); }
        public Optional<PersonaCampana> dePedido(int pedidoId) {
            return ligas.stream().filter(l -> (int) l[2] == pedidoId).findFirst().flatMap(l -> lista.stream().filter(p -> p.id().equals(l[1])).findFirst());
        }
        public List<com.ventas.key.hexagonal.campana.dominio.modelo.ResumenDePersona> resumen(int campanaId) {
            return lista.stream().map(p -> new com.ventas.key.hexagonal.campana.dominio.modelo.ResumenDePersona(p, p.nombre().equals("Ana") ? d("1600") : d("300"), 2)).toList();
        }
        public void eliminar(int id) { lista.removeIf(p -> p.id() == id); }
    }

    Personas personas;
    List<String> correos;
    ParticiparEnCampanaService servicio;
    ZoneId z = ZoneId.of("America/Mexico_City");
    Clock reloj = Clock.fixed(LocalDate.of(2026, 12, 25).atStartOfDay(z).toInstant(), z);
    ContactoCliente ana = new ContactoCliente("Ana", "Ana@Mail.com", "(722) 123-4567");

    @BeforeEach
    void preparar() {
        personas = new Personas();
        correos = new ArrayList<>();
        CampanaDominioYServiciosTest.Memoria campanas = new CampanaDominioYServiciosTest.Memoria();
        campanas.guardar(new Campana(null, "Reyes", LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 6), true, d("1500"),
                List.of(new PrecioDeCampana(1, "Carro", d("200")))));
        servicio = new ParticiparEnCampanaService(new ConsultarCampanaActivaService(campanas, reloj), personas,
                (persona, campana, destino, primeraVez) -> correos.add(destino + "|" + campana),
                new LimiteDeIntentos(reloj, 3, Duration.ofMinutes(10)), () -> "uuid-nuevo");
    }

    private SolicitudDeCampana venta(String codigo, boolean manual, boolean privacidad, FormaDeCobro cobro, ContactoCliente c, Linea... lineas) {
        return new SolicitudDeCampana(codigo, manual, privacidad, cobro, c, List.of(lineas));
    }

    private static final Linea JUGUETE = new Linea(1, 1, d("250"));
    private static Linea bolsa(int piezas) { return new Linea(2, piezas, d("500")); }

    private PersonaCampana ana() {
        return personas.guardar(new PersonaCampana(null, "abc-123", "Ana", "ana@mail.com", "7221234567", true));
    }

    @Test
    @DisplayName("matriz de precios: solo recurrente, solo Contado/Apartado, solo si baja")
    void matriz() {
        assertThat(ReglasCampana.precioParaLinea(FormaDeCobro.CONTADO, true, d("250"), d("200"))).isEqualByComparingTo("200");
        assertThat(ReglasCampana.precioParaLinea(FormaDeCobro.APARTADO, true, d("250"), d("200"))).isEqualByComparingTo("200");
        assertThat(ReglasCampana.precioParaLinea(FormaDeCobro.IR_PAGANDO, true, d("250"), d("200"))).isEqualByComparingTo("250");
        assertThat(ReglasCampana.precioParaLinea(FormaDeCobro.CONTADO, false, d("250"), d("200"))).isEqualByComparingTo("250");
        assertThat(ReglasCampana.precioParaLinea(FormaDeCobro.CONTADO, true, d("150"), d("200"))).as("gana el más bajo").isEqualByComparingTo("150");
    }

    @Test
    @DisplayName("califica: manual ignora la meta; la meta suma acumulado + ticket; meta 0 = solo a mano")
    void califica() {
        assertThat(ReglasCampana.califica(d("0"), d("0"), d("1500"), true)).isTrue();
        assertThat(ReglasCampana.califica(d("1000"), d("500"), d("1500"), false)).isTrue();
        assertThat(ReglasCampana.califica(d("1000"), d("499.99"), d("1500"), false)).isFalse();
        assertThat(ReglasCampana.califica(d("9999"), d("9999"), d("0"), false)).isFalse();
    }

    @Test
    @DisplayName("sin campaña vigente o sin las tablas, la venta no cambia")
    void sinCampana() {
        personas.disponible = false;
        assertThat(servicio.decidir(venta(null, true, true, FormaDeCobro.CONTADO, ana, JUGUETE))).isSameAs(DecisionDeCampana.NINGUNA);
        personas.disponible = true;
        CampanaDominioYServiciosTest.Memoria vacia = new CampanaDominioYServiciosTest.Memoria();
        var s = new ParticiparEnCampanaService(new ConsultarCampanaActivaService(vacia, reloj), personas, (p, c, d, n) -> { },
                new LimiteDeIntentos(reloj, 3, Duration.ofMinutes(1)), () -> "x");
        assertThat(s.decidir(venta(null, true, true, FormaDeCobro.CONTADO, ana, JUGUETE))).isSameAs(DecisionDeCampana.NINGUNA);
    }

    @Test
    @DisplayName("cliente recurrente a mano: el juguete baja en Contado y Apartado, no en Ir pagando")
    void manual() {
        assertThat(servicio.decidir(venta(null, true, false, FormaDeCobro.CONTADO, ana, JUGUETE)).precioDe(1)).isEqualByComparingTo("200");
        assertThat(servicio.decidir(venta(null, true, false, FormaDeCobro.APARTADO, ana, JUGUETE)).precioDe(1)).isEqualByComparingTo("200");
        assertThat(servicio.decidir(venta(null, true, false, FormaDeCobro.IR_PAGANDO, ana, JUGUETE)).precioDe(1)).isNull();
        assertThat(servicio.decidir(venta(null, false, false, FormaDeCobro.CONTADO, ana, JUGUETE)).precioDe(1)).isNull();
    }

    @Test
    @DisplayName("por meta: solo cuenta lo YA pagado; el ticket de esta venta no se suma a sí mismo (con $1,000 de $1,500 no hay descuento)")
    void porMeta() {
        ana();
        personas.acumulado = d("1000");
        // 1000 < 1500: aunque lleve bolsas de $500 en este ticket, todavía no es recurrente
        assertThat(servicio.decidir(venta("abc-123", false, false, FormaDeCobro.CONTADO, ana, JUGUETE, bolsa(1))).precioDe(1)).isNull();
        // ya pagó 1500 antes: sí
        personas.acumulado = d("1500");
        assertThat(servicio.decidir(venta("abc-123", false, false, FormaDeCobro.CONTADO, ana, JUGUETE)).precioDe(1)).isEqualByComparingTo("200");
    }

    @Test
    @DisplayName("un código de otra persona, o que no existe, se ignora: se cobra normal")
    void codigoAjeno() {
        personas.guardar(new PersonaCampana(null, "ajeno-1", "Beto", "beto@mail.com", "5551112222", true));
        personas.acumulado = d("5000");
        assertThat(servicio.decidir(venta("ajeno-1", false, false, FormaDeCobro.CONTADO, ana, JUGUETE, bolsa(1))).precioDe(1)).isNull();
        assertThat(servicio.decidir(venta("no-existe", false, false, FormaDeCobro.CONTADO, ana, JUGUETE, bolsa(1))).precioDe(1)).isNull();
    }

    @Test
    @DisplayName("el código vale si coincide el correo O el teléfono del cliente (mayúsculas y formato no importan)")
    void coincide() {
        ana();
        personas.acumulado = d("5000");
        var soloTelefono = new ContactoCliente("Ana", null, "+52 722 123 4567");
        var soloCorreo = new ContactoCliente("Ana", "ANA@mail.com", null);
        assertThat(servicio.decidir(venta("ABC-123", false, false, FormaDeCobro.CONTADO, soloTelefono, JUGUETE)).precioDe(1)).isNotNull();
        assertThat(servicio.decidir(venta("abc-123", false, false, FormaDeCobro.CONTADO, soloCorreo, JUGUETE)).precioDe(1)).isNotNull();
    }

    @Test
    @DisplayName("quien compra con sus datos y acepta privacidad recibe código; sin aceptar, no se guarda nada")
    void creaPersona() {
        var sinAceptar = servicio.decidir(venta(null, false, false, FormaDeCobro.CONTADO, ana, bolsa(1)));
        assertThat(sinAceptar.candidata()).isNull();
        servicio.registrarCompra(sinAceptar, 10, FormaDeCobro.CONTADO);
        assertThat(personas.lista).isEmpty();

        var aceptando = servicio.decidir(venta(null, false, true, FormaDeCobro.CONTADO, ana, bolsa(1)));
        assertThat(aceptando.candidata().uuid()).isEqualTo("uuid-nuevo");
        servicio.registrarCompra(aceptando, 11, FormaDeCobro.CONTADO);
        assertThat(personas.lista).hasSize(1);
        assertThat(personas.ligas.get(0)[2]).isEqualTo(11);
        assertThat(personas.ligas.get(0)[3]).isEqualTo(d("500"));
        assertThat(correos).containsExactly("ana@mail.com|Reyes");

        // misma persona otra vez: no se crea otro código, pero el correo con su código sale en CADA compra (pedido del dueño 2026-11-08)
        var otra = servicio.decidir(venta(null, false, true, FormaDeCobro.CONTADO, ana, bolsa(1)));
        servicio.registrarCompra(otra, 12, FormaDeCobro.CONTADO);
        assertThat(personas.lista).hasSize(1);
        assertThat(correos).hasSize(2);
        assertThat(personas.ligas).hasSize(2);
    }

    @Test
    @DisplayName("solo el Contado suma hacia la meta; Apartado e Ir pagando se ligan con 0")
    void soloContadoSuma() {
        var apartado = servicio.decidir(venta(null, false, true, FormaDeCobro.APARTADO, ana, bolsa(1)));
        servicio.registrarCompra(apartado, 20, FormaDeCobro.APARTADO);
        assertThat(personas.ligas.get(0)[3]).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("un correo que falla no tira el registro")
    void correoFalla() {
        var s = new ParticiparEnCampanaService(new ConsultarCampanaActivaService(unaCampana(), reloj), personas,
                (p, c, d, n) -> { throw new IllegalStateException("smtp caído"); }, new LimiteDeIntentos(reloj, 3, Duration.ofMinutes(1)), () -> "u1");
        var dec = s.decidir(venta(null, false, true, FormaDeCobro.CONTADO, ana, bolsa(1)));
        s.registrarCompra(dec, 30, FormaDeCobro.CONTADO);
        assertThat(personas.lista).hasSize(1);
    }

    private CampanaDominioYServiciosTest.Memoria unaCampana() {
        var m = new CampanaDominioYServiciosTest.Memoria();
        m.guardar(new Campana(null, "Reyes", LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 6), true, d("1500"), List.of(new PrecioDeCampana(1, "Carro", d("200")))));
        return m;
    }

    @Test
    @DisplayName("verificar: OK con avance, NO_COINCIDE si es de otra persona, NO_EXISTE; y frena el adivinar")
    void verificar() {
        ana();
        personas.acumulado = d("400");
        var ok = servicio.verificar("admin", "ABC-123", ana);
        assertThat(ok.estado()).isEqualTo(VerificacionDeCodigo.Estado.OK);
        assertThat(ok.nombre()).isEqualTo("Ana");
        assertThat(ok.falta()).isEqualByComparingTo("1100");
        assertThat(servicio.verificar("admin", "abc-123", new ContactoCliente("X", "otro@mail.com", "5550000000")).estado())
                .isEqualTo(VerificacionDeCodigo.Estado.NO_COINCIDE);
        assertThat(servicio.verificar("admin", "nada", ana).estado()).isEqualTo(VerificacionDeCodigo.Estado.NO_EXISTE);
        assertThatThrownBy(() -> servicio.verificar("admin", "otra", ana)).isInstanceOf(CampanaInvalidaException.class).hasMessageContaining("Demasiados intentos");
        assertThat(servicio.verificar("otro-usuario", "abc-123", ana).estado()).as("el límite es por usuario").isEqualTo(VerificacionDeCodigo.Estado.OK);
    }

    @Test
    void eliminarPersona() {
        PersonaCampana p = ana();
        servicio.eliminarPersona(p.id());
        assertThat(personas.lista).isEmpty();
    }

    @Test
    @DisplayName("reenviar código: manda el correo si el pedido tiene persona con correo; avisa si no hay código o correo")
    void reenviar() {
        PersonaCampana ana = ana();
        personas.ligarPedido(1, ana.id(), 50, BigDecimal.ZERO);
        assertThat(servicio.pedidoTieneCodigo(50)).isTrue();
        assertThat(servicio.pedidoTieneCodigo(51)).isFalse();
        assertThat(servicio.reenviarCodigo(50)).isEqualTo(com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ParticiparEnCampanaCasoUso.ResultadoReenvio.ENVIADO);
        assertThat(correos).containsExactly("ana@mail.com|Reyes");
        assertThat(servicio.reenviarCodigo(51)).isEqualTo(com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ParticiparEnCampanaCasoUso.ResultadoReenvio.SIN_CODIGO);

        PersonaCampana sinCorreo = personas.guardar(new PersonaCampana(null, "u-2", "Beto", null, "5551112222", true));
        personas.ligarPedido(1, sinCorreo.id(), 60, BigDecimal.ZERO);
        assertThat(servicio.reenviarCodigo(60)).isEqualTo(com.ventas.key.hexagonal.campana.dominio.puerto.entrada.ParticiparEnCampanaCasoUso.ResultadoReenvio.SIN_CORREO);
    }

    @Test
    @DisplayName("reporte: ordena por lo acumulado y marca quién llegó a la meta de $1,500")
    void reporte() {
        ana();
        personas.guardar(new PersonaCampana(null, "u-b", "Beto", null, "5551112222", true));
        var campanas = new CampanaDominioYServiciosTest.Memoria();
        campanas.guardar(new Campana(null, "Reyes", LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 6), true, d("1500"), List.of(new PrecioDeCampana(1, "Carro", d("200")))));
        var r = new com.ventas.key.hexagonal.campana.aplicacion.servicio.ConsultarReporteDeCampanaService(campanas, personas).reporte(1);
        assertThat(r.personas()).extracting(f -> f.nombre()).containsExactly("Ana", "Beto");
        assertThat(r.personas().get(0).llegoALaMeta()).isTrue();
        assertThat(r.personas().get(1).llegoALaMeta()).isFalse();
        assertThat(r.llegaronALaMeta()).isEqualTo(1);
        assertThat(r.totalAcumulado()).isEqualByComparingTo("1900");
        assertThatThrownBy(() -> new com.ventas.key.hexagonal.campana.aplicacion.servicio.ConsultarReporteDeCampanaService(campanas, personas).reporte(99))
                .isInstanceOf(CampanaInvalidaException.class);
    }
}
