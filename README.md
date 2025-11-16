server.servlet.context-path=/mis-productos
server.port=8081
spring.datasource.url=jdbc:mysql://167.88.164.132:3306/inventario_key
spring.datasource.username=noveda11_trece
spring.datasource.password=Luvianos*13

spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver


spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE






Recapitulación completa del montaje con Cloudflare Tunnel y Docker
Me alegra que haya quedado. Aquí va la secuencia completa —clara, ordenada y replicable— de lo que hiciste para que tu proyecto local funcione “como si fuera un servidor” detrás de Cloudflare.

Visión general del objetivo
Objetivo: Exponer tu backend local (Docker) a Internet usando un Cloudflare Tunnel con tu dominio api.novedades-jade.com.mx.

Modelo de tráfico: Cliente → Cloudflare (DNS + red) → Tunnel (cloudflared) → backend:8081.

Resultado: Tu servicio local responde bajo HTTPS con tu subdominio público, sin abrir puertos en tu red.

Prerrequisitos
Dominio en Cloudflare: DNS administrado por Cloudflare.

Docker y Docker Compose: Instalados en la máquina local.

Aplicación backend: Ejecutándose en el puerto 8081 dentro de Docker.

Cloudflared token: Generado al crear el túnel (modo conector remoto).

Pasos que seguiste
1. Creación del túnel en Cloudflare
Entraste al Dashboard: Zero Trust → Access → Tunnels.

Creaste un túnel: Nombre descriptivo, por ejemplo, “ventas-jade-docker”.

Elegiste el entorno: Seleccionaste “Docker” (conector remoto).

Obtuviste el token: Cloudflare te mostró un comando con un token largo (no .json).

Ese token autoriza al conector a correr el túnel sin archivo de credenciales.

2. Definición de la ruta publicada (Published Application Route)
Hostname público: Subdominio api en el dominio novedades-jade.com.mx.

Tipo de servicio: HTTP.

Destino interno (URL): http://backend:8081.

Aquí “backend” es el nombre del servicio/host de Docker Compose.

3. Configuración de cloudflared (sin credenciales .json)
Elegiste el flujo con token: Decidiste correr el conector directamente con el token, sin archivo .json.

Config.yml mínimo (ingress): Opcional, pero alineado con la ruta:

api.novedades-jade.com.mx → http://backend:8081.

Fallback http_status:404.

4. Docker Compose final
Servicios definidos:

Backend: Corre en 8081.

Nginx (opcional): Proxy en 80 si lo necesitas.

Cloudflared: Ejecuta el túnel con el token.

Comando clave en cloudflared:

command: tunnel --no-autoupdate run --token <TU_TOKEN>

Levantaste los contenedores: docker-compose up -d.

Verificaste logs: docker logs -f cloudflared mostró conexiones a mex01, dfw06, y la “ingress” aplicada.

5. Verificación de servicio
Prueba HTTP: Navegaste a https://api.novedades-jade.com.mx/....

Cabeceras de Cloudflare: Opcionalmente comprobaste Server: cloudflare / CF-Ray.

Logs del backend: Viste tus log.info(...) por cada petición en docker logs -f backend.

Cómo replicarlo en otra PC
1. Preparar entorno:

Instalar Docker y Docker Compose.

Clonar tu proyecto y ajustar variables si es necesario.

2. Reutilizar o crear túnel:

Opción A (reusar túnel): Ve al túnel existente en el Dashboard y genera un nuevo token de conector para esa PC.

Cada máquina que correrá un conector debe tener su propio token (seguro).

Opción B (crear túnel nuevo): Repite la creación con nombre nuevo y guarda el token.

3. Configurar Publish Route:

Mantener el mismo hostname: api.novedades-jade.com.mx apuntando a http://backend:8081.

Si usas múltiples máquinas, decide si solo una sirve tráfico o si vas a balancear.

4. Docker Compose en la nueva máquina:

Pegar el token en el servicio cloudflared:

command: tunnel --no-autoupdate run --token <TOKEN_DE_ESA_PC>

Asegurar nombre del servicio backend: Coincida con backend si usas esa URL interna.

5. Levantar y verificar:

docker-compose up -d.

docker logs -f cloudflared y docker logs -f backend.

Probar https://api.novedades-jade.com.mx.

Notas útiles y solución de problemas
Tokens vs .json: El flujo que usaste es con token (conector remoto). No necesitas .json.

Tunnel HEALTHY pero sin respuestas: Revisa que el backend esté arriba, el puerto correcto, y que la “ingress” apunte a http://backend:8081.

DNS en Cloudflare: El subdominio debe existir y estar proxied (naranja) si lo manejas por DNS tradicional; con Tunnel, la ruta publicada hace el enlace.

Logs detallados del back: Usa docker logs -f backend. Cloudflared no imprime cada request por defecto.

TLS/HTTPS: Cloudflare sirve HTTPS al cliente; tu backend puede seguir en HTTP interno (http://backend:8081).

Si quieres, te preparo un paquete “lista de archivos” (docker-compose.yml y config.yml mínimos) para que lo reutilices tal cual en otra máquina, con un espacio marcado para pegar el token.




