package com.estaciones.qestaciones.modelo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Stock actual de combustible del DISTRIBUIDOR.
 * Un registro por tipo de combustible por distribuidor.
 */
@Entity(tableName = "inventario_distribuidor")
public class InventarioDistribuidor {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String nombreDistribuidor;
    private String tipoCombustible;
    private double stockLitros;     // saldo actual
    private long ultimaActualizacion;

    public InventarioDistribuidor() {
        this.ultimaActualizacion = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombreDistribuidor() { return nombreDistribuidor; }
    public void setNombreDistribuidor(String n) { this.nombreDistribuidor = n; }

    public String getTipoCombustible() { return tipoCombustible; }
    public void setTipoCombustible(String t) { this.tipoCombustible = t; }

    public double getStockLitros() { return stockLitros; }
    public void setStockLitros(double s) { this.stockLitros = s; }

    public long getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(long u) { this.ultimaActualizacion = u; }
}
