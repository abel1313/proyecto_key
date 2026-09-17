# QA: Validación de Fix - Chat en Vivo No Promete Fotos

**Fecha:** 2026-09-17  
**Ticket:** Issue #9wb - Chat en vivo ofrece fotos que nunca llegan  
**Rama:** `dev` → `qa` (cambios ya mergeados)  
**Cambio principal:** Extracción de `seccionMostrarProductos()` en ChatbotBase + override en ChatbotChatVivoService  

---

## 📋 Resumen del Fix

**Problema:** El bot en chat en vivo le preguntaba al cliente "¿Quieres ver una foto?" pero esa pantalla no dibuja tarjetas ni imágenes. El cliente decía sí y se quedaba esperando una foto que nunca llegaba.

**Root cause:** El primer intento de fix solo quitó una línea del prompt, pero dejó:
- Ejemplos que mencionaban "¿Quieres ver una foto?" 
- Una REGLA CRÍTICA que mandaba mostrar imágenes siempre
- El modelo copiaba el ejemplo en lugar de obedecer la prohibición

**Solución:** 
- Extraer toda la sección "mostrar productos" a un método independiente
- ChatbotChatVivoService sobreescribe **completamente** la sección
- Nueva sección: "CÓMO HABLAR DE PRODUCTOS EN ESTE CHAT — SOLO TEXTO, NUNCA FOTOS"
- Los otros canales (sitio web, Facebook, Instagram) siguen sin cambios

---

## ✅ Test Cases - Chat en Vivo (CRÍTICOS)

### Test 1: Pregunta por producto → Bot NO ofrece foto
**Canal:** Chat en vivo  
**Entrada cliente:** "Hola, tendrás sort?"  
**Comportamiento esperado:**
- ✅ Bot responde con nombre del producto, presentación y precio en texto
- ✅ NO contiene: "¿Quieres ver una foto?"
- ✅ NO contiene: ##BUSCAR##
- ❌ Bot NO usa tarjetas (estilo site widget)

**Ejemplo esperado:**
```
"¡Sí! Tenemos el Jeans Short Especial (short chico) a $250 MXN 
y el Surprise SU8183 (short mezclilla) a $280 MXN 😊 ¿Te digo qué tallas hay?"
```

**Éxito:** Respuesta es puro texto, sin mención de fotos, sin intentar dibujar.

---

### Test 2: Cliente pide foto en chat en vivo → Bot ofrece alternativas
**Canal:** Chat en vivo  
**Entrada cliente:** "Me mandas foto?" / "Quiero ver imágenes"  
**Comportamiento esperado:**
- ✅ Bot dice que por aquí no se pueden ver fotos
- ✅ Ofrece alternativa 1: "tienda en línea" 
- ✅ Ofrece alternativa 2: "te paso con una persona" (##HUMANO##)
- ❌ Bot NO intenta usar ##BUSCAR##

**Ejemplo esperado:**
```
"Por aquí no puedo mandar fotos, pero lo puedes ver en la 
tienda en línea 😊 ¿O quieres que te pase con una persona que te las mande?"
```

**Éxito:** Bot no promete foto, da opciones válidas sin escalada si cliente elige tienda online.

---

### Test 3: Chat en vivo conserva sus features
**Comportamiento esperado:**
- ✅ Si cliente pregunta "Quiero hablar con una persona", bot escala con ##HUMANO##
- ✅ El catálogo actual sigue disponible en la conversación
- ✅ Typos se siguen interpretando (e.g., "sorth" → short)
- ✅ Admin puede escalar manualmente mensajes

**Éxito:** No se rompió nada más del canal mientras se quitaban las fotos.

---

### Test 4: Múltiples productos en una respuesta
**Entrada cliente:** "Tengo bolsas?" / "Qué tallas de shorts tienes?"  
**Comportamiento esperado:**
- ✅ Si hay varios productos que encajan, bot menciona VARIOS (no solo uno)
- ✅ Cada uno con nombre, presentación y precio
- ✅ Pregunta si quiere conocer más variantes

**Ejemplo esperado:**
```
"¡Sí! Tenemos la Bolsa Coach Mini a $450 MXN, la Bolsa Coach Grande a $650 MXN, 
y la Bolsa Coach Crossbody a $550 MXN 😊 ¿Cuál te interesa más?"
```

**Éxito:** Bot aprovecha el espacio de texto para ofrecer opciones, no las esconde tras tarjetas.

---

## 🔄 Test Cases - Regresión (Site Widget)

### Test 5: Site widget SÍ sigue mostrando tarjetas con fotos
**Canal:** Chat del sitio web (widget público)  
**Entrada cliente:** "Tendrás shorts?" / "Me muestras una bolsa"  
**Comportamiento esperado:**
- ✅ Bot responde CON "¿Quieres ver una foto?"
- ✅ Bot usa ##BUSCAR[término,0]##
- ✅ Tarjeta se dibuja en el widget
- ✅ Imagen aparece bajo la descripción

**Ejemplo esperado:**
```
"¡Sí! Tenemos shorts disponibles 😊 ¿Quieres ver una foto?
##BUSCAR[short,0]##"
```

**Éxito:** El widget sigue funcionando exactamente igual que antes del fix.

---

## 📊 Matriz de Validación

| Test | Chat en Vivo | Site Widget | Usuario | Ambiente | Estado |
|------|--------------|-------------|---------|----------|--------|
| 1    | ✓ Texto, sin foto | N/A | QA      | qa       | [ ] |
| 2    | ✓ Alternativas | N/A | QA      | qa       | [ ] |
| 3    | ✓ ##HUMANO## funciona | N/A | QA      | qa       | [ ] |
| 4    | ✓ Múltiples productos | N/A | QA      | qa       | [ ] |
| 5    | N/A | ✓ Fotos y ##BUSCAR## | QA | qa | [ ] |
| **Typo sorth→short** | ✓ Sigue funcionando | N/A | QA | qa | [ ] |

---

## 🧪 Cómo Probar

### Ambiente: QA
```
Base de datos: inventario_key_qa
Rama: qa
Endpoint: GET /chat/{conversationId}/mensaje
Body: { "mensaje": "tu pregunta aquí", "remitente": "USUARIO" }
```

### Canales disponibles para probar:
- **Chat en vivo** (MAIN): ChatbotChatVivoService.responder()
  - Usado por: Admin > Chat en vivo
  - Entrada: historial de ChatMensaje + mensaje nuevo
  
- **Site widget** (REGRESIÓN): ChatbotSitioWebService.chat()
  - Usado por: Widget público de login/tienda
  - Entrada: ChatbotRequest con historial y mensaje

---

## ✓ Criterios de Aceptación

**El fix está OK si:**
1. Chat en vivo NUNCA dice "¿Quieres ver una foto?" (Tests 1, 2)
2. Site widget SIGUE diciendo "¿Quieres ver una foto?" (Test 5)
3. Chat en vivo escala con ##HUMANO## si cliente lo pide (Test 3)
4. Typo handling sigue igual (sorth → short)
5. Catálogo actual sigue accesible en ambos canales

**El fix FALLA si:**
- Chat en vivo ofrece fotos en cualquier contexto
- Site widget dejó de mostrar fotos
- ##HUMANO## no funciona
- Typos se dejan de interpretar

---

## 📝 Pruebas Automáticas

**Archivo:** `src/test/java/com/ventas/key/mis/productos/chatbot/ChatbotPromptFotosTest.java`

**Tests que corren en CI/CD:**
```bash
mvn test -Dtest=ChatbotPromptFotosTest
```

**Resultado esperado:** 4/4 tests pasan ✅

- `elPromptDelChatEnVivoNoLeOfreceFotosAlCliente` 
- `elPromptDelChatEnVivoSiLeDiceQueHacerCuandoPidenFotos`
- `elChatEnVivoConservaLoSuyo`
- `elSitioWebSiSigueMostrandoTarjetas`

---

## 📞 Escalación

Si algo falla:
1. Captura screenshot o logs
2. Nota qué canal falló (chat en vivo / site widget)
3. Anota exactamente qué dijo el bot vs. qué esperabas
4. Reporta al backend si:
   - Chat en vivo sigue pidiendo fotos → falta override en seccionMostrarProductos()
   - Site widget dejó de mostrar fotos → check ChatbotBase.promptBase()
   - Typos no funcionan → check detectarCategoriaEnConversacion()
   - ##HUMANO## no funciona → check MARCA_HUMANO en ChatbotChatVivoService

---

## 📦 Archivos Afectados

- ✅ `ChatbotBase.java` — Método seccionMostrarProductos() extraído
- ✅ `ChatbotChatVivoService.java` — Override de seccionMostrarProductos()
- ✅ `ChatbotSitioWebService.java` — Sin cambios (usa base)
- ✅ `ChatbotPromptFotosTest.java` — Tests nuevos
- ✅ `CAMBIOS_FRONT.md` — Documentación actualizada

---

## 🔗 Referencias

- **Primer intento fallido (revertido):** Commit anterior, solo quitaba línea sin tocar ejemplos
- **Fix actual:** Método override de sección completa
- **Discusión:** Ver CAMBIOS_FRONT.md sección "Chat en vivo - FOTOS"
