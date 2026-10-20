# iarespaldo — qué hace el bot si el modelo de IA no contesta (2026-10-20)

Pedido del dueño: *"si no responde ChatGPT porque ya no tiene token, que se vaya a DeepSeek; si tengo otro, a otro."*

Todas las llamadas a la IA de los 4 canales (chat de la tienda, chat en vivo, Facebook, Instagram) pasan por
`ChatbotBase.llamarOpenAI()`. Ahí se prueban los modelos en orden con las reglas de este dominio.

## Reglas
| # | Regla |
|---|---|
| R1 | Orden: **OpenAI primero**, luego el respaldo (DeepSeek). El respaldo solo existe si tiene llave (`DEEPSEEK_API_KEY`); sin ella todo funciona como antes. |
| R2 | Se cambia de modelo si: llave mala o sin saldo (401, 402, 403, o 429 con "insufficient_quota"), límite por minuto (429), servidor caído (5xx), tiempo agotado, sin conexión o respuesta vacía. |
| R3 | **No** se cambia si el error es nuestro (400, 404, 422): otro modelo fallaría igual. |
| R4 | Un modelo que falló **descansa**: sin saldo/llave 10 min (`IA_PAUSA_MINUTOS`), límite o caída 1 min. Mientras descansa no se le espera: el mensaje va directo al siguiente. |
| R5 | El **último** modelo siempre se prueba, aunque esté descansando. Con un solo modelo (sin respaldo) no hay descanso: se prueba en cada mensaje, como antes. |
| R6 | El descanso es **compartido** entre los 4 canales. |
| R7 | Se avisa en el log (`ERROR`) **una sola vez** cuando un modelo entra a descansar, no por cada mensaje. |
| R8 | Si fallan todos: el error llega a quien llamó y todo sigue como hoy (el cliente lee "En un momento te atendemos" y pasa a una persona). |
| R9 | Nunca se escribe la llave ni el cuerpo del error en el log: el de OpenAI trae un pedazo de la llave. |
| R10 | El filtro de seguridad de `botredes` (P1) aplica a la respuesta de **cualquier** modelo. |

## Variables de entorno (Kubernetes, nunca en el repositorio)
| Variable | Para qué | Por defecto |
|---|---|---|
| `DEEPSEEK_API_KEY` | Llave del respaldo. **Sin ella no hay respaldo.** | vacío |
| `DEEPSEEK_URL` | Dirección de la API del respaldo | `https://api.deepseek.com` |
| `DEEPSEEK_MODEL` | Modelo del respaldo | `deepseek-chat` |
| `IA_PAUSA_MINUTOS` | Cuánto descansa un modelo sin saldo o con llave mala | `10` |

La dirección y el modelo de DeepSeek no se pudieron verificar contra su documentación al escribirlo (sin salida a
internet): **confirmarlos en la cuenta de DeepSeek** antes de activar el respaldo.

## Pendiente
- Correo al admin cuando un modelo entra a descansar (hoy solo queda en el log).
- Privacidad: los mensajes de los clientes viajan también al respaldo; mencionarlo en el Aviso de privacidad.

## Dónde está
`dominio/modelo`: `ProveedorIa`, `TipoDeFallo`, `PoliticaDeRespaldoIa`, `CircuitoDeProveedores` (Java puro, con
pruebas). El cableado HTTP está en `ChatbotBase`. Prueba de punta a punta con servidores falsos:
`ChatbotRespaldoIaTest`.
