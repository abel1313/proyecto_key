package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.PalabraClave;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IPalabraClaveRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PalabraClaveServiceImpl extends CrudAbstractServiceImpl<
        PalabraClave,
        List<PalabraClave>,
        Optional<PalabraClave>,
        Integer,
        PginaDto<List<PalabraClave>>> {

    private final IPalabraClaveRepository iPalabraClaveRepository;

    public PalabraClaveServiceImpl(IPalabraClaveRepository repository, ErrorGenerico error) {
        super(repository, error);
        this.iPalabraClaveRepository = repository;
    }

    @Cacheable(value = "palabrasClaveCache", key = "'buscar:' + #nombre + ':' + #pagina + ':' + #size")
    public PginaDto<List<PalabraClave>> buscarPorNombre(String nombre, int pagina, int size) {
        Page<PalabraClave> page = iPalabraClaveRepository
                .findByNombreContainingIgnoreCase(nombre, PageRequest.of(pagina - 1, size));
        PginaDto<List<PalabraClave>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent());
        return resultado;
    }

    @Override
    @CacheEvict(value = "palabrasClaveCache", allEntries = true)
    public PalabraClave save(PalabraClave req) {
        return super.save(req);
    }
}