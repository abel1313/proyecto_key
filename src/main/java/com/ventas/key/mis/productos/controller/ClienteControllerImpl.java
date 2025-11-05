package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Direccion;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.ClienteServiceImpl;
import com.ventas.key.mis.productos.service.UsuarioDetailsService;
import com.ventas.key.mis.productos.service.api.IClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("clientes")
public class ClienteControllerImpl extends AbstractController<
                                                            Cliente,
                                                            Optional<Cliente>,
                                                            List<Cliente>,
                                                            Integer,
                                                            PginaDto<List<Cliente>>,
                                                            ClienteServiceImpl
                                                            > {

    private final UsuarioDetailsService usuarioDetailsService;
    private final IClienteService iClienteService;
    public ClienteControllerImpl(ClienteServiceImpl sGenerico, UsuarioDetailsService usuarioDetailsService,
                                 final IClienteService iClienteService) {
        super(sGenerico);
        this.usuarioDetailsService = usuarioDetailsService;
        this.iClienteService = iClienteService;
    }

    @Override
    public ResponseEntity<ResponseGeneric<Cliente>> save(Cliente requestG, BindingResult result) {
        Optional<Usuario> usr = this.usuarioDetailsService.findById(requestG.getUsuario().getId().intValue());
        if(usr.isPresent()){
            requestG.setUsuario(usr.get());
            if( usr.get().getCliente()!= null && usr.get().getCliente().getId()!=null){
                requestG.setId(usr.get().getCliente().getId());
            }
        }
        Set<Direccion> setDirecciones = requestG.getListDirecciones().stream().map(mpa->{
            Direccion direccion = new Direccion();
            direccion.setCalle(mpa.getCalle());
            direccion.setColonia(mpa.getColonia());
            direccion.setMunicipio(mpa.getMunicipio());
            direccion.setReferencias(mpa.getReferencias());
            direccion.setCodigoPostal(mpa.getCodigoPostal());
            direccion.setPredefinida(mpa.isPredefinida());
            direccion.setCliente(requestG);
            return direccion;
        }).collect(Collectors.toSet());
        requestG.setListDirecciones(setDirecciones);
        return super.save(requestG, result);
    }


    @RequestMapping("buscarPorIdCliente/{idCliente}")
    public ResponseEntity<ResponseGeneric<Optional<Cliente>>> findByIdCliente(@PathVariable int idCliente) {
        return ResponseEntity.status(HttpStatus.OK).body(this.iClienteService.findClienteById(idCliente));
    }


    private static Cliente getCliente(Cliente requestG) {
        Cliente cliente = new Cliente();
        cliente.setApeidoMaterno(requestG.getApeidoMaterno());
        cliente.setApeidoPaterno(requestG.getApeidoPaterno());
        cliente.setSexo(requestG.getSexo());
        cliente.setCorreoElectronico(requestG.getCorreoElectronico());
        cliente.setFechaNacimiento(requestG.getFechaNacimiento());
        cliente.setNumeroTelefonico(requestG.getNumeroTelefonico());
        cliente.setSegundoNombre(requestG.getSegundoNombre() );
        cliente.setNombrePersona(requestG.getSegundoNombre());
        return cliente;
    }
}
