package com.ventas.key.mis.productos.models;

import com.ventas.key.mis.productos.entity.AccionSubmenu;
import com.ventas.key.mis.productos.entity.Submenu;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

// Encontrado 2026-08-27: JwtAuthenticationFilter y AuthController.login/refresh necesitaban las
// 3 cosas de aqui (pantallas, pantallasEscritura, acciones) juntas, y hasta ahora las pedian con
// 3 llamadas SEPARADAS (submenusEfectivos/submenusEscritura/accionesEfectivas), cada una
// haciendo su propio usuarioRepository.findById() -- 3 fetches redundantes del MISMO Usuario en
// cada request autenticado (Usuario.roles es EAGER y Roles tiene 4 colecciones @ManyToMany
// EAGER, asi que cada findById() ya arrastra varias queries por su cuenta). Esta clase junta el
// resultado de las 3 en un solo objeto para que UsuarioServiceImpl.permisosEfectivos() haga
// UN SOLO fetch del usuario y calcule las 3 de ahi -- tumba el problema de raiz en vez de
// optimizar cada metodo por separado.
@Getter
@AllArgsConstructor
public class PermisosEfectivosDto {
    private final Set<Submenu> pantallas;
    private final Set<Submenu> pantallasEscritura;
    private final Set<AccionSubmenu> acciones;
}
