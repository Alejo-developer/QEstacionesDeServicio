package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.estaciones.qestaciones.modelo.Auditoria;

import java.util.List;

@Dao
public interface AuditoriaDao {
    @Insert
    void insertar(Auditoria auditoria);

    @Query("SELECT * FROM auditoria ORDER BY fecha DESC")
    List<Auditoria> obtenerTodas();

    @Query("SELECT * FROM auditoria WHERE usuario = :usuario ORDER BY fecha DESC")
    List<Auditoria> obtenerPorUsuario(String usuario);

    @Query("SELECT * FROM auditoria WHERE " +
           "(:usuarioFiltro = '' OR usuario LIKE '%' || :usuarioFiltro || '%') AND " +
           "(:moduloFiltro = '' OR modulo LIKE '%' || :moduloFiltro || '%') AND " +
           "fecha BETWEEN :inicio AND :fin " +
           "ORDER BY fecha DESC")
    List<Auditoria> obtenerFiltrado(String usuarioFiltro, String moduloFiltro, long inicio, long fin);

    @Query("SELECT * FROM auditoria WHERE fecha BETWEEN :inicio AND :fin ORDER BY fecha DESC")
    List<Auditoria> obtenerPorRangoFechas(long inicio, long fin);
}
