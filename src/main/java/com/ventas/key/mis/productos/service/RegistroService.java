package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RegistroService {
    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registrarUsuario(String username, String rawPassword) {
        Usuario nuevo = new Usuario();
        nuevo.setUsername(username);
        nuevo.setPassword(passwordEncoder.encode(rawPassword)); // encriptación
        nuevo.setRol("ADMIN");
        nuevo.setEnabled(true);
        Optional<Usuario> existeAdmin = this.usuarioRepository.findByRol(nuevo.getRol());
        if (existeAdmin.isPresent()) {
            nuevo.setRol("USER");
        }
        usuarioRepository.save(nuevo);
    }
}
