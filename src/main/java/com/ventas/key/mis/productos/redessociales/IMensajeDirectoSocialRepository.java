package com.ventas.key.mis.productos.redessociales;

import com.ventas.key.mis.productos.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IMensajeDirectoSocialRepository extends BaseRepository<MensajeDirectoSocial, Integer> {

    Optional<MensajeDirectoSocial> findByMid(String mid);

    boolean existsByAutorId(String autorId);

    // Sirve para reconocer, cuando Instagram reenvia por webhook un mensaje "echo" (la propia
    // cuenta lo mando), si es un eco de nuestro propio bot (ya guardado aqui) o una respuesta
    // manual del admin desde la app real de Instagram -> ver
    // InstagramDirectMessageBotService.detectarRespuestaManualYPausar.
    boolean existsByRespuestaMid(String respuestaMid);
}
