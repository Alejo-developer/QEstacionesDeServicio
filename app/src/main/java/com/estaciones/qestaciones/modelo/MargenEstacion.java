package com.estaciones.qestaciones.modelo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Margen de precio que cada ESTACIÓN agrega sobre el precio de referencia.
 * precioVenta = precioReferencia + margenPesos
 * margenPesos puede ser negativo (para vender más barato y ganar en volumen).
 */
@Entity(tableName = "margenes_estacion")
public class MargenEstacion {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String nombreEstacion;
    private String tipoCombustible;
    private double margenPesos;       // COP por litro, puede ser negativo
    private double precioReferencia;  // snapshot del precio de referencia al momento
    private double precioVenta;       // precioReferencia + margenPesos
    private long fechaActualizacion;
    private String actualizadoPor;

    public MargenEstacion() {
        this.fechaActualizacion = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombreEstacion() { return nombreEstacion; }
    public void setNombreEstacion(String n) { this.nombreEstacion = n; }

    public String getTipoCombustible() { return tipoCombustible; }
    public void setTipoCombustible(String t) { this.tipoCombustible = t; }

    public double getMargenPesos() { return margenPesos; }
    public void setMargenPesos(double m) { this.margenPesos = m; }

    public double getPrecioReferencia() { return precioReferencia; }
    public void setPrecioReferencia(double p) { this.precioReferencia = p; }

    public double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(double p) { this.precioVenta = p; }

    public long getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(long f) { this.fechaActualizacion = f; }

    public String getActualizadoPor() { return actualizadoPor; }
    public void setActualizadoPor(String a) { this.actualizadoPor = a; }
}
