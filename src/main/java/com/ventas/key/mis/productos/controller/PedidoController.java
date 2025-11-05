package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.models.pedidos.PedidoGenerico;
import com.ventas.key.mis.productos.models.pedidos.PedidosDTOPedido;
import com.ventas.key.mis.productos.service.PedidoServiceImpl;
import com.ventas.key.mis.productos.service.api.IPedidoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("pedidos")
@Slf4j
public class PedidoController extends AbstractController<
                                        Pedido,
                                        Optional<Pedido>,
                                        List<Pedido>,
                                        Integer,
                                        PginaDto<List<Pedido>>,
                                        PedidoServiceImpl>{

    private final IPedidoService iPedidoService;


    public PedidoController(PedidoServiceImpl sGenerico,
                            final IPedidoService iPedidoService) {
        super(sGenerico);
        this.iPedidoService =iPedidoService;
    }

    @PostMapping("/savePedido")
    public ResponseEntity<ResponseGeneric<Pedido>> savePedido(@Validated @RequestBody PedidosDTOPedido requestG, BindingResult result) {
        try {
            if( result.hasErrors()){
                String errores = result.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(", "));
                ResponseGeneric<Pedido> erroResponse = new ResponseGeneric<>((Pedido) null);
                erroResponse.setMensaje(errores);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erroResponse);
            }
            Pedido response = iPedidoService.savePedido(requestG,result);
            log.info("ingo data {} ",response);
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseGeneric<>(response));
        } catch (Exception e) {
            return null;
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ResponseGeneric<PedidoGenerico>> updatePedido(@PathVariable int id, @Validated @RequestBody PedidoGenerico requestG, BindingResult result) {
        try {
            PedidoGenerico response = iPedidoService.updatePedido(id, requestG);
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseGeneric<>(response));
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/findPedido/{id}")
    public ResponseEntity<ResponseGeneric<PageableDto<List<PedidoGenerico>>>> findPedidoById(@PathVariable int id, @RequestParam int size, @RequestParam int page) {
        try {
            PageableDto<List<PedidoGenerico>> response = iPedidoService.obtenerPedido(id, size,  page);
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseGeneric<>(response));
        } catch (Exception e) {
            return null;
        }
    }
    @GetMapping("/findPedido/{idPedido}/{idCliente}")
    public ResponseEntity<ResponseGeneric<PageableDto<List<PedidoGenerico>>>> findPedioById(@PathVariable int idPedido, @PathVariable int idCliente, @RequestParam int size, @RequestParam int page) {
        try {
            PageableDto<List<PedidoGenerico>> response = iPedidoService.obtenerPedidoPorId(idPedido, idCliente, size,  page);
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseGeneric<>(response));
        } catch (Exception e) {
            return null;
        }
    }
    @GetMapping("/buscarClientePedido/{buscar}")
    public ResponseEntity<ResponseGeneric<PageableDto<List<PedidoGenerico>>>> buscarClienteNombre(@PathVariable String buscar, @RequestParam int size, @RequestParam int page) {
        try {
            PageableDto<List<PedidoGenerico>> response = iPedidoService.buscarClientePorPedido(buscar, size,  page);
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseGeneric<>(response));
        } catch (Exception e) {
            return null;
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable int id) {
        try {
            this.iPedidoService.deletePedidoById(id);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            return null;
        }
    }
}
