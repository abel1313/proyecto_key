package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.ColorFlor;
import com.ventas.key.mis.productos.entity.TipoFlor;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.IColorFlorRepository;
import com.ventas.key.mis.productos.repository.ITipoFlorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ColorFlorServiceImpl extends CrudAbstractServiceImpl<
        ColorFlor,
        List<ColorFlor>,
        Optional<ColorFlor>,
        Integer,
        PginaDto<List<ColorFlor>>> {

    private final IColorFlorRepository iColorFlorRepository;
    private final ITipoFlorRepository iTipoFlorRepository;
    private final ProductoSombraServiceImpl productoSombraService;

    public ColorFlorServiceImpl(IColorFlorRepository repository, ErrorGenerico error,
                                 ITipoFlorRepository iTipoFlorRepository,
                                 ProductoSombraServiceImpl productoSombraService) {
        super(repository, error);
        this.iColorFlorRepository = repository;
        this.iTipoFlorRepository = iTipoFlorRepository;
        this.productoSombraService = productoSombraService;
    }

    // El precio/costo de la variante sombra viven en el TipoFlor (especie) del color, no en el
    // color mismo -- todos los colores de una misma especie cuestan igual. Antes de guardar,
    // crea (alta) o sincroniza (edicion) la variante sombra con ese precio y el color seteado.
    @Transactional
    @Override
    public ColorFlor save(ColorFlor req) {
        if (req.getTipoFlor() == null || req.getTipoFlor().getId() == null) {
            throw new RuntimeException("El tipo de flor (especie) es obligatorio");
        }
        TipoFlor tipoFlor = iTipoFlorRepository.findById(req.getTipoFlor().getId())
                .orElseThrow(() -> new ExceptionDataNotFound("Tipo de flor no encontrado: " + req.getTipoFlor().getId()));
        req.setTipoFlor(tipoFlor);

        int stock = req.getStock() != null ? req.getStock() : 0;
        String nombreVariante = tipoFlor.getNombre() + " - " + req.getNombre();
        if (req.getId() != null) {
            ColorFlor existente = iColorFlorRepository.findById(req.getId())
                    .orElseThrow(() -> new ExceptionDataNotFound("Color de flor no encontrado: " + req.getId()));
            Variantes variante = existente.getVariante();
            productoSombraService.sincronizar(variante, nombreVariante, tipoFlor.getPrecioPorFlor(), tipoFlor.getPrecioCosto(), stock, req.getNombre());
            req.setVariante(variante);
        } else {
            Variantes variante = productoSombraService.crear(nombreVariante, tipoFlor.getPrecioPorFlor(), tipoFlor.getPrecioCosto(), stock, req.getNombre());
            req.setVariante(variante);
        }
        return super.save(req);
    }

    // La implementacion base (CrudAbstractServiceImpl.delete) no hace nada -- hay que
    // sobreescribirla para que si borre, igual que LugarEntregaServiceImpl.delete.
    @Transactional
    @Override
    public ColorFlor delete(Integer id) throws Exception {
        ColorFlor color = iColorFlorRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Color de flor no encontrado: " + id));
        iColorFlorRepository.delete(color);
        return color;
    }

    public List<ColorFlor> listarPorTipoFlor(Integer tipoFlorId) {
        return iColorFlorRepository.findByTipoFlorIdAndActivoTrue(tipoFlorId);
    }
}
