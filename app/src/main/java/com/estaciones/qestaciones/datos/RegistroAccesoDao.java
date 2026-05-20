package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.estaciones.qestaciones.modelo.RegistroAcceso;

import java.util.List;

/**
 * DAO para operaciones de la tabla registros_acceso.
 * HU-01: Trazabilidad de accesos al sistema.
 */
@Dao
public interface RegistroAccesoDao {

    @Insert
    void insertar(RegistroAcceso registro);

    @Query("SELECT * FROM registros_acceso WHERE idUsuario = :idUsuario ORDER BY fechaHora DESC")
    List<RegistroAcceso> obtenerPorUsuario(int idUsuario);

    @Query("SELECT * FROM registros_acceso ORDER BY fechaHora DESC LIMIT 50")
    List<RegistroAcceso> obtenerRecientes();
}
