# Vigilancia mínima con Uptime Kuma (gratis, de preferencia en OTRO servidor distinto al VPS)
Revisiones sugeridas (cada 60 s, aviso por WhatsApp/Telegram/correo):
- `https://<back-prod>/mis-productos/actuator/health` debe responder 200.
- La tienda (página principal) debe responder 200 y contener el texto "Novedades".
- El micro de imágenes: una imagen conocida debe responder 200.
- Certificado TLS: avisar 14 días antes de vencer.
- Heartbeat (tipo "push") que el CronJob de respaldo llama al terminar bien: si no llega en 26 h, avisa.
