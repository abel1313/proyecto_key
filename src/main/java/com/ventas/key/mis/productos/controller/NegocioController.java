package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.dto.negocio.ContactosPublicosDto;
import com.ventas.key.mis.productos.dto.negocio.ContactosUpdateDto;
import com.ventas.key.mis.productos.dto.negocio.HorarioUpdateDto;
import com.ventas.key.mis.productos.dto.negocio.NegocioConfigDto;
import com.ventas.key.mis.productos.dto.negocio.NegocioEstadoDto;
import com.ventas.key.mis.productos.dto.negocio.RedSocialCreateDto;
import com.ventas.key.mis.productos.dto.negocio.RedSocialDto;
import com.ventas.key.mis.productos.dto.negocio.RedSocialUpdateDto;
import com.ventas.key.mis.productos.entity.ConfiguracionNegocio;
import com.ventas.key.mis.productos.entity.RedSocialNegocio;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.NegocioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/negocio")
@RequiredArgsConstructor
@Slf4j
public class NegocioController {

    private final NegocioService negocioService;

    /** Público — el frontend lo llama al cargar la página */
    @GetMapping("/estado")
    public ResponseEntity<ResponseGeneric<NegocioEstadoDto>> getEstado() {
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.getEstado()));
    }

    /** Público — datos de contacto para el QR del ticket (siempre disponibles, sin importar
     *  si el negocio está abierto o cerrado; a diferencia de /estado) */
    @GetMapping("/contactos")
    public ResponseEntity<ResponseGeneric<ContactosPublicosDto>> getContactosPublicos() {
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.getContactosPublicos()));
    }

    /** Solo ADMIN — ver configuración completa */
    @GetMapping("/config")
    public ResponseEntity<ResponseGeneric<NegocioConfigDto>> getConfig() {
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.getConfig()));
    }

    /** Solo ADMIN — abrir el negocio */
    @PostMapping("/abrir")
    public ResponseEntity<ResponseGeneric<ConfiguracionNegocio>> abrir() {
        log.info("Admin abrió el negocio");
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.abrir()));
    }

    /** Solo ADMIN — cerrar el negocio */
    @PostMapping("/cerrar")
    public ResponseEntity<ResponseGeneric<ConfiguracionNegocio>> cerrar() {
        log.info("Admin cerró el negocio");
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.cerrar()));
    }

    /** Solo ADMIN — guardar horario de apertura y cierre */
    @PutMapping("/horario")
    public ResponseEntity<ResponseGeneric<NegocioConfigDto>> actualizarHorario(
            @RequestBody HorarioUpdateDto dto) {
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.actualizarHorario(dto)));
    }

    /** Solo ADMIN — actualizar WhatsApp y/o Facebook */
    @PutMapping("/contactos")
    public ResponseEntity<ResponseGeneric<ConfiguracionNegocio>> actualizarContactos(
            @RequestBody ContactosUpdateDto dto) {
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.actualizarContactos(dto)));
    }

    /** Público — lista de redes sociales activas (nombre + url) para pintar en el front */
    @GetMapping("/redes-sociales/publico")
    public ResponseEntity<ResponseGeneric<List<RedSocialDto>>> getRedesSocialesPublico() {
        return ResponseEntity.ok(new ResponseGeneric<List<RedSocialDto>>(negocioService.listarRedesSocialesPublico()));
    }

    /** Solo ADMIN — lista completa de redes sociales (activas e inactivas) */
    @GetMapping("/redes-sociales")
    public ResponseEntity<ResponseGeneric<List<RedSocialNegocio>>> getRedesSociales() {
        return ResponseEntity.ok(new ResponseGeneric<List<RedSocialNegocio>>(negocioService.listarRedesSociales()));
    }

    /** Solo ADMIN — dar de alta una red social nueva */
    @PostMapping("/redes-sociales")
    public ResponseEntity<ResponseGeneric<RedSocialNegocio>> crearRedSocial(
            @RequestBody RedSocialCreateDto dto) {
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.crearRedSocial(dto)));
    }

    /** Solo ADMIN — actualizar nombre, url y/o activo de una red social existente */
    @PutMapping("/redes-sociales/{id}")
    public ResponseEntity<ResponseGeneric<RedSocialNegocio>> actualizarRedSocial(
            @PathVariable Integer id, @RequestBody RedSocialUpdateDto dto) {
        return ResponseEntity.ok(new ResponseGeneric<>(negocioService.actualizarRedSocial(id, dto)));
    }

    /** Solo ADMIN — eliminar una red social */
    @DeleteMapping("/redes-sociales/{id}")
    public ResponseEntity<ResponseGeneric<String>> eliminarRedSocial(@PathVariable Integer id) {
        negocioService.eliminarRedSocial(id);
        return ResponseEntity.ok(new ResponseGeneric<>("Red social eliminada correctamente"));
    }
}