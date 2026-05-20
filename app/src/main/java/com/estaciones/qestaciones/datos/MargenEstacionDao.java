package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.estaciones.qestaciones.modelo.MargenEstacion;

import java.util.List;

@Dao
public interface MargenEstacionDao {

    @Insert
    void insertar(MargenEstacion margen);

    /** Margen actual de una estación para un tipo de combustible */
    @Query("SELECT * FROM margenes_estacion WHERE nombreEstacion = :estacion AND tipoCombustible = :tipo ORDER BY fechaActualizacion DESC LIMIT 1")
    MargenEstacion obtenerMargen(String estacion, String tipo);

    /** Todos los márgenes actuales de una estación (uno por tipo) */
    @Query("SELECT * FROM margenes_estacion WHERE nombreEstacion = :estacion AND id IN " +
           "(SELECT MAX(id) FROM margenes_estacion WHERE nombreEstacion = :estacion GROUP BY tipoCombustible) " +
           "ORDER BY tipoCombustible")
    List<MargenEstacion> obtenerMargenesPorEstacion(String estacion);

    /** Precio de venta actual para una estación y combustible */
    @Query("SELECT precioVenta FROM margenes_estacion WHERE nombreEstacion = :estacion AND tipoCombustible = :tipo ORDER BY fechaActualizacion DESC LIMIT 1")
    double obtenerPrecioVenta(String estacion, String tipo);
}
