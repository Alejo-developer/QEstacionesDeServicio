package com.estaciones.qestaciones.modelo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Orden de despacho generada por el DISTRIBUIDOR hacia una ESTACIÓN.
 * Al confirmar la entrega: baja el stock del distribuidor y
 * se crea una ENTRADA en el inventario de la estación destino.
 */
@Entity(tableName = "ordenes_despacho")
public class OrdenDespacho {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String numeroOrden;       // e.g. "OD-2024-001"
    private String distribuidorNombre;
    private String estacionDestino;
    private String tipoCombustible;
    private double volumenLitros;
    private String estado;            // PENDIENTE, ENTREGADA, CANCELADA
    private String observaciones;
    private long fechaCreacion;
    private long fechaEntrega;        // 0 si aún no entregada

    public OrdenDespacho() {
        this.fechaCreacion = System.currentTimeMillis();
        this.estado = "PENDIENTE";
        this.fechaEntrega = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumeroOrden() { return numeroOrden; }
    public void setNumeroOrden(String n) { this.numeroOrden = n; }

    public String getDistribuidorNombre() { return distribuidorNombre; }
    public void setDistribuidorNombre(String d) { this.distribuidorNombre = d; }

    public String getEstacionDestino() { return estacionDestino; }
    public void setEstacionDestino(String e) { this.estacionDestino = e; }

    public String getTipoCombustible() { return tipoCombustible; }
    public void setTipoCombustible(String t) { this.tipoCombustible = t; }

    public double getVolumenLitros() { return volumenLitros; }
    public void setVolumenLitros(double v) { this.volumenLitros = v; }

    public String getEstado() { return estado; }
    public void setEstado(String e) { this.estado = e; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String o) { this.observaciones = o; }

    public long getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(long f) { this.fechaCreacion = f; }

    public long getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(long f) { this.fechaEntrega = f; }
}
