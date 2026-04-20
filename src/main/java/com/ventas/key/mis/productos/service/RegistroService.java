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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class RegistroService {
    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IRolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario registrarUsuario(String username, String rawPassword, String email) throws Exception {
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
        Roles rolAdmin = new Roles();
        rolAdmin.setNombreRol("ROLE_USUARIO");
        // Asignar rol: solo el primer usuario será ADMIN
        Optional<Roles> yaExisteAdmin = rolRepository.findByNombreRol("ROLE_ADMIN");
        Roles rol = new Roles();
        Set<Roles> roles = new HashSet<>();
        rol.setNombreRol("ROLE_USUARIO");
        if(yaExisteAdmin.isEmpty()){
            rol.setNombreRol("ROLE_ADMIN");
            yaExisteAdmin = Optional.of(rolRepository.save(rol));
        }else{
            yaExisteAdmin = rolRepository.findByNombreRol(rol.getNombreRol());
            if(yaExisteAdmin.isEmpty()){
                yaExisteAdmin = Optional.of(rolRepository.save(rol));
            }
        }
        roles.add(yaExisteAdmin.orElseThrow(()-> new Exception("Ocurrio un error ")));
        nuevo.setRoles(yaExisteAdmin.orElseThrow(()-> new Exception("Ocurrio un error ")));


        // Guardar usuario y capturar errores
        try {
            return usuarioRepository.save(nuevo);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Error al guardar usuario: posible duplicado");
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

    }
}
