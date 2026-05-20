package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.estaciones.qestaciones.modelo.OrdenDespacho;

import java.util.List;

@Dao
public interface OrdenDespachoDao {

    @Insert
    void insertar(OrdenDespacho orden);

    @Update
    void actualizar(OrdenDespacho orden);

    @Query("SELECT * FROM ordenes_despacho WHERE distribuidorNombre = :distribuidor ORDER BY fechaCreacion DESC")
    List<OrdenDespacho> obtenerPorDistribuidor(String distribuidor);

    @Query("SELECT * FROM ordenes_despacho WHERE distribuidorNombre = :distribuidor AND estado = 'PENDIENTE' ORDER BY fechaCreacion DESC")
    List<OrdenDespacho> obtenerPendientesPorDistribuidor(String distribuidor);

    @Query("SELECT * FROM ordenes_despacho ORDER BY fechaCreacion DESC")
    List<OrdenDespacho> obtenerTodas();

    @Query("SELECT * FROM ordenes_despacho WHERE id = :id LIMIT 1")
    OrdenDespacho obtenerPorId(int id);

    @Query("UPDATE ordenes_despacho SET estado = :estado, fechaEntrega = :fecha WHERE id = :id")
    void actualizarEstado(int id, String estado, long fecha);

    @Query("SELECT COUNT(*) FROM ordenes_despacho WHERE distribuidorNombre = :distribuidor")
    int contarOrdenesPorDistribuidor(String distribuidor);
}
