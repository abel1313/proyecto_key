package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.entity.Submenu;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionOperacionNoPermitida;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IRolRepository;
import com.ventas.key.mis.productos.repository.ISubmenuRepository;
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

    public RolesServiceImpl(IRolRepository repository, ErrorGenerico error, ISubmenuRepository submenuRepository) {
        super(repository, error);
        this.rolRepository = repository;
        this.submenuRepository = submenuRepository;
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
        rolRepository.delete(rol);
        return rol;
    }
}
