package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.estaciones.qestaciones.modelo.ZonaRegulada;

import java.util.List;

/**
 * DAO para gestión de zonas reguladas.
 * HU-06: CRUD de zonas con versionamiento.
 */
@Dao
public interface ZonaReguladaDao {

    @Insert
    void insertar(ZonaRegulada zona);

    @Update
    void actualizar(ZonaRegulada zona);

    @Query("SELECT * FROM zonas_reguladas ORDER BY fechaModificacion DESC")
    List<ZonaRegulada> obtenerTodas();

    @Query("SELECT * FROM zonas_reguladas WHERE activa = 1 ORDER BY municipio ASC")
    List<ZonaRegulada> obtenerActivas();

    @Query("SELECT * FROM zonas_reguladas WHERE municipio = :municipio ORDER BY version DESC")
    List<ZonaRegulada> obtenerPorMunicipio(String municipio);

    @Query("SELECT * FROM zonas_reguladas WHERE municipio = :municipio ORDER BY version DESC LIMIT 1")
    ZonaRegulada obtenerVersionActual(String municipio);

    @Query("SELECT COUNT(*) FROM zonas_reguladas WHERE codigoZona = :codigo AND activa = 1")
    int contarPorCodigo(String codigo);

    @Query("SELECT MAX(version) FROM zonas_reguladas WHERE municipio = :municipio")
    int obtenerUltimaVersion(String municipio);
}
