package com.ventas.key.mis.productos.controller;

import com.ventas.key.mis.productos.entity.Producto;
import com.ventas.key.mis.productos.models.PginaDto;
import com.ventas.key.mis.productos.models.ProductoDTO;
import com.ventas.key.mis.productos.models.ProductoDetalle;
import com.ventas.key.mis.productos.models.ProductoResumen;
import com.ventas.key.mis.productos.service.ProductosServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("productos")
@RequiredArgsConstructor
@Slf4j
public class ProductosControllerImpl {

    private final ProductosServiceImpl pServiceImpl;

    @GetMapping("obtenerProductos")
    public ResponseEntity<PginaDto<List<ProductoDTO>>> obtenerProductos(@RequestParam int size, @RequestParam int page) {
        log.info("obtener productos cahce size {} page {}", size, page);
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.getAll(size,page) );
    }

    @GetMapping("buscarNombreOrCodigoBarra")
    public ResponseEntity<PginaDto<List<ProductoDTO>>> buscarNombreOrCodigoBarra(@RequestParam int size, 
                                                                                @RequestParam int page,
                                                                                @RequestParam String nombre) {
        log.info("buscarNombreOrCodigoBarras {} page {} nombre {}", size, page, nombre);
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.findNombreOrCodigoBarra(size,page,nombre) );
    }

    @PostMapping("save")
    @CacheEvict(value = {"obtenerProductosCache","buscarNombreOrCodigoBarrasCache","findByIdCache","buscarImagenIdCache"}, allEntries = true)
    public ResponseEntity<Producto> save(@RequestBody ProductoDetalle producto) throws Exception{
        log.info("Inicia el guardado del producto {}", producto);
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.saveProductoLote(producto) );
    }

    @PutMapping("update")
    public ResponseEntity<Producto> update(@RequestBody ProductoDetalle producto) throws IOException {
        log.info("se actualizo el producto {}", producto);
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.saveProductoLote(producto));
    }

    @GetMapping("findById/{id}")
    public ResponseEntity<Optional<ProductoResumen>> update(@PathVariable int id){
        log.info("Se busca producto por ID {}",id);
        return ResponseEntity.status(HttpStatus.OK).body(this.pServiceImpl.getResumen(id) );
    }

    @DeleteMapping("deleteBy/{id}")
    public ResponseEntity<Optional<ProductoResumen>> eliminarProductoById(@PathVariable int id){
        log.info("Eliminar producto por ID {}",id);
        this.pServiceImpl.deleteByIdProducto(id);
        log.info("Se elimino el producto con el ID {}",id);
        return ResponseEntity.noContent().build();
    }

}
