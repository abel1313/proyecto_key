package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RegistroService {
    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Usuario registrarUsuario(String username, String rawPassword, String email) {
        // Verificar si el username ya existe
        if (usuarioRepository.existsByUsername(username)) {
            throw new RuntimeException("El nombre de usuario ya está en uso");
        }

        // Crear nuevo usuario
        Usuario nuevo = new Usuario();
        nuevo.setUsername(username);
        nuevo.setPassword(passwordEncoder.encode(rawPassword));
        nuevo.setEmail(email);
        nuevo.setEnabled(true);

        // Asignar rol: solo el primer usuario será ADMIN
        boolean yaExisteAdmin = usuarioRepository.existsByRol("ADMIN");
        nuevo.setRol(yaExisteAdmin ? "USER" : "ADMIN");

        // Guardar usuario y capturar errores
        try {
            return usuarioRepository.save(nuevo);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Error al guardar usuario: posible duplicado");
        }

    }
}
