package com.ventas.key.hexagonal.datosprueba.infraestructura.salida.persistencia;

import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ArticuloDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ArticuloGuardado;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.CatalogoAleatorio;
import com.ventas.key.hexagonal.datosprueba.dominio.modelo.ModeloDePrueba;
import com.ventas.key.hexagonal.datosprueba.dominio.puerto.salida.CatalogoPruebaPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [Hexagonal: Driven Adapter] [Clean: Frameworks & Drivers]
 *
 * <p>Modelos y articulos de prueba por JDBC en lotes: con JPA, 20 mil modelos y 50 mil articulos
 * son 70 mil INSERT uno por uno con su SELECT de vuelta. Las columnas son las mismas que
 * {@code datos_prueba_qa_catalogo.sql}, que ya se probo contra el esquema real de QA (incluidos
 * los NOT NULL de {@code producto} que la entidad no tiene, ver ESPECIFICACIONES_AMBIENTES.md).
 *
 * <p>El SQL evita {@code UPDATE ... JOIN} y {@code DELETE x FROM ... JOIN} (solo MySQL) para que
 * la misma clase corra en la prueba con H2.
 */
@Component
@RequiredArgsConstructor
public class CatalogoPruebaJdbcAdapter implements CatalogoPruebaPort {

    /** R4: los 13 caracteres de un codigo de prueba de este generador. */
    private static final String PATRON_CODIGO = CatalogoAleatorio.PREFIJO_CODIGO + "_________";
    private static final int MAX_IMAGENES_A_REUSAR = 200;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final TransactionTemplate transaccion;

    @Override
    public long siguienteNumero() {
        String mayor = jdbc.queryForObject(
                "SELECT MAX(codigo_barras) FROM codigo_barras WHERE codigo_barras LIKE ?", String.class, PATRON_CODIGO);
        return mayor == null ? 1 : Long.parseLong(mayor.substring(CatalogoAleatorio.PREFIJO_CODIGO.length())) + 1;
    }

    @Override
    public void asegurarCategorias(List<String> categorias) {
        for (String nombre : categorias) {
            Integer hay = jdbc.queryForObject("SELECT COUNT(*) FROM palabra_clave WHERE nombre = ?", Integer.class, nombre);
            if (hay == null || hay == 0) {
                jdbc.update("INSERT INTO palabra_clave (nombre) VALUES (?)", nombre);
            }
        }
    }

    @Override
    public int imagenesDisponibles() {
        return imagenesAReusar().size();
    }

    /** R7: imagenes de articulos reales habilitados (nunca de los de prueba), como el script SQL. */
    private List<Long> imagenesAReusar() {
        return jdbc.queryForList("""
                SELECT DISTINCT vi.imagen_id
                FROM variante_imagen vi
                JOIN variantes v ON v.id = vi.variante_id
                JOIN producto p ON p.id = v.producto_id
                LEFT JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
                WHERE vi.imagen_id IS NOT NULL AND v.habilitado = '1' AND p.habilitado = '1'
                  AND (cb.codigo_barras IS NULL OR (cb.codigo_barras NOT LIKE '2099%%' AND cb.codigo_barras NOT LIKE '2098%%'))
                ORDER BY vi.imagen_id
                LIMIT %d""".formatted(MAX_IMAGENES_A_REUSAR), Long.class);
    }

    @Override
    public List<ArticuloGuardado> guardarLote(List<ModeloDePrueba> modelos) {
        if (modelos.isEmpty()) {
            return List.of();
        }
        List<Long> imagenes = imagenesAReusar();
        Map<String, Integer> categorias = categoriasPorNombre();
        return transaccion.execute(estado -> guardar(modelos, imagenes, categorias));
    }

    private List<ArticuloGuardado> guardar(List<ModeloDePrueba> modelos, List<Long> imagenes,
                                           Map<String, Integer> categorias) {
        // 1. Codigos de barras
        jdbc.batchUpdate("INSERT INTO codigo_barras (codigo_barras) VALUES (?)",
                modelos.stream().map(m -> new Object[]{m.codigoBarras()}).toList());
        Map<String, Integer> codigoId = porCodigo(
                "SELECT id, codigo_barras FROM codigo_barras WHERE codigo_barras IN (:codigos)", modelos);

        // 2. Modelos (producto). piezas = stock, como el script; rebaja = precio si no hay rebaja.
        jdbc.batchUpdate("""
                INSERT INTO producto (nombre, descripcion, color, marca, precio_costo, precio_venta, precio_rebaja,
                                      piezas, stock, habilitado, es_catalogo_interno, codigo_barras_generado,
                                      codigo_barras_id, palabra_clave_id, fecha_creacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, '1', 0, 0, ?, ?, NOW())""",
                modelos.stream().map(m -> new Object[]{
                        m.nombre(), m.descripcion(), m.color(), ModeloDePrueba.MARCA,
                        m.precioCosto(), m.precioVenta(), m.precioRebaja(), m.stock(), m.stock(),
                        codigoId.get(m.codigoBarras()), categorias.get(m.categoria())}).toList());
        Map<String, Integer> productoId = porCodigo("""
                SELECT p.id, cb.codigo_barras FROM producto p
                JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
                WHERE cb.codigo_barras IN (:codigos)""", modelos);

        // 3. Articulos (variantes). precio NULL = usa el del modelo; usar_descuento 0 = precio normal
        //    (en QA la columna ya tiene DEFAULT 0; se pone explicito para no depender de eso).
        List<Object[]> filas = new ArrayList<>();
        Map<Integer, Double> precioPorProducto = new HashMap<>();
        for (ModeloDePrueba m : modelos) {
            Integer pid = productoId.get(m.codigoBarras());
            precioPorProducto.put(pid, m.precioVenta());
            for (ArticuloDePrueba a : m.articulos()) {
                filas.add(new Object[]{pid, a.talla(), a.color(), m.descripcion(), ModeloDePrueba.MARCA, a.stock(),
                        categorias.get(m.categoria())});
            }
        }
        jdbc.batchUpdate("""
                INSERT INTO variantes (producto_id, talla, color, descripcion, marca, stock, habilitado,
                                       palabra_clave_id, precio_venta, precio_rebaja, usar_descuento, fecha_creacion)
                VALUES (?, ?, ?, ?, ?, ?, '1', ?, NULL, NULL, 0, NOW())""", filas);

        List<ArticuloGuardado> guardados = new ArrayList<>();
        List<int[]> varianteProducto = new ArrayList<>();
        named.query("SELECT id, producto_id, stock FROM variantes WHERE producto_id IN (:ids) ORDER BY id",
                new MapSqlParameterSource("ids", productoId.values()),
                rs -> {
                    int pid = rs.getInt("producto_id");
                    guardados.add(new ArticuloGuardado(rs.getInt("id"), precioPorProducto.get(pid), rs.getInt("stock")));
                    varianteProducto.add(new int[]{rs.getInt("id"), pid});
                });

        // 4. Imagenes reusadas (R7): una por articulo, en orden; el modelo usa la de su primer articulo.
        if (!imagenes.isEmpty()) {
            List<Object[]> ligas = new ArrayList<>();
            Map<Integer, Long> primeraDelModelo = new HashMap<>();
            for (int[] vp : varianteProducto) {
                Long imagen = imagenes.get(Math.floorMod(vp[0], imagenes.size()));
                ligas.add(new Object[]{vp[0], imagen});
                primeraDelModelo.putIfAbsent(vp[1], imagen);
            }
            jdbc.batchUpdate("INSERT INTO variante_imagen (variante_id, imagen_id, principal) VALUES (?, ?, 1)", ligas);
            jdbc.batchUpdate("INSERT INTO producto_imagen_copy (producto_id, imagen_id, principal) VALUES (?, ?, 1)",
                    primeraDelModelo.entrySet().stream().map(e -> new Object[]{e.getKey(), e.getValue()}).toList());
        }
        return guardados;
    }

    private Map<String, Integer> porCodigo(String sql, List<ModeloDePrueba> modelos) {
        Map<String, Integer> mapa = new HashMap<>();
        named.query(sql, new MapSqlParameterSource("codigos", modelos.stream().map(ModeloDePrueba::codigoBarras).toList()),
                rs -> {
                    mapa.put(rs.getString("codigo_barras"), rs.getInt("id"));
                });
        return mapa;
    }

    private Map<String, Integer> categoriasPorNombre() {
        Map<String, Integer> mapa = new HashMap<>();
        named.query("SELECT MIN(id) AS id, nombre FROM palabra_clave WHERE nombre IN (:nombres) GROUP BY nombre",
                new MapSqlParameterSource("nombres", CatalogoAleatorio.categorias()),
                rs -> {
                    mapa.put(rs.getString("nombre"), rs.getInt("id"));
                });
        return mapa;
    }

    /** R14: baja logica de modelos y articulos de prueba; se quitan sus ligas de imagen, nunca la imagen. */
    @Override
    public int darDeBaja() {
        String modelosDePrueba = """
                SELECT p.id FROM producto p JOIN codigo_barras cb ON cb.id = p.codigo_barras_id
                WHERE cb.codigo_barras LIKE ? AND p.marca = ?""";
        Object[] args = {PATRON_CODIGO, ModeloDePrueba.MARCA};
        Integer bajas = transaccion.execute(estado -> {
            jdbc.update("DELETE FROM variante_imagen WHERE variante_id IN "
                    + "(SELECT v.id FROM variantes v WHERE v.producto_id IN (" + modelosDePrueba + "))", args);
            jdbc.update("DELETE FROM producto_imagen_copy WHERE producto_id IN (" + modelosDePrueba + ")", args);
            int articulos = jdbc.update("UPDATE variantes SET habilitado = '0', stock = 0 "
                    + "WHERE habilitado = '1' AND producto_id IN (" + modelosDePrueba + ")", args);
            // MySQL no deja leer en un subquery la misma tabla que se actualiza (error 1093): la
            // tabla derivada "x" se materializa antes del UPDATE y lo evita.
            jdbc.update("UPDATE producto SET habilitado = '0', stock = 0 WHERE habilitado = '1' AND id IN ("
                    + "SELECT x.id FROM (" + modelosDePrueba + ") x)", args);
            return articulos;
        });
        return bajas == null ? 0 : bajas;
    }
}
