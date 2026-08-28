package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.AccionSubmenu;
import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.entity.Submenu;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionOperacionNoPermitida;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IAccionSubmenuRepository;
import com.ventas.key.mis.productos.repository.IRolRepository;
import com.ventas.key.mis.productos.repository.ISubmenuRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RolesServiceImpl extends CrudAbstractServiceImpl<
        Roles,
        List<Roles>,
        Optional<Roles>,
        Integer,
        PginaDto<List<Roles>>> {

    private static final String ROL_ADMIN = "ROLE_ADMIN";

    // Rutas que ROLE_ADMIN nunca puede perder: son las pantallas que asignan permisos.
    // Si se le quitan y no queda ningun otro rol con acceso, nadie puede volver a
    // dárselas -- el sistema queda sin forma de administrarse a si mismo.
    private static final Set<String> RUTAS_PROTEGIDAS_ADMIN = Set.of("gestion-menu", "gestion-menu/roles");

    private final IRolRepository rolRepository;
    private final ISubmenuRepository submenuRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IAccionSubmenuRepository accionSubmenuRepository;

    public RolesServiceImpl(IRolRepository repository, ErrorGenerico error, ISubmenuRepository submenuRepository,
                             IUsuarioRepository usuarioRepository, IAccionSubmenuRepository accionSubmenuRepository) {
        super(repository, error);
        this.rolRepository = repository;
        this.submenuRepository = submenuRepository;
        this.usuarioRepository = usuarioRepository;
        this.accionSubmenuRepository = accionSubmenuRepository;
    }

    @Transactional
    public Roles agregarSubmenu(Integer rolId, Integer submenuId) {
        Roles rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ExceptionDataNotFound("Rol no encontrado"));
        Submenu submenu = submenuRepository.findById(submenuId)
                .orElseThrow(() -> new ExceptionDataNotFound("Submenu no encontrado"));
        rol.getSubmenus().add(submenu);
        return rolRepository.save(rol);
    }

    @Transactional
    public Roles quitarSubmenu(Integer rolId, Integer submenuId) {
        Roles rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ExceptionDataNotFound("Rol no encontrado"));
        Submenu submenu = submenuRepository.findById(submenuId)
                .orElseThrow(() -> new ExceptionDataNotFound("Submenu no encontrado"));
        if (ROL_ADMIN.equals(rol.getNombreRol()) && RUTAS_PROTEGIDAS_ADMIN.contains(submenu.getRuta())) {
            throw new ExceptionOperacionNoPermitida(
                    "No se le puede quitar a ROLE_ADMIN el acceso a \"" + submenu.getNombre()
                            + "\" -- es la pantalla que asigna permisos, y sin ella nadie podria volver a dárselos.");
        }
        rol.getSubmenus().removeIf(s -> s.getId().equals(submenuId));
        // Sin ver la pantalla no tiene sentido poder escribir en ella, ni usar ninguna de sus
        // acciones puntuales -- se caen ambas en cascada para no dejar filas huerfanas en
        // rol_submenu_escritura / rol_accion (invariante que garantiza esta clase).
        rol.getSubmenusEscritura().removeIf(s -> s.getId().equals(submenuId));
        rol.getAcciones().removeIf(a -> a.getSubmenu().getId().equals(submenuId));
        return rolRepository.save(rol);
    }

    // ── Fase 2 de permisos de accion: quien puede ademas ESCRIBIR (no solo ver) ────────────

    @Transactional
    public Roles agregarSubmenuEscritura(Integer rolId, Integer submenuId) {
        Roles rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ExceptionDataNotFound("Rol no encontrado"));
        Submenu submenu = submenuRepository.findById(submenuId)
                .orElseThrow(() -> new ExceptionDataNotFound("Submenu no encontrado"));
        if (rol.getSubmenus().stream().noneMatch(s -> s.getId().equals(submenuId))) {
            throw new ExceptionOperacionNoPermitida(
                    "\"" + rol.getNombreRol() + "\" primero necesita poder VER \"" + submenu.getNombre()
                            + "\" antes de poder escribir en ella.");
        }
        rol.getSubmenusEscritura().add(submenu);
        return rolRepository.save(rol);
    }

    @Transactional
    public Roles quitarSubmenuEscritura(Integer rolId, Integer submenuId) {
        Roles rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ExceptionDataNotFound("Rol no encontrado"));
        Submenu submenu = submenuRepository.findById(submenuId)
                .orElseThrow(() -> new ExceptionDataNotFound("Submenu no encontrado"));
        if (ROL_ADMIN.equals(rol.getNombreRol()) && RUTAS_PROTEGIDAS_ADMIN.contains(submenu.getRuta())) {
            throw new ExceptionOperacionNoPermitida(
                    "No se le puede quitar a ROLE_ADMIN la escritura en \"" + submenu.getNombre()
                            + "\" -- es la pantalla que asigna permisos, y sin ella nadie podria volver a dárselos.");
        }
        rol.getSubmenusEscritura().removeIf(s -> s.getId().equals(submenuId));
        return rolRepository.save(rol);
    }

    // ── Fase 3 de permisos: acciones puntuales dentro de una pantalla (piloto en Modelos) ──

    @Transactional
    public Roles agregarAccion(Integer rolId, Integer accionId) {
        Roles rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ExceptionDataNotFound("Rol no encontrado"));
        AccionSubmenu accion = accionSubmenuRepository.findById(accionId)
                .orElseThrow(() -> new ExceptionDataNotFound("Accion no encontrada"));
        if (rol.getSubmenus().stream().noneMatch(s -> s.getId().equals(accion.getSubmenu().getId()))) {
            throw new ExceptionOperacionNoPermitida(
                    "\"" + rol.getNombreRol() + "\" primero necesita poder VER \"" + accion.getSubmenu().getNombre()
                            + "\" antes de poder usar la acción \"" + accion.getEtiqueta() + "\".");
        }
        rol.getAcciones().add(accion);
        return rolRepository.save(rol);
    }

    @Transactional
    public Roles quitarAccion(Integer rolId, Integer accionId) {
        Roles rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ExceptionDataNotFound("Rol no encontrado"));
        accionSubmenuRepository.findById(accionId)
                .orElseThrow(() -> new ExceptionDataNotFound("Accion no encontrada"));
        rol.getAcciones().removeIf(a -> a.getId().equals(accionId));
        return rolRepository.save(rol);
    }

    // La implementacion base (CrudAbstractServiceImpl.delete) no hace nada -- hay que
    // sobreescribirla para que si borre (mismo gotcha que LugarEntregaServiceImpl.delete).
    // Ademas ROLE_ADMIN nunca se puede borrar: es el unico rol con acceso garantizado a
    // la gestion de roles/menus, borrarlo deja el sistema sin forma de administrarse.
    @Transactional
    @Override
    public Roles delete(Integer id) throws Exception {
        Roles rol = rolRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Rol no encontrado: " + id));
        if (ROL_ADMIN.equals(rol.getNombreRol())) {
            throw new ExceptionOperacionNoPermitida("El rol ROLE_ADMIN no se puede eliminar.");
        }
        // Sin este chequeo se intentaba borrar el rol aunque hubiera usuarios con ese rol
        // asignado (Usuario.roles es @ManyToOne obligatorio) -- la unica razon de que no
        // rompiera nada hasta ahora es que la FK de la BD lo rechazaba con un 500 generico
        // en vez de un mensaje util, o -- si esa FK no existe en el ambiente -- dejaba a esos
        // usuarios con un rol_usuario huerfano incapaz de volver a iniciar sesion.
        long usuariosConEsteRol = usuarioRepository.countByRolesId(id);
        if (usuariosConEsteRol > 0) {
            throw new ExceptionOperacionNoPermitida(
                    "No se puede eliminar \"" + rol.getNombreRol() + "\": tiene " + usuariosConEsteRol
                            + " usuario(s) asignado(s). Cámbiales el rol primero.");
        }
        rolRepository.delete(rol);
        return rol;
    }
}
