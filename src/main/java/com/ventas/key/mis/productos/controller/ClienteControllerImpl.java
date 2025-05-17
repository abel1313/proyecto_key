package com.ventas.key.mis.productos.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.service.ClienteServiceImpl;

import lombok.RequiredArgsConstructor;

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

    public ClienteControllerImpl(ClienteServiceImpl sGenerico) {
        super(sGenerico);
    }

}
