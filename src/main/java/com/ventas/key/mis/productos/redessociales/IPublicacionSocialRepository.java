package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IPublicacionSocialRepository extends BaseRepository<PublicacionSocial, Integer> {

    Page<PublicacionSocial> findByVarianteIdOrderByIdDesc(Integer varianteId, Pageable pageable);

    List<PublicacionSocial> findByEstadoAndScheduledPublishTimeLessThanEqual(String estado, LocalDateTime hasta);
}
