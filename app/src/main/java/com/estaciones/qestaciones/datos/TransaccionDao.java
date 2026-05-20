package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.estaciones.qestaciones.modelo.Transaccion;

import java.util.List;

@Dao
public interface TransaccionDao {

    @Insert
    void insertar(Transaccion transaccion);

    @Query("SELECT * FROM transacciones ORDER BY fechaVenta DESC")
    List<Transaccion> obtenerTodas();

    @Query("SELECT * FROM transacciones WHERE nombreEstacion = :estacion ORDER BY fechaVenta DESC")
    List<Transaccion> obtenerPorEstacion(String estacion);

    @Query("SELECT * FROM transacciones WHERE compradorUsuario = :comprador ORDER BY fechaVenta DESC")
    List<Transaccion> obtenerPorComprador(String comprador);

    @Query("SELECT * FROM transacciones WHERE empresaVendedoraUsuario = :empresa ORDER BY fechaVenta DESC")
    List<Transaccion> obtenerPorEmpresaVendedora(String empresa);

    @Query("SELECT * FROM transacciones WHERE fechaVenta BETWEEN :inicio AND :fin ORDER BY fechaVenta DESC")
    List<Transaccion> obtenerPorRangoFechas(long inicio, long fin);

    @Query("SELECT SUM(totalCop) FROM transacciones WHERE nombreEstacion = :estacion")
    double obtenerTotalVentasPorEstacion(String estacion);

    @Query("SELECT SUM(volumenLitros) FROM transacciones WHERE tipoCombustible = :tipo")
    double obtenerVolumenTotalPorTipo(String tipo);

    @Query("SELECT COUNT(*) FROM transacciones WHERE subsidioAplicado = 1")
    int contarTransaccionesConSubsidio();
}
