package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.ProductoImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.VarianteImagen;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.hexagonal.dominio.mapper.RequestProductoImagen;
import com.ventas.key.mis.productos.hexagonal.infraestructura.ImagenProductoClienteAWS;
import com.ventas.key.mis.productos.models.ReconciliacionResultadoDto;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.repository.IProductoImagenRepository;
import com.ventas.key.mis.productos.repository.IProductosRepository;
import com.ventas.key.mis.productos.repository.IVarianteImagenRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliacionImagenService {

    @Value("${guardar-imagenes.ruta_imagenes}")
    private String rutaImagenes;

    private final IProductosRepository iProductosRepository;
    private final IVarianteRepository iVarianteRepository;
    private final IProductoImagenRepository iProductoImagenRepository;
    private final IVarianteImagenRepository iVarianteImagenRepository;
    private final IImagenRepository iImagenRepository;
    private final ImagenProductoClienteAWS imagenProductoClienteAWS;

    private volatile ReconciliacionResultadoDto ultimoResultado;
    private volatile boolean enProceso = false;

    @Async
    public void reconciliarTodos() {
        reconciliar(null);
    }

    @Async
    public void reconciliarProducto(Integer productoId) {
        reconciliar(productoId);
    }

    public boolean isEnProceso() {
        return enProceso;
    }

    private void reconciliar(Integer soloProductoId) {
        enProceso = true;
        ReconciliacionResultadoDto resultado = new ReconciliacionResultadoDto();

        if (soloProductoId != null) {
            procesarProducto(soloProductoId, resultado);
            resultado.setProductosRevisados(1);
            iVarianteRepository.findByProductoId(soloProductoId)
                    .forEach(v -> procesarVariante(v, resultado));
        } else {
            int page = 0;
            Page<Producto> pagina;
            do {
                pagina = iProductosRepository.findAll(PageRequest.of(page++, 50, Sort.by("id").ascending()));
                for (Producto p : pagina.getContent()) {
                    procesarProducto(p.getId(), resultado);
                    resultado.setProductosRevisados(resultado.getProductosRevisados() + 1);
                    iVarianteRepository.findByProductoId(p.getId())
                            .forEach(v -> procesarVariante(v, resultado));
                }
            } while (!pagina.isLast());
        }

        resultado.setEnProceso(false);
        ultimoResultado = resultado;
        enProceso = false;
        log.info("Reconciliacion completada: {} productos, {} variantes, {} reparados, {} faltantes en disco",
                resultado.getProductosRevisados(), resultado.getVariantesRevisadas(),
                resultado.getReparados().size(), resultado.getFaltantesEnDisco().size());
    }

    private void procesarProducto(Integer productoId, ReconciliacionResultadoDto resultado) {
        List<ProductoImagen> relaciones = iProductoImagenRepository.findByProductoId(productoId);
        if (relaciones.isEmpty()) return;

        List<RequestProductoImagen> aReenviar = new ArrayList<>();
        for (ProductoImagen rel : relaciones) {
            String nombreArchivo = rel.getImagen().getBase64();
            Path ruta = Paths.get(rutaImagenes, nombreArchivo);
            if (Files.exists(ruta)) {
                RequestProductoImagen req = new RequestProductoImagen();
                req.setProductoId(productoId);
                req.setImagenId(rel.getImagen().getId());
                aReenviar.add(req);
            } else {
                resultado.getFaltantesEnDisco().add("PRODUCTO id=" + productoId + " | " + nombreArchivo);
                log.warn("Archivo faltante en disco - PRODUCTO id={} archivo={}", productoId, nombreArchivo);
            }
        }

        if (!aReenviar.isEmpty()) {
            try {
                imagenProductoClienteAWS.saveAll(aReenviar);
                aReenviar.forEach(r -> resultado.getReparados().add(
                        "PRODUCTO id=" + productoId + " imagenId=" + r.getImagenId()));
                log.info("Re-enviadas {} imagenes del producto {}", aReenviar.size(), productoId);
            } catch (Exception e) {
                log.error("Error re-enviando imagenes del producto {}: {}", productoId, e.getMessage());
            }
        }
    }

    private void procesarVariante(Variantes variante, ReconciliacionResultadoDto resultado) {
        List<VarianteImagen> relaciones = iVarianteImagenRepository.findByVarianteId(variante.getId());
        if (relaciones.isEmpty()) return;

        List<RequestProductoImagen> aReenviar = new ArrayList<>();
        for (VarianteImagen rel : relaciones) {
            String nombreArchivo = rel.getImagen().getBase64();
            Path ruta = Paths.get(rutaImagenes, nombreArchivo);
            if (Files.exists(ruta)) {
                RequestProductoImagen req = new RequestProductoImagen();
                req.setProductoId(variante.getProducto().getId());
                req.setImagenId(rel.getImagen().getId());
                aReenviar.add(req);
            } else {
                resultado.getFaltantesEnDisco().add("VARIANTE id=" + variante.getId() + " | " + nombreArchivo);
                log.warn("Archivo faltante en disco - VARIANTE id={} archivo={}", variante.getId(), nombreArchivo);
            }
        }

        if (!aReenviar.isEmpty()) {
            try {
                imagenProductoClienteAWS.saveAll(aReenviar);
                aReenviar.forEach(r -> resultado.getReparados().add(
                        "VARIANTE id=" + variante.getId() + " imagenId=" + r.getImagenId()));
                resultado.setVariantesRevisadas(resultado.getVariantesRevisadas() + aReenviar.size());
                log.info("Re-enviadas {} imagenes de la variante {}", aReenviar.size(), variante.getId());
            } catch (Exception e) {
                log.error("Error re-enviando imagenes de la variante {}: {}", variante.getId(), e.getMessage());
            }
        }
    }

    public void limpiarDiscoDia() {
        ReconciliacionResultadoDto resultado = new ReconciliacionResultadoDto();

        Set<String> nombresValidos = Set.copyOf(iImagenRepository.findAllBase64());

        File directorio = new File(rutaImagenes);
        File[] archivos = directorio.listFiles();
        if (archivos == null) {
            log.warn("El directorio de imagenes no existe o esta vacio: {}", rutaImagenes);
            ultimoResultado = resultado;
            return;
        }

        Instant unaHoraAtras = Instant.now().minusSeconds(3600);
        int eliminados = 0;
        long bytes = 0;

        for (File archivo : archivos) {
            if (!archivo.isFile()) continue;
            try {
                BasicFileAttributes attr = Files.readAttributes(archivo.toPath(), BasicFileAttributes.class);
                if (attr.creationTime().toInstant().isAfter(unaHoraAtras)) continue;

                if (!nombresValidos.contains(archivo.getName())) {
                    bytes += archivo.length();
                    Files.delete(archivo.toPath());
                    eliminados++;
                    log.info("Archivo huerfano eliminado: {}", archivo.getName());
                }
            } catch (IOException e) {
                log.error("Error al procesar archivo {}: {}", archivo.getName(), e.getMessage());
            }
        }

        resultado.setArchivosEliminadosDisco(eliminados);
        resultado.setBytesLiberados(bytes);
        resultado.setEnProceso(false);
        log.info("Limpieza disco completada: {} archivos eliminados, {} bytes liberados", eliminados, bytes);
        ultimoResultado = resultado;
    }

    public ReconciliacionResultadoDto getUltimoResultado() {
        return ultimoResultado;
    }
}