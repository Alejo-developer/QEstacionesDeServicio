package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.estaciones.qestaciones.modelo.ValidacionSubsidio;

import java.util.List;

/**
 * DAO para operaciones de la tabla validaciones_subsidio.
 * HU-05: Registro de validaciones de subsidios.
 */
@Dao
public interface ValidacionSubsidioDao {

    @Insert
    void insertar(ValidacionSubsidio validacion);

    @Query("SELECT * FROM validaciones_subsidio WHERE placa = :placa ORDER BY fechaValidacion DESC")
    List<ValidacionSubsidio> obtenerPorPlaca(String placa);

    /**
     * Bug 5 / HU-ESTACION: La estación solo puede aplicar subsidio si un
     * OPERADOR, REGULADOR o ADMINISTRADOR validó previamente la placa vía API de tránsito.
     * Devuelve el registro aprobado más reciente, o null si nunca se aprobó.
     */
    @Query("SELECT * FROM validaciones_subsidio WHERE placa = :placa AND resultado = 'APROBADO' ORDER BY fechaValidacion DESC LIMIT 1")
    ValidacionSubsidio obtenerUltimaAprobadaPorPlaca(String placa);

    @Query("SELECT * FROM validaciones_subsidio ORDER BY fechaValidacion DESC LIMIT 50")
    List<ValidacionSubsidio> obtenerTodas();

    @Delete
    void eliminarValidacion(ValidacionSubsidio validacion);
}
