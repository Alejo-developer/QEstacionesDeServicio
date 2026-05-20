package com.estaciones.qestaciones.modelo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Precio de referencia nacional fijado por el REGULADOR.
 * Las estaciones lo usan como base para calcular su precio de venta.
 */
@Entity(tableName = "precios_referencia")
public class PrecioReferencia {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String tipoCombustible;  // ACPM, Gasolina Corriente, Gasolina Extra
    private double precioPorLitro;   // COP por litro
    private String decreto;
    private long fechaVigencia;
    private String fijadoPor;        // nombreUsuario del regulador
    private long fechaCreacion;

    public PrecioReferencia() {
        this.fechaCreacion = System.currentTimeMillis();
        this.fechaVigencia = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTipoCombustible() { return tipoCombustible; }
    public void setTipoCombustible(String t) { this.tipoCombustible = t; }

    public double getPrecioPorLitro() { return precioPorLitro; }
    public void setPrecioPorLitro(double p) { this.precioPorLitro = p; }

    public String getDecreto() { return decreto; }
    public void setDecreto(String d) { this.decreto = d; }

    public long getFechaVigencia() { return fechaVigencia; }
    public void setFechaVigencia(long f) { this.fechaVigencia = f; }

    public String getFijadoPor() { return fijadoPor; }
    public void setFijadoPor(String f) { this.fijadoPor = f; }

    public long getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(long f) { this.fechaCreacion = f; }
}
