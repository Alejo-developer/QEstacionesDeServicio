package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.estaciones.qestaciones.modelo.RegistroEntrega;

import java.util.List;

/**
 * DAO para gestión de registros de entrega.
 * HU-09: CRUD de entregas con trazabilidad.
 */
@Dao
public interface RegistroEntregaDao {

    @Insert
    void insertar(RegistroEntrega entrega);

    @Query("SELECT * FROM registros_entrega ORDER BY fechaRegistro DESC")
    List<RegistroEntrega> obtenerTodas();

    @Query("SELECT * FROM registros_entrega WHERE estacionDestino = :estacion ORDER BY fechaRegistro DESC")
    List<RegistroEntrega> obtenerPorEstacion(String estacion);

    @Query("SELECT * FROM registros_entrega WHERE fechaEntrega BETWEEN :inicio AND :fin ORDER BY fechaEntrega DESC")
    List<RegistroEntrega> obtenerPorFecha(long inicio, long fin);

    @Query("SELECT * FROM registros_entrega WHERE placaVehiculo = :placa ORDER BY fechaRegistro DESC")
    List<RegistroEntrega> obtenerPorPlaca(String placa);

    @Query("SELECT SUM(volumenLitros) FROM registros_entrega WHERE estacionDestino = :estacion")
    double obtenerVolumenTotalPorEstacion(String estacion);

    // HU-14: Filtro combinado por estacion, combustible y rango de fechas
    @Query("SELECT * FROM registros_entrega " +
           "WHERE (:estacion = '' OR estacionDestino LIKE '%' || :estacion || '%') " +
           "AND (:combustible = '' OR tipoCombustible = :combustible) " +
           "AND fechaEntrega BETWEEN :inicio AND :fin " +
           "ORDER BY fechaEntrega DESC")
    List<RegistroEntrega> obtenerConFiltros(String estacion, String combustible,
                                            long inicio, long fin);
}
