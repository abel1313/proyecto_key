package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Submenu;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.exeption.ExceptionOperacionNoPermitida;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.ISubmenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SubmenuServiceImpl extends CrudAbstractServiceImpl<
        Submenu,
        List<Submenu>,
        Optional<Submenu>,
        Integer,
        PginaDto<List<Submenu>>> {

    private final ISubmenuRepository iSubmenuRepository;

    public SubmenuServiceImpl(ISubmenuRepository repository, ErrorGenerico error) {
        super(repository, error);
        this.iSubmenuRepository = repository;
    }

    public List<Submenu> porMenu(Integer menuId) {
        return iSubmenuRepository.findByMenuId(menuId);
    }

    // La implementacion base (CrudAbstractServiceImpl.delete) no hace nada -- hay que
    // sobreescribirla para que si borre (mismo gotcha que LugarEntregaServiceImpl.delete).
    // Sin el chequeo de abajo se podia vaciar un grupo por completo borrando su unico submenu
    // (encontrado 2026-08-27: el usuario borro por accidente el ultimo submenu de un grupo
    // mientras limpiaba duplicados) -- MenuServiceImpl.delete ya protege que no quede el
    // catalogo entero en 0 Menus, esto es lo mismo un nivel mas abajo: que ningun Menu se quede
    // con 0 pantallas mientras siga existiendo. Los submenus sin grupo (menu=null, ej. Home,
    // Tienda) no aplican -- no pertenecen a ningun acordeon que se pueda vaciar.
    @Transactional
    @Override
    public Submenu delete(Integer id) throws Exception {
        Submenu submenu = iSubmenuRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Submenu no encontrado: " + id));
        if (submenu.getMenu() != null && iSubmenuRepository.countByMenuId(submenu.getMenu().getId()) <= 1) {
            throw new ExceptionOperacionNoPermitida(
                    "No se puede eliminar \"" + submenu.getNombre() + "\": es la única pantalla que le queda al "
                            + "grupo \"" + submenu.getMenu().getNombre() + "\". Agrega otra pantalla a ese grupo "
                            + "primero, o elimina el grupo completo si ya no lo quieres.");
        }
        iSubmenuRepository.delete(submenu);
        return submenu;
    }
}
