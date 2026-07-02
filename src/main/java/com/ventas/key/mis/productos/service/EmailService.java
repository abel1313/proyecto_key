package com.ventas.key.mis.productos.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Envía un correo HTML al destinatario.
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarTicket(String destinatario, String asunto, String htmlContent) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("EmailService: destinatario vacío, correo no enviado");
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Correo enviado a: {}", destinatario);
            return true;
        } catch (MessagingException e) {
            log.error("Error enviando correo a {}: {}", destinatario, e.getMessage());
            return false;
        }
    }
}
