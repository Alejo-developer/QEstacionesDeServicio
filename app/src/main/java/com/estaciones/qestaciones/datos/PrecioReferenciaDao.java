package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.estaciones.qestaciones.modelo.PrecioReferencia;

import java.util.List;

@Dao
public interface PrecioReferenciaDao {

    @Insert
    void insertar(PrecioReferencia precio);

    /** Último precio de referencia vigente para un tipo de combustible */
    @Query("SELECT * FROM precios_referencia WHERE tipoCombustible = :tipo ORDER BY fechaCreacion DESC LIMIT 1")
    PrecioReferencia obtenerUltimoPorTipo(String tipo);

    /** Historial completo (más recientes primero) */
    @Query("SELECT * FROM precios_referencia ORDER BY fechaCreacion DESC")
    List<PrecioReferencia> obtenerHistorial();

    /** Los precios actuales de todos los tipos (uno por tipo, el más reciente) */
    @Query("SELECT * FROM precios_referencia WHERE id IN " +
           "(SELECT MAX(id) FROM precios_referencia GROUP BY tipoCombustible) " +
           "ORDER BY tipoCombustible")
    List<PrecioReferencia> obtenerPreciosActuales();

    @Delete
    void eliminarPrecio(PrecioReferencia precio);
}
