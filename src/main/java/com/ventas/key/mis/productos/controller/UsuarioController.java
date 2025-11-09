package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.mapper.UserDto;
import com.ventas.key.mis.productos.mapper.UserUpdate;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ResponseGeneric;
import com.ventas.key.mis.productos.service.UsuarioServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("usuarios")
public class UsuarioController extends AbstractController<
        Usuario,
        Optional<Usuario>,
        List<Usuario>,
        Integer,
        PginaDto<List<Usuario>>,
        UsuarioServiceImpl
        > {


    private final UsuarioServiceImpl usu;
    public UsuarioController(UsuarioServiceImpl usuarioService) {
        super(usuarioService);
        this.usu = usuarioService;
    }

    @GetMapping("/getAllPage")
    public ResponseEntity<ResponseGeneric<PginaDto<List<UserDto>>>> findAllPage(@RequestParam String buscar,@RequestParam int page, @RequestParam int size) {
        try {
            PginaDto<List<UserDto>> listResponse  = this.usu.findAllPage(page, size, buscar);
            ResponseGeneric<PginaDto<List<UserDto>>> respo = new ResponseGeneric<>(listResponse);
            return ResponseEntity.status(HttpStatus.OK).body(respo);
        } catch (Exception e) {
            return null;
        }
    }

    @PutMapping("/updateUsuario/{tipoDato}")
    public ResponseEntity<ResponseGeneric<UserUpdate>> updateUsuario(@RequestBody UserUpdate usuarioDto, @PathVariable int tipoDato) {
        try {
            UserUpdate listResponse  = this.usu.updateUserDto(usuarioDto, tipoDato);
            ResponseGeneric<UserUpdate> respo = new ResponseGeneric<>(listResponse);
            return ResponseEntity.status(HttpStatus.OK).body(respo);
        } catch (Exception e) {
            return null;
        }
    }
    @DeleteMapping("/eliminarUsuarioDto/{tipoDato}")
    public ResponseEntity<Void> updateUsuario(@PathVariable int tipoDato) {
        try {
            this.usu.eliminarUsuario(tipoDato);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return null;
        }
    }
}
