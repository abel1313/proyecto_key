package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.entity.Submenu;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IRolRepository;
import com.ventas.key.mis.productos.repository.ISubmenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RolesServiceImpl extends CrudAbstractServiceImpl<
        Roles,
        List<Roles>,
        Optional<Roles>,
        Integer,
        PginaDto<List<Roles>>> {

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
        rol.getSubmenus().removeIf(s -> s.getId().equals(submenuId));
        return rolRepository.save(rol);
    }
}
