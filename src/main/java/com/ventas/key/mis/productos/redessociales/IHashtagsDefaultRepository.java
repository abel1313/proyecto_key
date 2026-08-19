package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IHashtagsDefaultRepository extends BaseRepository<HashtagsDefault, Integer> {

    Optional<HashtagsDefault> findByRedSocial(String redSocial);

    List<HashtagsDefault> findAllByOrderByRedSocialAsc();
}
