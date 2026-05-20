package com.estaciones.qestaciones.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.estaciones.qestaciones.modelo.MovimientoSaldo;

import java.util.List;

@Dao
public interface MovimientoSaldoDao {

    @Insert
    void insertar(MovimientoSaldo m);

    @Query("SELECT * FROM movimientos_saldo WHERE usuarioNombre = :usuario ORDER BY fecha DESC")
    List<MovimientoSaldo> obtenerPorUsuario(String usuario);

    @Query("SELECT * FROM movimientos_saldo ORDER BY fecha DESC LIMIT 200")
    List<MovimientoSaldo> obtenerRecientes();
}
