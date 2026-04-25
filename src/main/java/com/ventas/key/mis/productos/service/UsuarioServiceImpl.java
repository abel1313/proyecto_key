package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Permiso;
import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.mapper.UserDto;
import com.ventas.key.mis.productos.mapper.UserUpdate;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.BaseRepository;
import com.ventas.key.mis.productos.repository.IPermisoRepository;
import com.ventas.key.mis.productos.repository.IRolRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.service.api.IUsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl extends CrudAbstractServiceImpl<Usuario, List<Usuario>, Optional<Usuario>, Integer, PginaDto<List<Usuario>>>
        implements IUsuarioService {

    private final PasswordEncoder passwordEncoder;
    private final IUsuarioRepository usuarioRepository;
    private final IRolRepository rolRepository;
    private final IPermisoRepository permisoRepository;

    public UsuarioServiceImpl(BaseRepository<Usuario, Integer> repoGenerico, ErrorGenerico error,
                              IUsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder,
                              IRolRepository rolRepository,
                              IPermisoRepository permisoRepository) {
        super(repoGenerico, error);
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
    }

    @Override
    public PginaDto<List<UserDto>> findAllPage(int pagina, int size, String buscar) {
        Pageable pageable = PageRequest.of(pagina - 1, size);
        Page<UserDto> dataPaginacion;

        if (buscar.isEmpty()) {
            dataPaginacion = usuarioRepository.findAll(pageable).map(this::toUserDto);
        } else {
            dataPaginacion = usuarioRepository.findAllPage(buscar, pageable).map(this::toUserDto);
        }

        PginaDto<List<UserDto>> pginaDto = new PginaDto<>();
        pginaDto.setPagina(pagina);
        pginaDto.setTotalPaginas(dataPaginacion.getTotalPages());
        pginaDto.setTotalRegistros((int) dataPaginacion.getTotalElements());
        pginaDto.setT(dataPaginacion.getContent());
        return pginaDto;
    }

    private UserDto toUserDto(Usuario u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setEnabled(u.getEnabled());
        dto.setRol(u.getRoles().getNombreRol());
        dto.setPermisosExtra(u.getPermisosExtra().stream()
                .map(Permiso::getNombrePermiso)
                .collect(Collectors.toSet()));
        return dto;
    }

    @Override
    public UserUpdate updateUserDto(UserUpdate usuarioDto, int id) {
        Usuario existe = usuarioRepository.findById(id)
                .orElseThrow(() -> new ExceptionErrorInesperado("Usuario no encontrado"));
        existe.setPassword(passwordEncoder.encode(usuarioDto.getPassword()));
        existe.setEmail(usuarioDto.getEmail());
        existe.setUsername(usuarioDto.getUsername());
        existe.setEnabled(usuarioDto.isEnabled());
        usuarioRepository.save(existe);
        return new UserUpdate();
    }

    @Override
    public void eliminarUsuario(int id) {
        Usuario existe = usuarioRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("El usuario no existe"));
        existe.setEnabled(false);
        usuarioRepository.save(existe);
    }

    @Override
    public int existeClientePorIdUsuario(int idUsuario) {
        return usuarioRepository.existsUsuarioByClienteId(idUsuario);
    }

    @Transactional
    public UserDto cambiarRol(Integer usuarioId, Integer rolId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));
        Roles rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ExceptionDataNotFound("Rol no encontrado"));
        usuario.setRoles(rol);
        return toUserDto(usuarioRepository.save(usuario));
    }

    @Transactional
    public UserDto agregarPermisoExtra(Integer usuarioId, Integer permisoId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));
        Permiso permiso = permisoRepository.findById(permisoId)
                .orElseThrow(() -> new ExceptionDataNotFound("Permiso no encontrado"));
        usuario.getPermisosExtra().add(permiso);
        return toUserDto(usuarioRepository.save(usuario));
    }

    @Transactional
    public UserDto quitarPermisoExtra(Integer usuarioId, Integer permisoId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));
        usuario.getPermisosExtra().removeIf(p -> p.getId().equals(permisoId));
        return toUserDto(usuarioRepository.save(usuario));
    }

    public List<Roles> listarRoles() {
        return rolRepository.findAll();
    }

    public List<Permiso> listarPermisos() {
        return permisoRepository.findAll();
    }
}