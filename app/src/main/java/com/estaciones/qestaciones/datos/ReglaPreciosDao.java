package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.estaciones.qestaciones.modelo.ReglaPrecios;

import java.util.List;

/**
 * DAO para operaciones de la tabla reglas_precios.
 * HU-03: Configuración de precios ACPM por vehículo y zona.
 */
@Dao
public interface ReglaPreciosDao {

    @Insert
    void insertarRegla(ReglaPrecios regla);

    @Query("SELECT * FROM reglas_precios WHERE zona = :zona AND tipoVehiculo = :tipoVehiculo ORDER BY fechaCreacion DESC LIMIT 1")
    ReglaPrecios obtenerPorZonaYTipo(String zona, String tipoVehiculo);

    @Query("SELECT * FROM reglas_precios ORDER BY fechaCreacion DESC")
    List<ReglaPrecios> obtenerTodas();

    @Query("SELECT * FROM reglas_precios ORDER BY fechaCreacion DESC")
    List<ReglaPrecios> obtenerHistorial();

    // HU-15: Filtro avanzado por rango de fechas
    @Query("SELECT * FROM reglas_precios WHERE fechaCreacion BETWEEN :fechaInicio AND :fechaFin ORDER BY fechaCreacion DESC")
    List<ReglaPrecios> obtenerHistorialPorFechas(long fechaInicio, long fechaFin);

    // HU-15: Filtro por zona y rango de fechas
    @Query("SELECT * FROM reglas_precios WHERE zona LIKE '%' || :zona || '%' AND fechaCreacion BETWEEN :fechaInicio AND :fechaFin ORDER BY fechaCreacion DESC")
    List<ReglaPrecios> obtenerHistorialPorZonaYFechas(String zona, long fechaInicio, long fechaFin);

    @Query("SELECT * FROM reglas_precios WHERE tipoVehiculo != 'SERVICIO_PUBLICO' ORDER BY fechaCreacion DESC")
    List<ReglaPrecios> obtenerReglasConSubsidio();

    // HU-15: Contar total de registros (para verificación)
    @Query("SELECT COUNT(*) FROM reglas_precios")
    int contarReglas();

    @Delete
    void eliminarRegla(ReglaPrecios regla);
}
