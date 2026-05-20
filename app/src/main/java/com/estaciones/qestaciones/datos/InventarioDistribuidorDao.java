package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.estaciones.qestaciones.modelo.InventarioDistribuidor;

import java.util.List;

@Dao
public interface InventarioDistribuidorDao {

    @Insert
    void insertar(InventarioDistribuidor inventario);

    @Update
    void actualizar(InventarioDistribuidor inventario);

    @Query("SELECT * FROM inventario_distribuidor WHERE nombreDistribuidor = :distribuidor AND tipoCombustible = :tipo LIMIT 1")
    InventarioDistribuidor obtenerPorDistribuidorYTipo(String distribuidor, String tipo);

    @Query("SELECT * FROM inventario_distribuidor WHERE nombreDistribuidor = :distribuidor")
    List<InventarioDistribuidor> obtenerPorDistribuidor(String distribuidor);

    @Query("UPDATE inventario_distribuidor SET stockLitros = stockLitros + :litros, ultimaActualizacion = :ts WHERE nombreDistribuidor = :distribuidor AND tipoCombustible = :tipo")
    void agregarStock(String distribuidor, String tipo, double litros, long ts);

    @Query("UPDATE inventario_distribuidor SET stockLitros = stockLitros - :litros, ultimaActualizacion = :ts WHERE nombreDistribuidor = :distribuidor AND tipoCombustible = :tipo")
    void reducirStock(String distribuidor, String tipo, double litros, long ts);

    @Query("SELECT stockLitros FROM inventario_distribuidor WHERE nombreDistribuidor = :distribuidor AND tipoCombustible = :tipo")
    double obtenerStock(String distribuidor, String tipo);
}
