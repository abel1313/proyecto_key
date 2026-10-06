package com.ventas.key.hexagonal.busquedapedido.infraestructura.entrada.rest;

import com.ventas.key.hexagonal.busquedapedido.dominio.excepcion.FiltroInvalidoException;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.CuandoSeEntrega;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.EstadoBuscado;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.FiltroPedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.FormaDeCobro;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.ModoDeEntrega;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.OrdenDePedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.PaginaDePedidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.PedidosUnidos;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.RangoDeFechas;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.SituacionDeDinero;
import com.ventas.key.hexagonal.busquedapedido.dominio.modelo.TextoBuscado;
import com.ventas.key.hexagonal.busquedapedido.dominio.puerto.entrada.BuscarPedidosCasoUso;
import com.ventas.key.hexagonal.busquedapedido.infraestructura.dto.PedidosEncontradosResponse;
import com.ventas.key.hexagonal.busquedapedido.infraestructura.salida.persistencia.TarjetasDePedidoLector;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * [Hexagonal: Driving Adapter] [Clean: Interface Adapters — Controllers]
 *
 * <p>La lista de pedidos del administrador con todos los filtros (reglas en el README del
 * dominio). Permiso: solo ADMIN, igual que {@code buscarClientePedido}, que busca en los pedidos de
 * todos los clientes (SecurityConfig).
 *
 * <p>Las opciones llegan como texto y se traducen aqui, para que un valor mal escrito conteste un
 * 400 que diga cuales valen, en vez del error generico de conversion de Spring.
 */
@RestController
@RequestMapping("/v1/pedidos")
@RequiredArgsConstructor
public class BuscarPedidosController {

    private final BuscarPedidosCasoUso casoUso;
    private final TarjetasDePedidoLector tarjetas;

    @GetMapping("/buscar")
    public ResponseEntity<ResponseGeneric<PedidosEncontradosResponse>> buscar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) List<String> formaCobro,
            @RequestParam(required = false) List<String> estado,
            @RequestParam(required = false) List<String> dinero,
            @RequestParam(required = false) Double totalDesde,
            @RequestParam(required = false) Double totalHasta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registroDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registroHasta,
            @RequestParam(required = false) String entrega,
            @RequestParam(required = false) Integer lugarEntregaId,
            @RequestParam(required = false) String modoEntrega,
            @RequestParam(required = false) String unidos,
            @RequestParam(defaultValue = "false") boolean soloRamos,
            @RequestParam(defaultValue = "false") boolean soloConPromocion,
            @RequestParam(required = false) String orden,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {

        FiltroPedidos filtro = new FiltroPedidos(
                TextoBuscado.de(buscar),
                lista(formaCobro, FormaDeCobro.class, "formaCobro"),
                lista(estado, EstadoBuscado.class, "estado"),
                lista(dinero, SituacionDeDinero.class, "dinero"),
                totalDesde, totalHasta,
                RangoDeFechas.de(registroDesde, registroHasta),
                uno(entrega, CuandoSeEntrega.class, "entrega"),
                lugarEntregaId,
                uno(modoEntrega, ModoDeEntrega.class, "modoEntrega"),
                uno(unidos, PedidosUnidos.class, "unidos"),
                soloRamos, soloConPromocion,
                uno(orden, OrdenDePedidos.class, "orden"),
                pagina, tamano);

        PaginaDePedidos encontrados = casoUso.buscar(filtro);
        return ResponseEntity.ok(new ResponseGeneric<>(new PedidosEncontradosResponse(
                tarjetas.tarjetas(encontrados.pedidoIds()),
                encontrados.totalPaginas(),
                encontrados.totalRegistros(),
                encontrados.pagina())));
    }

    @ExceptionHandler(FiltroInvalidoException.class)
    public ResponseEntity<ResponseGeneric<Object>> filtroInvalido(FiltroInvalidoException e) {
        ResponseGeneric<Object> cuerpo = new ResponseGeneric<>((Object) null);
        cuerpo.setMensaje(e.getMessage());
        cuerpo.setCode(HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(cuerpo);
    }

    private static <E extends Enum<E>> Set<E> lista(List<String> valores, Class<E> tipo, String parametro) {
        if (valores == null) {
            return Set.of();
        }
        // ?estado=PAGADO,CANCELADO y ?estado=PAGADO&estado=CANCELADO valen igual.
        return valores.stream()
                .flatMap(v -> Arrays.stream(v.split(",")))
                .filter(v -> !v.isBlank())
                .map(v -> uno(v, tipo, parametro))
                .collect(Collectors.toSet());
    }

    private static <E extends Enum<E>> E uno(String valor, Class<E> tipo, String parametro) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            String validos = Arrays.stream(tipo.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
            throw new FiltroInvalidoException(
                    "\"" + valor + "\" no es un valor de " + parametro + ". Valen: " + validos);
        }
    }
}
