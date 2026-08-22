package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.AccesorioRamo;
import com.ventas.key.mis.productos.entity.CantidadFlorValida;
import com.ventas.key.mis.productos.entity.ColorFlor;
import com.ventas.key.mis.productos.entity.RamoArmado;
import com.ventas.key.mis.productos.entity.RamoArmadoAccesorio;
import com.ventas.key.mis.productos.entity.TipoFlor;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoArmadoAccesorioRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoArmadoAccesorioResponseDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoArmadoRequestDto;
import com.ventas.key.mis.productos.models.floreseternas.RamoArmadoResponseDto;
import com.ventas.key.mis.productos.repository.IAccesorioRamoRepository;
import com.ventas.key.mis.productos.repository.ICantidadFlorValidaRepository;
import com.ventas.key.mis.productos.repository.IColorFlorRepository;
import com.ventas.key.mis.productos.repository.IRamoArmadoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RamoArmadoServiceImpl {

    private final IRamoArmadoRepository iRamoArmadoRepository;
    private final IColorFlorRepository iColorFlorRepository;
    private final ICantidadFlorValidaRepository iCantidadFlorValidaRepository;
    private final IAccesorioRamoRepository iAccesorioRamoRepository;
    private final AccesorioRamoServiceImpl accesorioRamoService;
    private final ProductoSombraServiceImpl productoSombraService;

    public RamoArmadoServiceImpl(IRamoArmadoRepository iRamoArmadoRepository,
                                  IColorFlorRepository iColorFlorRepository,
                                  ICantidadFlorValidaRepository iCantidadFlorValidaRepository,
                                  IAccesorioRamoRepository iAccesorioRamoRepository,
                                  AccesorioRamoServiceImpl accesorioRamoService,
                                  ProductoSombraServiceImpl productoSombraService) {
        this.iRamoArmadoRepository = iRamoArmadoRepository;
        this.iColorFlorRepository = iColorFlorRepository;
        this.iCantidadFlorValidaRepository = iCantidadFlorValidaRepository;
        this.iAccesorioRamoRepository = iAccesorioRamoRepository;
        this.accesorioRamoService = accesorioRamoService;
        this.productoSombraService = productoSombraService;
    }

    @Transactional
    public RamoArmadoResponseDto crear(RamoArmadoRequestDto dto) {
        validarRequest(dto);
        RamoArmado ramo = new RamoArmado();
        ramo.setActivo(dto.getActivo() == null || dto.getActivo());
        aplicarDatos(ramo, dto);
        RamoArmado guardado = iRamoArmadoRepository.save(ramo);
        return toResponseDto(guardado);
    }

    @Transactional
    public RamoArmadoResponseDto editar(Integer id, RamoArmadoRequestDto dto) {
        validarRequest(dto);
        RamoArmado ramo = iRamoArmadoRepository.findByIdConAccesorios(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Ramo armado no encontrado: " + id));
        if (dto.getActivo() != null) {
            ramo.setActivo(dto.getActivo());
        }
        aplicarDatos(ramo, dto);
        RamoArmado guardado = iRamoArmadoRepository.save(ramo);
        return toResponseDto(guardado);
    }

    @Transactional
    public RamoArmadoResponseDto cambiarActivo(Integer id, boolean activo) {
        RamoArmado ramo = iRamoArmadoRepository.findById(id)
                .orElseThrow(() -> new ExceptionDataNotFound("Ramo armado no encontrado: " + id));
        ramo.setActivo(activo);
        return toResponseDto(iRamoArmadoRepository.save(ramo));
    }

    public PginaDto<List<RamoArmadoResponseDto>> listarAdmin(int pagina, int size) {
        Page<RamoArmado> page = iRamoArmadoRepository.findAllConAccesorios(PageRequest.of(pagina - 1, size));
        return construirPagina(pagina, page);
    }

    public PginaDto<List<RamoArmadoResponseDto>> listarActivos(int pagina, int size) {
        Page<RamoArmado> page = iRamoArmadoRepository.findActivosConAccesorios(PageRequest.of(pagina - 1, size));
        return construirPagina(pagina, page);
    }

    private PginaDto<List<RamoArmadoResponseDto>> construirPagina(int pagina, Page<RamoArmado> page) {
        PginaDto<List<RamoArmadoResponseDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(page.getContent().stream().map(this::toResponseDto).toList());
        return resultado;
    }

    // Arma precioFlores, la regla del papel (automatica segun el umbral configurado en el
    // accesorio) y la lista de accesorios (excluyendo el papel de esa lista cuando la regla ya
    // lo cubrio, para no cobrarlo dos veces).
    private void aplicarDatos(RamoArmado ramo, RamoArmadoRequestDto dto) {
        ColorFlor colorFlor = iColorFlorRepository.findById(dto.getColorFlorId())
                .orElseThrow(() -> new ExceptionDataNotFound("Color de flor no encontrado: " + dto.getColorFlorId()));
        TipoFlor tipoFlor = colorFlor.getTipoFlor();
        CantidadFlorValida cantidadValida = iCantidadFlorValidaRepository.findById(dto.getCantidadFlorValidaId())
                .orElseThrow(() -> new ExceptionDataNotFound("Cantidad valida no encontrada: " + dto.getCantidadFlorValidaId()));
        if (!cantidadValida.getTipoFlor().getId().equals(tipoFlor.getId())) {
            throw new RuntimeException("La cantidad valida seleccionada no pertenece a la especie del color elegido");
        }

        ramo.setNombre(dto.getNombre());
        ramo.setImagenUrl(dto.getImagenUrl());
        ramo.setColorFlor(colorFlor);
        ramo.setCantidadFlorValida(cantidadValida);

        double precioFlores = cantidadValida.getCantidad() * tipoFlor.getPrecioPorFlor();
        ramo.setPrecioFlores(precioFlores);

        // Mano de obra: monto que el dueno configuro para ESTE tamano de ramo, sumado directo al
        // total sin aparecer como accesorio -- el cliente no debe poder distinguir cuanto es
        // material y cuanto es trabajo (decision del dueno, ver CAMBIOS_FRONT.md).
        double precioManoDeObra = cantidadValida.getManoDeObra() != null ? cantidadValida.getManoDeObra() : 0;
        ramo.setPrecioManoDeObra(cantidadValida.getManoDeObra());

        Optional<AccesorioRamo> papel = accesorioRamoService.obtenerPapelAutomaticoSiAplica(cantidadValida.getCantidad());
        Integer papelAccesorioId = null;
        if (papel.isPresent()) {
            ramo.setPapelIncluido(true);
            ramo.setPrecioPapel(accesorioRamoService.calcularPrecioPapel(papel.get(), cantidadValida.getCantidad(), cantidadValida.getPliegos()));
            papelAccesorioId = papel.get().getId();
        } else {
            ramo.setPapelIncluido(false);
            ramo.setPrecioPapel(null);
        }

        List<RamoArmadoAccesorio> accesorios = new ArrayList<>();
        double subtotalAccesorios = 0;
        if (dto.getAccesorios() != null) {
            for (RamoArmadoAccesorioRequestDto a : dto.getAccesorios()) {
                if (papelAccesorioId != null && papelAccesorioId.equals(a.getAccesorioId())) {
                    continue; // ya cubierto por la regla del papel automatico, no duplicar
                }
                AccesorioRamo accesorio = iAccesorioRamoRepository.findById(a.getAccesorioId())
                        .orElseThrow(() -> new ExceptionDataNotFound("Accesorio no encontrado: " + a.getAccesorioId()));
                int cantidad = a.getCantidad() != null ? a.getCantidad() : 1;
                RamoArmadoAccesorio detalle = new RamoArmadoAccesorio();
                detalle.setRamoArmado(ramo);
                detalle.setAccesorio(accesorio);
                detalle.setCantidad(cantidad);
                accesorios.add(detalle);
                subtotalAccesorios += accesorio.getPrecio() * cantidad;
            }
        }
        if (ramo.getAccesorios() == null) {
            ramo.setAccesorios(accesorios);
        } else {
            ramo.getAccesorios().clear();
            ramo.getAccesorios().addAll(accesorios);
        }

        double precioPapel = Boolean.TRUE.equals(ramo.getPapelIncluido()) ? ramo.getPrecioPapel() : 0;
        ramo.setPrecioTotal(precioFlores + precioPapel + subtotalAccesorios + precioManoDeObra);

        sincronizarVarianteSombra(ramo);
    }

    // Crea (alta) o sincroniza (edicion) la variante "sombra" del ramo -- unicamente para poder
    // reusar el sistema de fotos de variantes ya existente (ver comentario en RamoArmado.variante).
    // A diferencia de ColorFlor/AccesorioRamo/FraseListonPredefinida, esta variante nunca se vende:
    // no se referencia en ninguna linea de pedido, no se descuenta stock real de ella.
    private void sincronizarVarianteSombra(RamoArmado ramo) {
        int stock = ProductoSombraServiceImpl.STOCK_SIN_CONTROL;
        if (ramo.getVariante() != null) {
            productoSombraService.sincronizar(ramo.getVariante(), ramo.getNombre(), ramo.getPrecioTotal(), 0.0, stock);
        } else {
            Variantes variante = productoSombraService.crear(ramo.getNombre(), ramo.getPrecioTotal(), 0.0, stock);
            ramo.setVariante(variante);
        }
    }

    private void validarRequest(RamoArmadoRequestDto dto) {
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new RuntimeException("El nombre del ramo es obligatorio");
        }
        if (dto.getColorFlorId() == null) {
            throw new RuntimeException("El color de la flor es obligatorio");
        }
        if (dto.getCantidadFlorValidaId() == null) {
            throw new RuntimeException("La cantidad de flores del ramo es obligatoria");
        }
    }

    private RamoArmadoResponseDto toResponseDto(RamoArmado ramo) {
        List<RamoArmadoAccesorioResponseDto> accesorios = ramo.getAccesorios() == null ? List.of()
                : ramo.getAccesorios().stream().map(this::toAccesorioResponseDto).toList();
        ColorFlor colorFlor = ramo.getColorFlor();
        TipoFlor tipoFlor = colorFlor.getTipoFlor();
        return new RamoArmadoResponseDto(
                ramo.getId(),
                ramo.getNombre(),
                ramo.getImagenUrl(),
                ramo.getVariante() != null ? ramo.getVariante().getId() : null,
                ramo.getVariante() != null ? ramo.getVariante().getProducto().getId() : null,
                tipoFlor.getId(),
                tipoFlor.getNombre(),
                colorFlor.getId(),
                colorFlor.getNombre(),
                colorFlor.getVariante() != null ? colorFlor.getVariante().getId() : null,
                ramo.getCantidadFlorValida().getCantidad(),
                ramo.getPrecioFlores(),
                ramo.getPapelIncluido(),
                ramo.getPrecioPapel(),
                papelVarianteId(ramo),
                pliegosPapel(ramo),
                precioUnitarioPapel(ramo),
                ramo.getPrecioManoDeObra(),
                accesorios,
                ramo.getPrecioTotal(),
                ramo.getActivo());
    }

    // El papel no se guarda como FK propio en RamoArmado (solo su precio, ya congelado al
    // guardar) -- para dar el varianteId con el que armar la linea de venta se re-consulta el
    // accesorio marcado "es papel" vigente. Es informativo, no afecta el precio ya calculado.
    private Integer papelVarianteId(RamoArmado ramo) {
        if (!Boolean.TRUE.equals(ramo.getPapelIncluido())) {
            return null;
        }
        return iAccesorioRamoRepository.findFirstByEsPapelTrueAndActivoTrue()
                .map(AccesorioRamo::getVariante)
                .map(v -> v != null ? v.getId() : null)
                .orElse(null);
    }

    // Igual que papelVarianteId: informativo, recalculado con la config vigente del accesorio
    // "es papel" (no queda congelado, a diferencia de precioPapel/precioTotal).
    private Integer pliegosPapel(RamoArmado ramo) {
        if (!Boolean.TRUE.equals(ramo.getPapelIncluido())) {
            return null;
        }
        CantidadFlorValida cantidadValida = ramo.getCantidadFlorValida();
        return iAccesorioRamoRepository.findFirstByEsPapelTrueAndActivoTrue()
                .map(papel -> accesorioRamoService.calcularPliegosPapel(papel, cantidadValida.getCantidad(), cantidadValida.getPliegos()))
                .orElse(null);
    }

    // Igual que pliegosPapel: informativo, recalculado en vivo. Es el precioUnitario exacto que
    // hay que mandar en la linea de savePedido (ver comentario en RamoArmadoResponseDto).
    private Double precioUnitarioPapel(RamoArmado ramo) {
        if (!Boolean.TRUE.equals(ramo.getPapelIncluido())) {
            return null;
        }
        return iAccesorioRamoRepository.findFirstByEsPapelTrueAndActivoTrue()
                .map(AccesorioRamo::getPrecio)
                .orElse(null);
    }

    private RamoArmadoAccesorioResponseDto toAccesorioResponseDto(RamoArmadoAccesorio detalle) {
        AccesorioRamo accesorio = detalle.getAccesorio();
        return new RamoArmadoAccesorioResponseDto(
                accesorio.getId(),
                accesorio.getNombre(),
                detalle.getCantidad(),
                accesorio.getPrecio(),
                accesorio.getPrecio() * detalle.getCantidad(),
                accesorio.getVariante() != null ? accesorio.getVariante().getId() : null);
    }
}
