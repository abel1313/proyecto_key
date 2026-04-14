package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Direccion;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.ClienteServiceImpl;
import com.ventas.key.mis.productos.service.UsuarioDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("clientes")
@Slf4j
public class ClienteControllerImpl extends AbstractController<
        Cliente,
        Optional<Cliente>,
        List<Cliente>,
        Integer,
        PginaDto<List<Cliente>>,
        ClienteServiceImpl> {

    private final UsuarioDetailsService usuarioDetailsService;

    public ClienteControllerImpl(ClienteServiceImpl sGenerico, UsuarioDetailsService usuarioDetailsService) {
        super(sGenerico);
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    public ResponseEntity<ResponseGeneric<Cliente>> save(Cliente requestG, BindingResult result) {
        Optional<Usuario> usr = this.usuarioDetailsService.findById(requestG.getUsuario().getId().intValue());
        if (usr.isPresent()) {
            requestG.setUsuario(usr.get());
            if (usr.get().getCliente() != null && usr.get().getCliente().getId() != null) {
                requestG.setId(usr.get().getCliente().getId());
            }
        }

        Set<Direccion> direcciones = Optional.ofNullable(requestG.getListDirecciones())
                .orElse(Set.of())
                .stream()
                .map(mpa -> {
                    Direccion direccion = new Direccion();
                    direccion.setCalle(mpa.getCalle());
                    direccion.setColonia(mpa.getColonia());
                    direccion.setMunicipio(mpa.getMunicipio());
                    direccion.setReferencias(mpa.getReferencias());
                    direccion.setCodigoPostal(mpa.getCodigoPostal());
                    direccion.setPredefinida(mpa.isPredefinida());
                    direccion.setCliente(requestG);
                    return direccion;
                })
                .collect(Collectors.toSet());

        requestG.setListDirecciones(direcciones);
        return super.save(requestG, result);
    }

    @GetMapping("buscarPorIdCliente/{idCliente}")
    public ResponseEntity<ResponseGeneric<Optional<Cliente>>> findByIdCliente(@PathVariable int idCliente) {
        return ResponseEntity.status(HttpStatus.OK).body(sGenerico.findClienteById(idCliente));
    }
}
