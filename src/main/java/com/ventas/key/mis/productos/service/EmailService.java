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

    /**
     * Envía el código de verificación de correo (6 dígitos, expira en 15 minutos).
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarCodigoVerificacion(String destinatario, String codigo) {
        String asunto = "Verifica tu correo — Novedades Jade";
        String html = "<p>Tu código de verificación es:</p>"
                + "<h2>" + codigo + "</h2>"
                + "<p>Este código vence en 15 minutos. Si tú no solicitaste esta verificación, ignora este correo.</p>";
        return enviarTicket(destinatario, asunto, html);
    }

    /**
     * Envía el código para restablecer contraseña (6 dígitos, expira en 15 minutos).
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarCodigoResetPassword(String destinatario, String codigo) {
        String asunto = "Restablecer tu contraseña — Novedades Jade";
        String html = "<p>Tu código para restablecer la contraseña es:</p>"
                + "<h2>" + codigo + "</h2>"
                + "<p>Este código vence en 15 minutos. Si tú no solicitaste este cambio, ignora este correo — "
                + "tu contraseña actual sigue siendo válida.</p>";
        return enviarTicket(destinatario, asunto, html);
    }

    /**
     * Envía el código para que el cliente agregue a su cuenta una venta de mostrador hecha
     * con ClienteSinRegistro (caso: no se identificó en el momento de la compra). El texto
     * evita la palabra "reclamo" -- en español suena a queja, no a "esta compra es mía".
     * @return true si el envío fue exitoso, false si falló (no lanza excepción).
     */
    public boolean enviarCodigoReclamoVenta(String destinatario, String codigo) {
        String asunto = "Agrega tu compra a tu cuenta — Novedades Jade";
        String html = "<p>Gracias por tu compra. Para que quede asociada a tu cuenta, entra a la app, "
                + "inicia sesión y captura este código en la sección \"Agregar mi compra\":</p>"
                + "<h2>" + codigo + "</h2>"
                + "<p>Este código solo se puede usar una vez. Si tú no realizaste esta compra, ignora este correo.</p>";
        return enviarTicket(destinatario, asunto, html);
    }
}
