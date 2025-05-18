package com.ventas.key.mis.productos.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ventas.key.mis.productos.entity.Rifa;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.service.RifaServiceImpl;
import com.ventas.key.mis.productos.service.api.IRifaService;

@RestController
@RequestMapping("rifa")
public class RifaControllerImpl extends AbstractController<
                                                            Rifa,
                                                            Optional<Rifa>,
                                                            List<Rifa>,
                                                            Integer,
                                                            PginaDto<List<Rifa>>,
                                                            RifaServiceImpl
                                                            > {


    private final IRifaService iRifaService;
    public RifaControllerImpl(RifaServiceImpl sGenerico) {
        super(sGenerico);
        this.iRifaService = sGenerico;
    }

    @GetMapping("getRifasPorHora")
    public ResponseEntity<List<Rifa>> getRifaPorHora(@RequestParam String inicio,
                                                    @RequestParam String fin, 
                                                    @RequestParam String palabraRifa) throws Exception{
        return ResponseEntity.status(HttpStatus.OK).body(this.iRifaService.buscarPorRangoDeHora(inicio,fin,palabraRifa));
    }

    @MessageMapping("/actualizar")  // Cuando el cliente envía datos
    @SendTo("/topic/ruleta")        // Se envían a todos los usuarios conectados
    public String enviarActualizacion(String mensaje) {
          System.out.println("📡 Mensaje recibido en WebSocket: " + mensaje);
        return mensaje; // Transmite el mensaje (puede ser una imagen, palabra, lista de usuarios, etc.)
    }


}
