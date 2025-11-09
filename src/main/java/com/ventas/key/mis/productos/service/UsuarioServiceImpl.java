package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.handleExeption.GenericException;
import com.ventas.key.mis.productos.mapper.UserDto;
import com.ventas.key.mis.productos.mapper.UserUpdate;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.BaseRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.service.api.IUsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl extends CrudAbstractServiceImpl<Usuario, List<Usuario>, Optional<Usuario>, Integer, PginaDto<List<Usuario>>>
        implements IUsuarioService {

    private final PasswordEncoder passwordEncoder;
    private final IUsuarioRepository usuarioRepository;
    public UsuarioServiceImpl(BaseRepository<Usuario, Integer> repoGenerico, ErrorGenerico error,
                              final IUsuarioRepository usuarioRepository,
                              final PasswordEncoder passwordEncoder) {
        super(repoGenerico, error);
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PginaDto<List<UserDto>> findAllPage(int pagina, int size, String buscar) {
        PginaDto<List<UserDto>> pginaDto = new PginaDto<>();
        Pageable pageable = PageRequest.of(pagina - 1, size);
        Page<UserDto> dataPaginacion;
        if(buscar.isEmpty()){
            dataPaginacion = this.usuarioRepository.findAllPage(pageable);
        }else{
            dataPaginacion = this.usuarioRepository.findAllPage(pageable, buscar);
        }
        pginaDto.setPagina(pagina);
        pginaDto.setTotalPaginas(dataPaginacion.getTotalPages());
        pginaDto.setTotalRegistros((int) dataPaginacion.getTotalElements());
        pginaDto.setT(dataPaginacion.getContent());
        return pginaDto;
    }

    @Override
    public UserUpdate updateUserDto(UserUpdate usuarioDto, int tipoDato) {
        Usuario existe = this.usuarioRepository.findById(tipoDato).orElseThrow(()-> new GenericException(404, "Ocurrio un erro al restablecer la contrasena"));
        existe.setPassword(passwordEncoder.encode(usuarioDto.getPassword()));
        existe.setEmail(usuarioDto.getEmail());
        existe.setRol(usuarioDto.getRol().toUpperCase());
        existe.setUsername(usuarioDto.getUsername());
        existe.setEnabled(usuarioDto.isEnabled());
        this.usuarioRepository.save(existe);
        return new UserUpdate();
    }
    @Override
    public void eliminarUsuario(int tipoDato) {
        Usuario existe = this.usuarioRepository.findById(tipoDato).orElseThrow(()-> new GenericException(404, "El usuario no existe"));
        existe.setEnabled(false);
        this.usuarioRepository.save(existe);
    }
}
