package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.*;
import com.ventas.key.mis.productos.repository.BaseRepository;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.service.api.IImagenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ImagenServiceImpl extends CrudAbstractServiceImpl<
        Imagen,
        List<Imagen>,
        Optional<Imagen>,
        Long,
        PginaDto<List<Imagen>>> implements IImagenService {

    @Value("${guardar-imagenes.ruta_imagenes}")
    private String rutaImagenes;

    private final IImagenRepository iImagenRepository;


    public ImagenServiceImpl(BaseRepository<Imagen, Long> repoGenerico, ErrorGenerico error,
                             final IImagenRepository iImagenRepository) {
        super(repoGenerico, error);
        this.iImagenRepository = iImagenRepository;
    }


    @Override
    public List<Imagen> saveAll(List<Imagen> list) {
        return this.iImagenRepository.saveAll(list);
    }

    @Override
    public List<ImagenProductoResult> findIdsImagenesProducto(List<Integer> list) {
        return this.iImagenRepository.findImagenPrincipalPorProductoIds(list);
    }

    @Override
//    @Cacheable(value = "detalle", key = "'id:' + #id + ':page:' + #page + ':size:' + #size")
    public PageableDto<List<ImagenProductoBase64>> findImagenPrincipalPorProductoIds(Integer id, int page, int size) {


        Pageable pageable = PageRequest.of(page, size);
        log.info("detalle page {} size {}", page, size);
        PageableDto<List<ImagenProductoBase64>> pageableDto = new PageableDto<>();
        Page<ImagenProductoBase64> pageImgDto = this.iImagenRepository.findImagenPrincipalPorProductoIds(id, pageable)
                .map(mpa-> {
                    ImagenProductoBase64 imagenProductoBase64 = new ImagenProductoBase64();
                    imagenProductoBase64.setIdProducto(mpa.getIdProducto());
                    imagenProductoBase64.setIdImagen(mpa.getIdImagen());
                    imagenProductoBase64.setName(mpa.getName());
                    imagenProductoBase64.setPrice(mpa.getPrice());
                    imagenProductoBase64.setInventoryStatus(mpa.getInventoryStatus());
                    Optional<Imagen> imagenOpt = iImagenRepository.findById(mpa.getIdImagen());

                    if (imagenOpt.isPresent()){
                        Imagen imf = imagenOpt.get();
                        log.info("imf {}",imf.getBase64());
                        Path path = Paths.get(rutaImagenes, imf.getBase64());
                        try {
                            byte[] imagenBytes = Files.readAllBytes(path);
                            imagenProductoBase64.setImage(imagenBytes);
                        } catch (IOException e) {
                            log.info("No se encontro el archivo {0}", e.getCause());
                        }
                    }
                    return imagenProductoBase64;
                });
        log.info("imagenes paginadas {}", pageableDto);
        pageableDto.setList(pageImgDto.getContent());
        pageableDto.setTotalPaginas(pageImgDto.getTotalPages());
        return pageableDto;
    }

    @Override
    @CacheEvict(value = "detalleImagen", allEntries = true)
    public void deleteById(Long id) {
        this.iImagenRepository.deleteById(id);
    }

    @Override
    public com.ventas.key.mis.productos.hexagonal.dominio.Imagen findByIdImg(Integer id) throws IOException {
        Optional<ImagenProductoResult> imfRes = this.iImagenRepository.findImagenByImg(id);
        Optional<Imagen> img = this.iImagenRepository.findById(imfRes.isPresent() ? imfRes.get().getImagenId() : 0L);
        if (img.isPresent()) {
            Path path = Paths.get(rutaImagenes, img.get().getBase64());
            log.info("info {}",path);
            com.ventas.key.mis.productos.hexagonal.dominio.Imagen devolverImagen = new com.ventas.key.mis.productos.hexagonal.dominio.Imagen();
            devolverImagen.setImagen(Files.readAllBytes(path));
            devolverImagen.setContentType(img.get().getExtension());
            return devolverImagen;
        }
        throw new IOException("No se encontro el imagen");
    }
}
