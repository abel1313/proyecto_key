package com.ventas.key.hexagonal.pagotarjeta.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.pagotarjeta.dominio.puerto.salida.TransaccionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * [Hexagonal: Driven Adapter] [Clean: Interface Adapter]
 *
 * <p>Una transaccion de Spring por llamada. Si la accion lanza una RuntimeException se deshace todo.
 */
@Component
public class TransaccionSpringAdapter implements TransaccionPort {

    private final TransactionTemplate plantilla;

    public TransaccionSpringAdapter(PlatformTransactionManager manager) {
        this.plantilla = new TransactionTemplate(manager);
    }

    @Override
    public <T> T ejecutar(Supplier<T> accion) {
        return plantilla.execute(estado -> accion.get());
    }
}
