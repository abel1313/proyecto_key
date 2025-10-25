package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Imagen;
import com.ventas.key.mis.productos.entity.LotesProductos;
import com.ventas.key.mis.productos.entity.ProductoImagen;
import com.ventas.key.mis.productos.errores.ErrorGenerico;
import com.ventas.key.mis.productos.models.ImagenProductoDto;
import com.ventas.key.mis.productos.models.ImagenProductoResult;
import com.ventas.key.mis.productos.models.PageableDto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.repository.BaseRepository;
import com.ventas.key.mis.productos.repository.IImagenRepository;
import com.ventas.key.mis.productos.service.api.IImagenService;
import com.ventas.key.mis.productos.service.api.ILoteProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ImagenServiceImpl extends CrudAbstractServiceImpl<
        Imagen,
        List<Imagen>,
        Optional<Imagen>,
        Integer,
        PginaDto<List<Imagen>>> implements IImagenService {
    private final IImagenRepository iImagenRepository;


    public ImagenServiceImpl(BaseRepository<Imagen, Integer> repoGenerico, ErrorGenerico error,
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
    public PageableDto findImagenPrincipalPorProductoIds(Integer id, int page, int size) {


        Pageable pageable = PageRequest.of(page, size);
        List<ImagenProductoDto> prodIm = this.iImagenRepository.findImagenPrincipalPorProductoIds55(id);
        PageableDto pageableDto = new PageableDto();
        Page<ImagenProductoDto> pageImgDto = this.iImagenRepository.findImagenPrincipalPorProductoIds(id, pageable);
        pageableDto.setList(pageImgDto.getContent());
        pageableDto.setTotalPaginas(pageImgDto.getTotalPages());
        return pageableDto;
    }

    @Override
    public void deleteById(Integer id) {
        this.iImagenRepository.deleteById(id);
    }


    @Override
    public Optional<Imagen> findById(Integer id) {
        Optional<ImagenProductoResult> imfRes = this.iImagenRepository.findImagenByImg(id);
        if(imfRes.isPresent()) {
            log.info("info {}",1);
        }
        return this.iImagenRepository.findById(imfRes.isPresent() ? imfRes.get().getImagenId() : 0);
    }
}
