package com.estaciones.qestaciones.modelo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad que registra las validaciones de subsidio realizadas.
 * HU-05: Registro de validaciones para trazabilidad.
 */
@Entity(tableName = "validaciones_subsidio")
public class ValidacionSubsidio {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String placa;
    private long fechaValidacion;
    private String resultado; // "APROBADO" o "RECHAZADO"
    private double precioAplicado;
    private String motivoRechazo;
    private String tipoVehiculo;

    // Constructor
    public ValidacionSubsidio() {
        this.fechaValidacion = System.currentTimeMillis();
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public long getFechaValidacion() {
        return fechaValidacion;
    }

    public void setFechaValidacion(long fechaValidacion) {
        this.fechaValidacion = fechaValidacion;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public double getPrecioAplicado() {
        return precioAplicado;
    }

    public void setPrecioAplicado(double precioAplicado) {
        this.precioAplicado = precioAplicado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public String getTipoVehiculo() {
        return tipoVehiculo;
    }

    public void setTipoVehiculo(String tipoVehiculo) {
        this.tipoVehiculo = tipoVehiculo;
    }
}
