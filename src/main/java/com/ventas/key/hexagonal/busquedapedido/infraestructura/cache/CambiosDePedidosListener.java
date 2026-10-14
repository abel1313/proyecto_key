package com.ventas.key.hexagonal.busquedapedido.infraestructura.cache;

import com.ventas.key.mis.productos.entity.AbonoPedido;
import com.ventas.key.mis.productos.entity.Cliente;
import com.ventas.key.mis.productos.entity.ClienteSinRegistro;
import com.ventas.key.mis.productos.entity.DetallePedido;
import com.ventas.key.mis.productos.entity.GrupoPedido;
import com.ventas.key.mis.productos.entity.GrupoPedidoMiembro;
import com.ventas.key.mis.productos.entity.LugarEntrega;
import com.ventas.key.mis.productos.entity.Pedido;
import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.entity.RamoPedidoDetalle;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>R18: cuando se <b>confirma</b> en la base un cambio de algo que sale en una tarjeta de Mis pedidos,
 * sube la {@link VersionDeListasDePedidos version} de las listas. Hibernate lo avisa despues del commit y
 * antes de que el servicio regrese, asi que la pantalla que acaba de abonar, cancelar o unir ya pide su
 * lista con la version nueva. Cubre todo lo que se guarda por JPA (ventas, abonos, cancelaciones,
 * grupos, cambio de forma de cobro, editar articulos, cancelacion automatica, datos de prueba). Lo que
 * se escribe con SQL directo llama a {@link VersionDeListasDePedidos#subirAlConfirmar}.
 */
@Component
@RequiredArgsConstructor
public class CambiosDePedidosListener
        implements PostCommitInsertEventListener, PostCommitUpdateEventListener, PostCommitDeleteEventListener {

    /** Lo que dibuja o filtra la tarjeta (nombre del cliente, lugar, nombre del modelo, ramos...). */
    private static final Set<Class<?>> SE_VEN_EN_LA_LISTA = Set.of(
            Pedido.class, DetallePedido.class, AbonoPedido.class, GrupoPedido.class, GrupoPedidoMiembro.class,
            Cliente.class, ClienteSinRegistro.class, LugarEntrega.class, Producto.class, RamoPedidoDetalle.class);

    private final EntityManagerFactory emf;
    private final VersionDeListasDePedidos version;

    @PostConstruct
    void registrar() {
        if (!version.habilitado()) {
            return;
        }
        EventListenerRegistry registro = emf.unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry().getService(EventListenerRegistry.class);
        registro.appendListeners(EventType.POST_COMMIT_INSERT, this);
        registro.appendListeners(EventType.POST_COMMIT_UPDATE, this);
        registro.appendListeners(EventType.POST_COMMIT_DELETE, this);
    }

    private void cambio(Object entidad) {
        if (entidad != null && SE_VEN_EN_LA_LISTA.contains(entidad.getClass())) {
            version.subir(entidad.getClass().getSimpleName());
        }
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        cambio(event.getEntity());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        cambio(event.getEntity());
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        cambio(event.getEntity());
    }

    @Override
    public void onPostInsertCommitFailed(PostInsertEvent event) {
        // no se guardo: no hay nada que invalidar
    }

    @Override
    public void onPostUpdateCommitFailed(PostUpdateEvent event) {
    }

    @Override
    public void onPostDeleteCommitFailed(PostDeleteEvent event) {
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return SE_VEN_EN_LA_LISTA.contains(persister.getMappedClass());
    }
}
