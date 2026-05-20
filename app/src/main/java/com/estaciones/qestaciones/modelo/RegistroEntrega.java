package com.estaciones.qestaciones.modelo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad que representa un registro de entrega de combustible.
 * HU-09: Registro de entregas para trazabilidad.
 */
@Entity(tableName = "registros_entrega")
public class RegistroEntrega {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String estacionDestino;
    private String vehiculoTransporte;
    private String placaVehiculo;
    private double volumenLitros;
    private String tipoCombustible;
    private long fechaEntrega;
    private String observaciones;
    private String registradoPor;
    private long fechaRegistro;

    // Constructor
    public RegistroEntrega() {
        this.fechaEntrega = System.currentTimeMillis();
        this.fechaRegistro = System.currentTimeMillis();
        this.tipoCombustible = "ACPM";
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEstacionDestino() { return estacionDestino; }
    public void setEstacionDestino(String estacionDestino) { this.estacionDestino = estacionDestino; }

    public String getVehiculoTransporte() { return vehiculoTransporte; }
    public void setVehiculoTransporte(String vehiculoTransporte) { this.vehiculoTransporte = vehiculoTransporte; }

    public String getPlacaVehiculo() { return placaVehiculo; }
    public void setPlacaVehiculo(String placaVehiculo) { this.placaVehiculo = placaVehiculo; }

    public double getVolumenLitros() { return volumenLitros; }
    public void setVolumenLitros(double volumenLitros) { this.volumenLitros = volumenLitros; }

    public String getTipoCombustible() { return tipoCombustible; }
    public void setTipoCombustible(String tipoCombustible) { this.tipoCombustible = tipoCombustible; }

    public long getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(long fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }

    public long getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(long fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
