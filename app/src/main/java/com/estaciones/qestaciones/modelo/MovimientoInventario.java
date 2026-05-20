package com.estaciones.qestaciones.modelo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad que representa un movimiento de inventario de combustible.
 * HU-13: Control detallado de inventarios para auditoría.
 */
@Entity(tableName = "movimientos_inventario")
public class MovimientoInventario {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String tipoMovimiento;   // ENTRADA, SALIDA, AJUSTE
    private String tipoCombustible;
    private double volumenLitros;
    private double saldoAnterior;
    private double saldoNuevo;
    private String motivo;
    private String responsable;
    private String distribuidorNombre; // quién hizo la entrega (solo en ENTRADA)
    private long fechaMovimiento;
    private boolean esAuditable;

    // Migración v8 → v9: scope de empresa para aislar inventario por estación.
    // nullable para preservar movimientos antiguos (saldo global).
    private String empresaUsuario;

    // Constructor
    public MovimientoInventario() {
        this.fechaMovimiento = System.currentTimeMillis();
        this.esAuditable = true;
        this.tipoCombustible = "ACPM";
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }

    public String getTipoCombustible() { return tipoCombustible; }
    public void setTipoCombustible(String tipoCombustible) { this.tipoCombustible = tipoCombustible; }

    public double getVolumenLitros() { return volumenLitros; }
    public void setVolumenLitros(double volumenLitros) { this.volumenLitros = volumenLitros; }

    public double getSaldoAnterior() { return saldoAnterior; }
    public void setSaldoAnterior(double saldoAnterior) { this.saldoAnterior = saldoAnterior; }

    public double getSaldoNuevo() { return saldoNuevo; }
    public void setSaldoNuevo(double saldoNuevo) { this.saldoNuevo = saldoNuevo; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }

    public String getDistribuidorNombre() { return distribuidorNombre; }
    public void setDistribuidorNombre(String distribuidorNombre) { this.distribuidorNombre = distribuidorNombre; }

    public long getFechaMovimiento() { return fechaMovimiento; }
    public void setFechaMovimiento(long fechaMovimiento) { this.fechaMovimiento = fechaMovimiento; }

    public boolean isEsAuditable() { return esAuditable; }
    public void setEsAuditable(boolean esAuditable) { this.esAuditable = esAuditable; }

    public String getEmpresaUsuario() { return empresaUsuario; }
    public void setEmpresaUsuario(String empresaUsuario) { this.empresaUsuario = empresaUsuario; }
}
