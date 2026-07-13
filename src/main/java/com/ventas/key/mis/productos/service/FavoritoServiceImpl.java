package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.Utils.AuthenticationUtils;
import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.Favorito;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.exeption.ExceptionDataNotFound;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.VarianteResumenDto;
import com.ventas.key.mis.productos.repository.IFavoritoRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoritoServiceImpl {

    private final IFavoritoRepository iFavoritoRepository;
    private final IVarianteRepository iVarianteRepository;
    private final VarianteServiceImpl varianteServiceImpl;

    // El JWT autentica un Usuario, no un Cliente directamente (relacion 1 a 1, Usuario.cliente).
    // Si el usuario logueado todavia no tiene Cliente asociado (registro sin completar), no puede
    // usar favoritos todavia -- mismo caso que ya existe en otros flujos de "datosCompletos".
    private Cliente clienteActual() {
        Usuario usuario = AuthenticationUtils.currentUsuario();
        Cliente cliente = usuario.getCliente();
        if (cliente == null) {
            throw new RuntimeException("Tu cuenta todavia no tiene un perfil de cliente completo");
        }
        return cliente;
    }

    @Transactional
    public void agregar(Integer varianteId) {
        Cliente cliente = clienteActual();
        if (iFavoritoRepository.existsByCliente_IdAndVariante_Id(cliente.getId(), varianteId)) {
            return;
        }
        Variantes variante = iVarianteRepository.findById(varianteId)
                .orElseThrow(() -> new ExceptionDataNotFound("No existe la variante con id: " + varianteId));

        Favorito favorito = new Favorito();
        favorito.setCliente(cliente);
        favorito.setVariante(variante);
        favorito.setFechaAgregado(LocalDateTime.now());
        iFavoritoRepository.save(favorito);
    }

    @Transactional
    public void quitar(Integer varianteId) {
        Cliente cliente = clienteActual();
        iFavoritoRepository.deleteByCliente_IdAndVariante_Id(cliente.getId(), varianteId);
    }

    public PginaDto<List<VarianteResumenDto>> listar(int pagina, int size) {
        Cliente cliente = clienteActual();
        Page<Integer> page = iFavoritoRepository.findVarianteIdsByClienteId(cliente.getId(), PageRequest.of(pagina - 1, size));

        PginaDto<List<VarianteResumenDto>> resultado = new PginaDto<>();
        resultado.setPagina(pagina);
        resultado.setTotalPaginas(page.getTotalPages());
        resultado.setTotalRegistros((int) page.getTotalElements());
        resultado.setT(varianteServiceImpl.resumenPorIds(page.getContent()));
        return resultado;
    }

    public List<Integer> listarIds() {
        Cliente cliente = clienteActual();
        return iFavoritoRepository.findAllVarianteIdsByClienteId(cliente.getId());
    }
}
