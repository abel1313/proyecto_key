package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.AccionSubmenu;
import com.ventas.key.mis.productos.entity.Permiso;
import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.entity.Submenu;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.entity.UsuarioSubmenu;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionErrorInesperado;
import com.ventas.key.mis.productos.mapper.UserDto;
import com.ventas.key.mis.productos.mapper.UserUpdate;
import com.ventas.key.mis.productos.models.ActualizarMiPerfilRequestDto;
import com.ventas.key.mis.productos.models.CambioCorreoPendienteResponseDto;
import com.ventas.key.mis.productos.models.PermisosEfectivosDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.BaseRepository;
import com.ventas.key.mis.productos.repository.IPermisoRepository;
import com.ventas.key.mis.productos.repository.IRolRepository;
import com.ventas.key.mis.productos.repository.ISubmenuRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.repository.IUsuarioSubmenuRepository;
import com.ventas.key.mis.productos.service.api.IUsuarioService;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
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
    private final ISubmenuRepository submenuRepository;
    private final IUsuarioSubmenuRepository usuarioSubmenuRepository;
    private final UsuarioVerificacionService usuarioVerificacionService;
    private final SesionRefreshService sesionRefreshService;

    public UsuarioServiceImpl(BaseRepository<Usuario, Integer> repoGenerico, ErrorGenerico error,
                              IUsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder,
                              IRolRepository rolRepository,
                              IPermisoRepository permisoRepository,
                              ISubmenuRepository submenuRepository,
                              IUsuarioSubmenuRepository usuarioSubmenuRepository,
                              UsuarioVerificacionService usuarioVerificacionService,
                              SesionRefreshService sesionRefreshService) {
        super(repoGenerico, error);
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.submenuRepository = submenuRepository;
        this.usuarioSubmenuRepository = usuarioSubmenuRepository;
        this.usuarioVerificacionService = usuarioVerificacionService;
        this.sesionRefreshService = sesionRefreshService;
    }

    @Override
    public PginaDto<List<UserDto>> findAllPage(int pagina, int size, String buscar) {
        return findAllPage(pagina, size, buscar, true);
    }

    // activos=false -> lista los desactivados (soft-delete), para poder reactivarlos.
    public PginaDto<List<UserDto>> findAllPage(int pagina, int size, String buscar, boolean activos) {
        Pageable pageable = PageRequest.of(pagina - 1, size);
        Page<UserDto> dataPaginacion;

        if (buscar.isEmpty()) {
            dataPaginacion = (activos
                    ? usuarioRepository.findByEnabledTrue(pageable)
                    : usuarioRepository.findByEnabledFalse(pageable)
            ).map(this::toUserDto);
        } else {
            dataPaginacion = (activos
                    ? usuarioRepository.findAllPage(buscar, pageable)
                    : usuarioRepository.findAllPageInactivos(buscar, pageable)
            ).map(this::toUserDto);
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

    // No toca password ni email: password solo se cambia via resetearPasswordAleatoria() (admin)
    // o PasswordResetService.cambiarPassword() (self-service, valida la actual). El email solo se
    // cambia via UsuarioVerificacionService.solicitarCambioCorreo/confirmarCambioCorreo (requiere
    // validar un codigo antes de aplicarse) - nunca directo desde este update generico.
    @Override
    public UserUpdate updateUserDto(UserUpdate usuarioDto, int id) {
        Usuario existe = usuarioRepository.findById(id)
                .orElseThrow(() -> new ExceptionErrorInesperado("Usuario no encontrado"));
        existe.setUsername(usuarioDto.getUsername());
        existe.setEnabled(usuarioDto.isEnabled());
        usuarioRepository.save(existe);
        return new UserUpdate();
    }

    /**
     * El propio usuario logueado actualiza su username (nunca su password ni su email aqui - ver
     * comentario de updateUserDto). Identifica al usuario por el username del JWT
     * (authentication.getName()), nunca por un id que mande el body/path - evita que un usuario
     * edite la cuenta de otro.
     */
    @Override
    @Transactional
    public void actualizarMiPerfil(String usernameActual, ActualizarMiPerfilRequestDto request) {
        Usuario existe = usuarioRepository.findByUsername(usernameActual)
                .orElseThrow(() -> new ExceptionErrorInesperado("Usuario no encontrado"));
        existe.setUsername(request.getUsername());
        usuarioRepository.save(existe);
    }

    /** Admin: solicita el cambio de correo de OTRO usuario (por id) - manda el codigo al correo nuevo. */
    @Override
    public boolean solicitarCambioCorreo(Integer id, String correoNuevo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));
        return usuarioVerificacionService.solicitarCambioCorreo(usuario, correoNuevo);
    }

    /** Admin: confirma el codigo del cambio de correo de OTRO usuario (por id). */
    @Override
    public void confirmarCambioCorreo(Integer id, String codigo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));
        usuarioVerificacionService.confirmarCambioCorreo(usuario, codigo);
    }

    /** Admin: estado del cambio de correo pendiente de OTRO usuario (por id). */
    @Override
    public CambioCorreoPendienteResponseDto obtenerCambioCorreoPendiente(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));
        return usuarioVerificacionService.obtenerCambioCorreoPendiente(usuario);
    }

    // Sin 0/O/1/l/I para que sea mas facil de dictar por telefono sin confundir caracteres.
    private static final String CHARSET_PASSWORD_ALEATORIA = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LONGITUD_PASSWORD_ALEATORIA = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Genera una contrasena aleatoria nueva, la asigna al usuario y la devuelve (el admin se la pasa al usuario). */
    @Transactional
    public String resetearPasswordAleatoria(Integer id) {
        Usuario existe = usuarioRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));
        String nuevaPassword = generarPasswordAleatoria();
        existe.setPassword(passwordEncoder.encode(nuevaPassword));
        existe.setPasswordTemporal(true);
        existe.setPasswordActualizadoEn(LocalDateTime.now());
        usuarioRepository.save(existe);

        // El reseteo por admin suele ser la respuesta a una cuenta comprometida: si no se cierran
        // las sesiones, quien ya estaba dentro sigue con su refresh token vigente.
        sesionRefreshService.cerrarTodasLasSesiones(existe.getId());
        return nuevaPassword;
    }

    private String generarPasswordAleatoria() {
        StringBuilder sb = new StringBuilder(LONGITUD_PASSWORD_ALEATORIA);
        for (int i = 0; i < LONGITUD_PASSWORD_ALEATORIA; i++) {
            sb.append(CHARSET_PASSWORD_ALEATORIA.charAt(RANDOM.nextInt(CHARSET_PASSWORD_ALEATORIA.length())));
        }
        return sb.toString();
    }

    @Override
    public void eliminarUsuario(int id) {
        Usuario existe = usuarioRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("El usuario no existe"));
        existe.setEnabled(false);
        usuarioRepository.save(existe);
    }

    // Contraparte de eliminarUsuario -- reactiva a alguien a quien se le hizo soft-delete
    // (por accidente o porque volvió a hacer falta), sin tener que tocar la base a mano.
    @Transactional
    public UserDto activarUsuario(int id) {
        Usuario existe = usuarioRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("El usuario no existe"));
        existe.setEnabled(true);
        return toUserDto(usuarioRepository.save(existe));
    }

    @Override
    public Integer existeClientePorIdUsuario(Integer idUsuario) {
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

    // ── Excepciones de pantalla por usuario (usuario_submenu) ───────────────────
    // Ver PLAN_PERMISOS_PANTALLAS.md seccion 3 -- concedido=true suma una pantalla que el rol no
    // da, concedido=false quita una que el rol si daria, sin tocar el rol para nadie mas.

    @Transactional
    public UsuarioSubmenu agregarSubmenuUsuario(Integer usuarioId, Integer submenuId, boolean concedido) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));
        Submenu submenu = submenuRepository.findById(submenuId)
                .orElseThrow(() -> new ExceptionDataNotFound("Submenu no encontrado"));
        UsuarioSubmenu excepcion = usuarioSubmenuRepository.findByUsuarioIdAndSubmenuId(usuarioId, submenuId)
                .orElseGet(() -> {
                    UsuarioSubmenu nueva = new UsuarioSubmenu();
                    nueva.setUsuario(usuario);
                    nueva.setSubmenu(submenu);
                    return nueva;
                });
        excepcion.setConcedido(concedido);
        return usuarioSubmenuRepository.save(excepcion);
    }

    @Transactional
    public void quitarSubmenuUsuario(Integer usuarioId, Integer submenuId) {
        usuarioSubmenuRepository.findByUsuarioIdAndSubmenuId(usuarioId, submenuId)
                .ifPresent(usuarioSubmenuRepository::delete);
    }

    public List<UsuarioSubmenu> listarExcepcionesSubmenu(Integer usuarioId) {
        return usuarioSubmenuRepository.findByUsuarioId(usuarioId);
    }

    public Set<Submenu> submenusEfectivos(Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));
        Set<Submenu> efectivos = new HashSet<>(usuario.getRoles() != null
                ? usuario.getRoles().getSubmenus() : Set.of());
        List<UsuarioSubmenu> excepciones = usuarioSubmenuRepository.findByUsuarioId(usuarioId);
        excepciones.stream().filter(UsuarioSubmenu::getConcedido).forEach(e -> efectivos.add(e.getSubmenu()));
        excepciones.stream().filter(e -> !e.getConcedido())
                .forEach(e -> efectivos.removeIf(s -> s.getId().equals(e.getSubmenu().getId())));
        return efectivos;
    }

    // Fase 2 de permisos de accion (2026-08-27): de las pantallas efectivas de arriba, cuales
    // ademas dan ESCRIBIR (crear/editar/borrar). A diferencia de submenusEfectivos(), esto SOLO
    // sale del rol -- las excepciones por usuario (usuario_submenu) siguen siendo nada mas de
    // visibilidad, no tienen su propio nivel de accion todavia (no hay front para eso aun).
    public Set<Submenu> submenusEscritura(Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));
        return usuario.getRoles() != null ? usuario.getRoles().getSubmenusEscritura() : Set.of();
    }

    // Fase 3 de permisos (2026-08-27, piloto en Modelos): acciones puntuales dentro de una
    // pantalla (ej. "eliminar", "habilitar" en Modelos). Mismo alcance que submenusEscritura --
    // solo del rol, sin excepciones por usuario individual todavia.
    public Set<AccionSubmenu> accionesEfectivas(Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));
        return usuario.getRoles() != null ? usuario.getRoles().getAcciones() : Set.of();
    }

    /**
     * Junta pantallas + pantallasEscritura + acciones en UN SOLO fetch del usuario (encontrado
     * 2026-08-27: JwtAuthenticationFilter y AuthController.login/refresh llamaban a
     * submenusEfectivos + submenusEscritura + accionesEfectivas por separado, cada una haciendo
     * su propio usuarioRepository.findById() -- 3 fetches redundantes del MISMO usuario en cada
     * request autenticado, con Usuario.roles EAGER y Roles con 4 colecciones @ManyToMany EAGER
     * arrastrando varias queries cada vez. Esto era el causante de la lentitud reportada en
     * login y en cualquier pantalla que mandara el token, no solo en las nuevas). Usar este
     * metodo en vez de las 3 sueltas para cualquier caller que necesite las 3 juntas.
     */
    public PermisosEfectivosDto permisosEfectivos(Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ExceptionDataNotFound("Usuario no encontrado"));
        Set<Submenu> pantallas = new HashSet<>(usuario.getRoles() != null
                ? usuario.getRoles().getSubmenus() : Set.of());
        List<UsuarioSubmenu> excepciones = usuarioSubmenuRepository.findByUsuarioId(usuarioId);
        excepciones.stream().filter(UsuarioSubmenu::getConcedido).forEach(e -> pantallas.add(e.getSubmenu()));
        excepciones.stream().filter(e -> !e.getConcedido())
                .forEach(e -> pantallas.removeIf(s -> s.getId().equals(e.getSubmenu().getId())));
        Set<Submenu> pantallasEscritura = usuario.getRoles() != null ? usuario.getRoles().getSubmenusEscritura() : Set.of();
        Set<AccionSubmenu> acciones = usuario.getRoles() != null ? usuario.getRoles().getAcciones() : Set.of();
        return new PermisosEfectivosDto(pantallas, pantallasEscritura, acciones);
    }
}