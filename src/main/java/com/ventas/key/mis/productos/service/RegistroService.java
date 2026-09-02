package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Roles;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.repository.IRolRepository;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RegistroService {

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IRolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario registrarUsuario(String username, String rawPassword, String email, boolean aceptoPrivacidad) throws Exception {
        if (usuarioRepository.existsByUsername(username)) {
            throw new RuntimeException("El nombre de usuario ya está en uso");
        }
        // Defensa en profundidad: RegistroRequest ya lo valida con @AssertTrue, pero este metodo
        // no depende de quien lo llame para garantizar que nunca se registre sin aceptar.
        if (!aceptoPrivacidad) {
            throw new RuntimeException("Debes aceptar el aviso de privacidad para registrarte");
        }

        Roles rol = rolRepository.findByNombreRol("ROLE_USUARIO")
                .orElseThrow(() -> new RuntimeException("ROLE_USUARIO no encontrado. Ejecutá el script de migración de permisos."));

        Usuario nuevo = new Usuario();
        nuevo.setUsername(username);
        nuevo.setPassword(passwordEncoder.encode(rawPassword));
        nuevo.setEmail(email);
        nuevo.setEnabled(true);
        nuevo.setRoles(rol);
        nuevo.setAceptoPrivacidad(true);
        nuevo.setFechaAceptoPrivacidad(LocalDateTime.now());

        try {
            return usuarioRepository.save(nuevo);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Error al guardar usuario: posible duplicado");
        }
    }
}