package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface IPublicacionSocialRepository extends BaseRepository<PublicacionSocial, Integer> {

    Page<PublicacionSocial> findByVarianteIdOrderByIdDesc(Integer varianteId, Pageable pageable);
}
