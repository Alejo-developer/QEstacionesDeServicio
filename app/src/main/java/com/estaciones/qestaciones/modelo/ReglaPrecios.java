package com.estaciones.qestaciones.modelo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad que define una regla de precios ACPM.
 * HU-03: Configuración de precios diferenciados por vehículo y zona.
 * Cumplimiento del Decreto 1428 de 2025.
 */
@Entity(tableName = "reglas_precios")
public class ReglaPrecios {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String zona;
    private String tipoVehiculo; // Valor del enum TipoVehiculo
    private double precioBase;
    private double precioSubsidiado;
    private boolean aplicaSubsidio;
    private long fechaVigencia;
    private String decreto;
    private long fechaCreacion;

    // Constructor
    public ReglaPrecios() {
        this.fechaCreacion = System.currentTimeMillis();
        this.fechaVigencia = System.currentTimeMillis();
        this.aplicaSubsidio = true;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getTipoVehiculo() {
        return tipoVehiculo;
    }

    public void setTipoVehiculo(String tipoVehiculo) {
        this.tipoVehiculo = tipoVehiculo;
    }

    public double getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(double precioBase) {
        this.precioBase = precioBase;
    }

    public double getPrecioSubsidiado() {
        return precioSubsidiado;
    }

    public void setPrecioSubsidiado(double precioSubsidiado) {
        this.precioSubsidiado = precioSubsidiado;
    }

    public boolean isAplicaSubsidio() {
        return aplicaSubsidio;
    }

    public void setAplicaSubsidio(boolean aplicaSubsidio) {
        this.aplicaSubsidio = aplicaSubsidio;
    }

    public long getFechaVigencia() {
        return fechaVigencia;
    }

    public void setFechaVigencia(long fechaVigencia) {
        this.fechaVigencia = fechaVigencia;
    }

    public String getDecreto() {
        return decreto;
    }

    public void setDecreto(String decreto) {
        this.decreto = decreto;
    }

    public long getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(long fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
