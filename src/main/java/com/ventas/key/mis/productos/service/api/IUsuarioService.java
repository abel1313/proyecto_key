package com.ventas.key.mis.productos.service.api;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.mapper.UserDto;
import com.ventas.key.mis.productos.mapper.UserUpdate;
import com.ventas.key.mis.productos.models.ICrud;
import com.ventas.key.mis.productos.models.PginaDto;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IUsuarioService extends ICrud<
        Usuario,
        List<Usuario>,
        Optional<Usuario>,
        Integer,
        PginaDto<List<Usuario>>> {

    PginaDto<List<UserDto>> findAllPage(int pagina, int size, String buscar);

    UserUpdate updateUserDto(UserUpdate usuarioDto, int tipoDato);
    void eliminarUsuario(int tipoDato);
}
