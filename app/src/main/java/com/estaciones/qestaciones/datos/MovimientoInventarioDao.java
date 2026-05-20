package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.estaciones.qestaciones.modelo.MovimientoInventario;

import java.util.List;

/**
 * DAO para gestión de movimientos de inventario.
 * HU-13: Historial de movimientos con datos auditables.
 */
@Dao
public interface MovimientoInventarioDao {

    @Insert
    void insertar(MovimientoInventario movimiento);

    @Query("SELECT * FROM movimientos_inventario ORDER BY fechaMovimiento DESC")
    List<MovimientoInventario> obtenerTodos();

    @Query("SELECT * FROM movimientos_inventario WHERE tipoMovimiento = :tipo ORDER BY fechaMovimiento DESC")
    List<MovimientoInventario> obtenerPorTipo(String tipo);

    @Query("SELECT * FROM movimientos_inventario WHERE fechaMovimiento BETWEEN :inicio AND :fin ORDER BY fechaMovimiento DESC")
    List<MovimientoInventario> obtenerPorFechas(long inicio, long fin);

    @Query("SELECT * FROM movimientos_inventario WHERE esAuditable = 1 ORDER BY fechaMovimiento DESC")
    List<MovimientoInventario> obtenerAuditables();

    @Query("SELECT COALESCE(SUM(CASE WHEN tipoMovimiento = 'ENTRADA' THEN volumenLitros WHEN tipoMovimiento = 'SALIDA' THEN -volumenLitros ELSE 0 END), 0) FROM movimientos_inventario WHERE tipoCombustible = :combustible")
    double obtenerSaldoActual(String combustible);

    /**
     * Saldo aislado por empresa (scope por nombreUsuario de la estación).
     *
     * Incluye también movimientos con empresaUsuario IS NULL para no perder el inventario
     * creado antes de la migración v9 (cuando el saldo era global). En cuanto el usuario
     * empiece a registrar movimientos nuevos, todos llevarán su empresaUsuario y el scope
     * estricto se mantiene entre empresas distintas.
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN tipoMovimiento = 'ENTRADA' THEN volumenLitros WHEN tipoMovimiento = 'SALIDA' THEN -volumenLitros ELSE 0 END), 0) FROM movimientos_inventario WHERE tipoCombustible = :combustible AND (empresaUsuario = :empresa OR empresaUsuario IS NULL)")
    double obtenerSaldoPorEmpresa(String combustible, String empresa);

    @Query("SELECT * FROM movimientos_inventario WHERE empresaUsuario = :empresa OR empresaUsuario IS NULL ORDER BY fechaMovimiento DESC")
    List<MovimientoInventario> obtenerPorEmpresa(String empresa);

    @Query("SELECT COUNT(*) FROM movimientos_inventario")
    int contarMovimientos();
}
