package com.ventas.key.mis.productos.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    private long id;
    private String username;
    private String email;
    private String rol;
    private Set<String> permisosExtra;
    private boolean enabled;
    // Solo lectura -- se acepta una vez en el registro, nunca se edita desde aqui. Para que el
    // admin pueda ver el estado de cualquier usuario en "Actualizar usuario" (pedido en QA
    // 2026-09-02, seguimiento del checkbox de privacidad agregado el mismo dia).
    private Boolean aceptoPrivacidad;
    private LocalDateTime fechaAceptoPrivacidad;
}