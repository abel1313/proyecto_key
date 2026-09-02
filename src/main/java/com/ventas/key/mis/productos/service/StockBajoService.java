package com.ventas.key.mis.productos.service;

import com.ventas.key.mis.productos.entity.ConfiguracionNegocio;
import com.ventas.key.mis.productos.entity.Usuario;
import com.ventas.key.mis.productos.entity.productoVariantes.Variantes;
import com.ventas.key.mis.productos.repository.IUsuarioRepository;
import com.ventas.key.mis.productos.repository.IVarianteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Digest diario de stock bajo (StockBajoScheduler) -- a diferencia de la alerta de restock de
 * Favoritos (VarianteServiceImpl.notificarRestock, dispara por evento), esta es por barrido
 * periódico: más robusta contra los múltiples lugares donde el stock de una variante puede bajar
 * (pedidos, venta directa, abonos, rifas, ajuste manual de producto) sin tener que enganchar cada
 * uno, y de paso sirve de recordatorio mientras la variante siga baja, no solo la primera vez.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockBajoService {

    private final IVarianteRepository iVarianteRepository;
    private final IUsuarioRepository iUsuarioRepository;
    private final NegocioService negocioService;
    private final EmailService emailService;

    public void verificarYNotificar() {
        int umbral = negocioService.getConfig().getUmbralStockBajo() != null
                ? negocioService.getConfig().getUmbralStockBajo()
                : ConfiguracionNegocio.UMBRAL_DEFAULT_STOCK_BAJO;

        List<Variantes> bajas = iVarianteRepository.findConStockBajo(umbral);
        if (bajas.isEmpty()) {
            log.info("StockBajoService: sin variantes en o por debajo del umbral ({})", umbral);
            return;
        }

        List<Usuario> admins = iUsuarioRepository.findByRoles_NombreRolAndEnabledTrue("ROLE_ADMIN");
        if (admins.isEmpty()) {
            log.warn("StockBajoService: {} variantes bajas pero no hay ningun ADMIN activo a quien avisar", bajas.size());
            return;
        }

        List<String> lineas = bajas.stream().map(this::formatearLinea).collect(Collectors.toList());
        for (Usuario admin : admins) {
            if (admin.getEmail() == null || admin.getEmail().isBlank()) continue;
            emailService.enviarAlertaStockBajo(admin.getEmail(), lineas, umbral);
        }
        log.info("StockBajoService: {} variantes bajas, avisado a {} admin(es)", bajas.size(), admins.size());
    }

    private String formatearLinea(Variantes v) {
        StringBuilder sb = new StringBuilder();
        sb.append(v.getProducto() != null ? v.getProducto().getNombre() : "producto");
        if (v.getTalla() != null && !v.getTalla().isBlank()) sb.append(" · Talla ").append(v.getTalla());
        if (v.getColor() != null && !v.getColor().isBlank()) sb.append(" · ").append(v.getColor());
        sb.append(" — <strong>").append(v.getStock()).append(" en stock</strong>");
        return sb.toString();
    }
}
