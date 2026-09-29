# Dominio, DNS y correo — qué tengo y con quién

Objetivo: saber exactamente qué se paga, a quién, y qué depende de qué, **antes** de decidir si
el correo se cambia a otro proveedor más barato.

La idea a validar: **quedarse con el dominio y la VPS como están, y mover solo el correo.**

---

## 1. Lo que ya se sabe (sacado del código y de docs anteriores)

Son tres servicios distintos, aunque dos los venda la misma empresa:

| Pieza | Qué es | Dónde está | Se paga | Estado |
|---|---|---|---|---|
| **Dominio** `novedades-jade.com.mx` | El nombre | Hosting-Mexico | Anual | ✅ Renovado |
| **DNS** (la tabla de "este nombre → esta IP") | Configuración del dominio | **Dentro del hospedaje** (servidor `hapi`, ver 2.1) | Incluido en el hospedaje | ⚠️ Depende del hospedaje |
| **Hospedaje / alojamiento** (orden 609155) | Servidor compartido de Hosting-Mexico (`hapi.hosting-mexico.net`, 63.143.40.210) | Hosting-Mexico | Anual (el primer año venía en la compra) | ❌ Pendiente de pago |
| **VPS** | Donde corren tienda, backend e imágenes (Kubernetes) | IP `51.178.29.99` | Aparte | Funcionando |

**La tienda y el backend NO viven en Hosting-Mexico.** Todos estos subdominios apuntan a la VPS:

| Subdominio | Para qué |
|---|---|
| `shop.` / `qa.shop.` | Tienda (Angular) prod / QA |
| `backend.` / `qa.backend.` | Este backend prod / QA |
| `backend-imagenes.` / `qa.backend-imagenes.` | Micro de imágenes prod / QA |
| `front.` | Alias viejo de shop |

**Lo único que el código usa del hospedaje de Hosting-Mexico es el correo** (SMTP
`hapi.hosting-mexico.net:465`, en QA y en prod):

| Buzón | Para qué lo usa el sistema |
|---|---|
| `boutique.bolsas@` | Remitente de prod: verificación de cuenta, recuperar contraseña, tickets, pedidos |
| `qa.boutique.bolsas@` | Remitente de QA |
| `admin@` | Recibe los avisos del chat (`CHAT_ADMIN_EMAIL`) |
| `contacto@` | Aparece en Aviso de privacidad, Términos y Eliminar datos (lo revisan Meta y TikTok) |
| `ventas@`, `developers@` | Aparecen en algún lado; confirmar si existen y si se usan |

**Si el hospedaje vence sin mover el correo antes:** el registro de clientes nuevos se rompe (exige
verificar el correo), no llega "olvidé mi contraseña", no salen tickets, y `contacto@` rebota.

**La duda que decide todo:** si la zona DNS vive *dentro del hospedaje* (cPanel), al cancelarlo se
borra y también se caen la tienda y el backend. En ese caso, antes de cancelar hay que mover el DNS
(al panel del dominio o a Cloudflare gratis).

---

## 2. Qué revisar — en la VPS

Conectarse por SSH y pegar la salida de cada bloque debajo de él.

### 2.1 ¿Quién contesta el DNS del dominio? (la pregunta clave)
```bash
# Si falta dig: sudo apt install -y dnsutils
dig NS novedades-jade.com.mx +short
dig SOA novedades-jade.com.mx +short
```
Interpretación:
- Salen servidores tipo `ns1.hosting-mexico.net` / `hapi.hosting-mexico.net` → el DNS vive en Hosting-Mexico. Falta saber si en el hospedaje o en el dominio (ver sección 3).
- Salen `*.ns.cloudflare.com` → el DNS está en Cloudflare y el hospedaje no lo afecta.

**Resultado (2026-09-29):**
```
NS   ns1-hapi.hosting-mexico.net.
     ns2-hapi.hosting-mexico.net.
SOA  ns1-hapi.hosting-mexico.net. root.hapi.hosting-mexico.net. 2026081904 3600 1800 1209600 86400
MX   0 hapi.hosting-mexico.net.
TXT  "v=spf1 +a +mx +ip4:63.143.40.210 ~all"
```

**Conclusión: el DNS vive DENTRO del servidor del hospedaje (`hapi`), no aparte.**
- Los nameservers llevan el nombre del servidor del alojamiento (`ns1-hapi`/`ns2-hapi`), y el
  responsable de la zona es `root.hapi...`: es la zona DNS de la cuenta cPanel del hospedaje.
- **Si el hospedaje (orden 609155) se suspende o se cancela, lo más probable es que la zona se
  borre y dejen de resolver `shop.`, `backend.`, `backend-imagenes.` y los de QA → la tienda se
  cae**, aunque la VPS siga prendida y el dominio esté pagado. Confirmarlo con soporte.
- El número de serie `2026081904` = última modificación de la zona el 2026-08-19 (cuando soporte
  arregló el correo, ticket 551620).
- El correo entra por `hapi.hosting-mexico.net` (MX) y el SPF solo autoriza a la IP del hospedaje
  (`63.143.40.210`). Si se cambia de proveedor de correo, **MX y SPF tienen que cambiar**.

**Consecuencia para el plan:** no se puede "solo cambiar el correo y dejar vencer el hospedaje".
Antes hay que sacar el DNS del hospedaje: a la administración DNS del dominio en Hosting-Mexico
(si la ofrecen sin hospedaje) o a Cloudflare (gratis), cambiando los nameservers del dominio.

### 2.2 Todos los registros que existen hoy (respaldo antes de mover nada)
```bash
D=novedades-jade.com.mx
for s in "" www shop backend backend-imagenes qa.shop qa.backend qa.backend-imagenes front mail api webmail ftp cpanel; do
  n=${s:+$s.}$D; printf "%-45s A=%s  CNAME=%s\n" "$n" "$(dig +short A $n | tr '\n' ' ')" "$(dig +short CNAME $n | tr '\n' ' ')"
done
echo "--- MX";    dig +short MX $D
echo "--- TXT";   dig +short TXT $D
echo "--- DMARC"; dig +short TXT _dmarc.$D
echo "--- DKIM";  dig +short TXT default._domainkey.$D
echo "--- IP de esta VPS"; curl -s -4 ifconfig.me; echo
```
Qué buscar: qué apunta a la VPS (`51.178.29.99`) y qué apunta a Hosting-Mexico (`63.143.40.210`).
Los registros de MX, SPF (TXT con `v=spf1`), DKIM y DMARC son los que cambian si se cambia de
proveedor de correo.

```
PEGAR AQUÍ
```

### 2.3 Qué dominios sirve la VPS
```bash
sudo grep -rh "server_name" /etc/nginx/ 2>/dev/null | sort -u
sudo certbot certificates 2>/dev/null | grep -E "Certificate Name|Domains|Expiry"
kubectl get ingress -A
```
```
PEGAR AQUÍ
```

### 2.4 Qué correo usa cada ambiente (sin contraseñas)
```bash
for ns in default qa; do
  echo "== $ns"
  kubectl get deploy proyecto-key-deployment -n $ns -o yaml | grep -A4 -E "MAIL_USERNAME|CHAT_ADMIN_EMAIL|SPRING_MAIL_HOST"
done
# ¿Algún otro deployment manda correo?
kubectl get deploy -A -o yaml | grep -n -E "MAIL_USERNAME|SPRING_MAIL|smtp" | grep -vi password
```
Si sale `secretKeyRef`, ver solo el usuario (no la contraseña):
```bash
kubectl get secret NOMBRE_DEL_SECRET -n default -o jsonpath='{.data.MAIL_USERNAME}' | base64 -d; echo
```
```
PEGAR AQUÍ
```

### 2.5 ¿La VPS tiene servidor de correo propio? (debería ser que no)
```bash
sudo ss -ltnp | grep -E ':(25|465|587|993|995)\b' || echo "sin servidor de correo en la VPS"
nc -vz -w5 hapi.hosting-mexico.net 465
```
```
PEGAR AQUÍ
```

---

## 3. Qué revisar — en el panel de Hosting-Mexico (esto no está en la VPS)

- [ ] **Mis servicios:** lista de todo lo contratado, con precio de renovación y fecha de vencimiento de cada uno (dominio, hospedaje 609155, y cualquier otro).
- [ ] **Plan del hospedaje:** nombre del plan, cuántos buzones y cuánto espacio incluye.
- [ ] **Dominio → Nameservers:** qué nameservers tiene el dominio (debe coincidir con 2.1).
- [ ] **Dominio → Administración DNS:** ¿existe esa opción en el panel del *dominio* (fuera del cPanel)? Si sí, el DNS puede sobrevivir sin hospedaje.
- [ ] **cPanel → Cuentas de correo:** lista de buzones que existen y cuánto ocupa cada uno.
- [ ] **cPanel → Reenviadores:** si algún buzón reenvía a Gmail.
- [ ] **cPanel → Zona DNS:** exportar o tomar captura de todos los registros (respaldo).
- [ ] **cPanel → Sitios / archivos:** ¿hay alguna página publicada en el hospedaje (por ejemplo en `novedades-jade.com.mx` o `www.`)? Si no hay, el hospedaje solo sirve para el correo.

Pregunta para soporte (mismo ticket 086983):
> ¿El hospedaje de la orden 609155 es donde están mis cuentas de correo @novedades-jade.com.mx?
> Si no lo renuevo, ¿la zona DNS de mi dominio se conserva o también se borra? ¿Tienen un plan
> que sea solo de correo, y cuánto cuesta?

Respuesta:
```
PEGAR AQUÍ
```

---

## 4. Con qué comparar (llenar con precios reales)

| Opción | Costo/año | Buzones | Notas |
|---|---|---|---|
| Renovar hospedaje Hosting-Mexico (609155) | $ | | Cero cambios en el código |
| Plan solo de correo en Hosting-Mexico (si existe) | $ | | Cambios mínimos |
| Otro proveedor de correo con dominio propio | $ | | Hay que cambiar MX/SPF/DKIM y la config SMTP |

Cuentan de verdad dos necesidades distintas:
1. **Mandar correos automáticos** desde el backend (verificación, contraseña, tickets): necesita SMTP y buena entrega (SPF/DKIM).
2. **Recibir correos** en `contacto@` y `admin@`: basta con un buzón, o con un reenvío a Gmail.

---

## 5. Si se decide cambiar el correo — orden para no quedarse sin servicio

1. Confirmar dónde vive el DNS (2.1 y sección 3). Si vive en el hospedaje, **primero** mover el DNS (copiar todos los registros de 2.2) y esperar a que propague.
2. Crear los buzones en el proveedor nuevo (`boutique.bolsas@`, `qa.boutique.bolsas@`, `admin@`, `contacto@`).
3. Cambiar MX, SPF y DKIM al proveedor nuevo.
4. Cambiar en el backend `spring.mail.host`/`port` (`application-qa.yml`, `application-docker.yml`) y las variables `MAIL_USERNAME`/`MAIL_PASSWORD` en la VPS. Probar en QA: registro, olvidé mi contraseña, ticket.
5. Solo cuando todo funcione, dejar vencer el hospedaje.

---

## 6. ¿Se puede quedar el dominio y la VPS, y cambiar solo el correo? — Sí, en este orden

Decisión buscada: el dominio se queda, la VPS se queda igual (shop, backend, imágenes, QA), y solo
el correo se va a otro proveedor más barato. **Se puede**, y en la VPS no se toca nada.

El detalle es que hoy el DNS (el "directorio" que dice *shop → VPS*, *correo → Hosting-Mexico*)
viene **dentro del hospedaje** (ver 2.1). Si el hospedaje se deja vencer sin más, lo más probable es
que el directorio se borre y la tienda deje de encontrarse aunque la VPS esté perfecta.

Orden correcto:
1. Sacar el DNS del hospedaje (a la administración DNS del dominio si Hosting-Mexico la ofrece sin
   hospedaje, o a Cloudflare gratis): copiar los mismos registros de hoy y cambiar los nameservers
   del dominio. La tienda sigue igual porque apunta al mismo lugar.
2. Contratar el correo nuevo y crear los buzones (`boutique.bolsas@`, `qa.boutique.bolsas@`,
   `admin@`, `contacto@`).
3. En el DNS, cambiar solo los registros del correo (MX, SPF, DKIM) al proveedor nuevo.
4. En el backend, cambiar servidor SMTP y credenciales; probar en QA registro, "olvidé mi
   contraseña" y tickets.
5. Hasta entonces, dejar vencer el hospedaje.

Alternativa que puede evitar los pasos 1 y 3: que Hosting-Mexico tenga un **plan solo de correo**
más barato (se pregunta en la sección 7).

⚠️ **Mientras no esté terminado el paso 5, el hospedaje no se puede suspender**: de él dependen el
correo **y** la tienda. Si la fecha de corte llega antes, conviene pagar esta renovación y migrar
con calma.

---

## 7. Pregunta enviada a Hosting-Mexico (ticket 086983)

> Nota: el proveedor es **Hosting-Mexico** (hosting-mexico.net), no Hostinger — son empresas
> distintas. `VPS_AUDITORIA.md` dice "Hostinger" por error.

Texto a enviar:

> **Asunto:** Dar de baja el hospedaje (orden 609155) sin afectar mi dominio ni sus subdominios
>
> Hola,
>
> Sobre la orden de pago 609155 (renovación del hospedaje) del dominio **novedades-jade.com.mx**:
>
> Mi tienda y mi sistema no están en su hospedaje, están en un servidor propio (VPS) con IP
> **51.178.29.99**. En su zona DNS tengo registros que apuntan a esa IP, por ejemplo:
> shop.novedades-jade.com.mx, backend.novedades-jade.com.mx, backend-imagenes.novedades-jade.com.mx,
> qa.shop.novedades-jade.com.mx, qa.backend.novedades-jade.com.mx y qa.backend-imagenes.novedades-jade.com.mx.
>
> Del hospedaje solo uso las cuentas de correo @novedades-jade.com.mx. Mi dominio usa los
> nameservers **ns1-hapi.hosting-mexico.net** y **ns2-hapi.hosting-mexico.net**.
>
> 1. Si no renuevo el hospedaje (orden 609155), **¿se borra la zona DNS de mi dominio?**
>    ¿Dejarían de funcionar shop, backend y los demás subdominios aunque el dominio esté pagado?
> 2. ¿Pueden conservarme la administración DNS del dominio **sin** el hospedaje (solo con el
>    registro del dominio)? ¿Tiene algún costo?
> 3. ¿Tienen un plan **solo de correo** más económico? ¿Cuánto cuesta al año y cuántas cuentas incluye?
> 4. Si decido llevar el DNS a otro proveedor, ¿me pueden mandar la exportación completa de mi
>    zona DNS? ¿Hasta qué fecha puedo pagar la orden 609155 antes de que suspendan el servicio?
>
> Gracias.
> Abel Tiburcio Felipe

Cómo leer la respuesta:
- **"La zona DNS se conserva con el dominio"** → se puede dar de baja el hospedaje sin que se caiga
  nada de la VPS; solo hay que mover antes los correos.
- **"La zona DNS se borra"** → antes de dar de baja, mover el DNS (sección 6, paso 1).
- **Tienen plan solo de correo y sale barato** → puede ser lo más sencillo, sin mover nada.

Respuesta de Hosting-Mexico:
```
PEGAR AQUÍ
```

---

## 8. Pendientes de esta investigación

- [ ] Paso 2 en la VPS (sección 2.2): respaldo de todos los registros DNS.
- [ ] Respuesta de Hosting-Mexico (sección 7).
- [ ] Revisar el panel de Hosting-Mexico (sección 3): precios, vencimientos, buzones, exportar zona DNS.
- [ ] Comparar precios de correo (sección 4) y decidir.
