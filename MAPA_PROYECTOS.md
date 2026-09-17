# 📍 MAPA DE PROYECTOS — Arquitectura General

Este documento es la **fuente de verdad** para entender la arquitectura de los 3 proyectos principales.

---

## 🖥️ Proyecto 1: BACKEND (proyecto_key)

**Repositorio:** https://github.com/abel1313/proyecto_key  
**Puerto:** 9091  
**Rama de desarrollo:** `dev` → `qa` → `main`  
**BD:** `inventario_key_qa` (dev/qa) | `inventario_key` (main/prod)  

### Responsabilidad
Expone la **API REST** del sistema. Orquesta lógica de negocio, autenticación, permisos, y consume el microservicio de imágenes.

### Endpoints principales
```
POST   /auth/login                                    - acceso
POST   /auth/refresh                                  - renovar token
POST   /auth/logout                                   - cerrar sesión

GET    /v1/productos/obtenerProductos                - listado paginado
GET    /v1/productos/buscarNombreOrCodigoBarra       - búsqueda
POST   /v1/productos/save                            - crear producto
PUT    /v1/productos/update                          - actualizar

GET    /v1/variantes/buscar                          - búsqueda variantes
GET    /v1/variantes/porProducto/{id}               - variantes de producto
POST   /v1/variantes/guardarConImagenes             - crear con imágenes
GET    /v1/variantes/imagenes/{varianteId}         - imágenes variante

GET    /v1/clientes/search                           - búsqueda clientes
POST   /v1/clientes/save                             - crear cliente

POST   /chatbot/mensaje                              - mensaje al bot
GET    /admin/gestion-roles                          - listar roles
POST   /admin/gestion-roles                          - crear/actualizar rol
GET    /admin/configuracion-negocio                  - config del negocio
PUT    /admin/configuracion-negocio                  - actualizar config
```

### Dependencias internas
- **micro_imagenes** (vía HTTP a puerto 9096)
- **RabbitMQ** (para colas de procesamiento)
- **Redis** (cache)

---

## 🌐 Proyecto 2: FRONTEND (producto_venta_online)

**Repositorio:** https://github.com/abel1313/producto_venta_online  
**Framework:** Angular 17+  
**Rama de desarrollo:** `dev` → `qa` → `main`  

### Responsabilidad
**UI/UX** del sistema. Pantallas para cliente (tienda) y admin (gestión).

### Estructura
```
src/app/
├── tienda/                 - pantallas públicas (login, registro, compra)
├── menu-admin/             - panel de administración
├── shared/                 - componentes reutilizables
└── services/               - clientes HTTP para proyecto_key
```

### Servicios HTTP (todos apuntan a proyecto_key:9091)
```typescript
// En src/app/services/
- ProductoService
- ClienteService
- AuthService
- VarianteService
- ChatService
- AdminService
- ImagenService  // IMPORTANTE: llama a micro_imagenes para URLs
```

### Consumo de imágenes
**De micro_imagenes directamente:**
```html
<img src="http://localhost:9096/mis-productos/imagenes/file/{imagenId}">
<!-- En producción: https://imagen.ejemplo.com/mis-productos/imagenes/file/{id} -->
```

---

## 🖼️ Proyecto 3: MICROSERVICIO DE IMÁGENES (micro_imagenes)

**Repositorio:** https://github.com/abel1313/micro_imagenes  
**Puerto:** 9096  
**Rama de desarrollo:** `dev` → `qa` → `main`  
**BD:** Tablas compartidas `imagenes_copy`, `producto_imagen_copy` (base datos proyecto_key)  

### Responsabilidad
Gestión de almacenamiento de archivos en disco. **No contiene lógica de negocio**, solo operaciones CRUD de archivos.

### Endpoints
```
GET    /mis-productos/imagenes/file/{imagenId}           - imagen completa
GET    /mis-productos/imagenes/thumbnail/{imagenId}      - miniatura
GET    /mis-productos/producto-imagen/listar/{productoId} - lista por producto
POST   /mis-productos/producto-imagen/subir              - subir archivo
DELETE /mis-productos/producto-imagen/{imagenId}         - eliminar

GET    /mis-productos/variante-imagen/listar/{varianteId} - imágenes variante
```

### Integración
- **Consumidor:** proyecto_key + producto_venta_online (vía URLs)
- **BD:** Acceso directo a `inventario_key_qa` / `inventario_key`

---

## 🔄 Flujo de una solicitud completa

**Ejemplo: Usuario ve foto de un producto en la tienda**

```
1. producto_venta_online (frontend)
   ↓ GET /v1/variantes/buscar
   
2. proyecto_key (backend:9091)
   ├─ Consulta BD (inventario_key_qa)
   ├─ Obtiene IDs de imagen
   ├─ Construye response con:
   │  └─ urlImagen: "http://localhost:9096/mis-productos/imagenes/file/{id}"
   ↓ Devuelve JSON
   
3. producto_venta_online recibe:
   ├─ {
   │    id: 123,
   │    nombre: "Pantalón azul",
   │    imagenes: [
   │      { id: "abc123", 
   │        urlImagen: "http://localhost:9096/mis-productos/imagenes/file/abc123"
   │      }
   │    ]
   │  }
   ↓ Renderiza <img src="...">
   
4. El navegador hace GET a micro_imagenes:9096
   ↓
   
5. micro_imagenes (backend:9096)
   ├─ Consulta BD (inventario_key_qa)
   ├─ Verifica que imagen existe
   ├─ Lee archivo de disco (/ruta/imagenes/abc123.jpg)
   ↓ Envía bytes + Content-Type: image/jpeg
   
6. Navegador muestra la imagen ✅
```

---

## 📋 Cambios recientes documentados

### 2026-09-17
- ✅ Implementado `tieneEscritura` en backend (campo Submenu)
- ✅ Frontend: checkbox Editar controlado por `tieneEscritura`
- ✅ Migración SQL ejecutada en BD

---

## ⚠️ Reglas importantes

### URL de Imágenes
- **Local/dev:** `http://localhost:9096/mis-productos/imagenes/file/{id}`
- **Producción:** URL será apuntada a CDN/servidor de imágenes

### Endpoints versionados
- **v1:** Endpoints antiguos (algunos deprecados)
- **v2:** Endpoints más nuevos (algunos migrados a micro_imagenes)

### Flujo de cambios
```
código → dev → qa → main
           ↓     ↓
        QA database   BD producción
```

### Base de datos
| Rama   | BD usada |
|--------|----------|
| dev    | `inventario_key_qa` |
| qa     | `inventario_key_qa` |
| main   | `inventario_key`    |

---

## 🚨 Si no sabes a qué proyecto pertenece algo

1. ¿Es una pantalla/componente? → **producto_venta_online** (frontend)
2. ¿Es un endpoint REST (POST, GET, etc.)? → **proyecto_key** (backend)
3. ¿Es sobre guardar/servir archivos de imagen? → **micro_imagenes**

Si aún tienes dudas, **consulta este documento** o pregunta al equipo.
